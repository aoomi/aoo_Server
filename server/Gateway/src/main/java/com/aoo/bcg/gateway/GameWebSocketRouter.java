package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.IdempotencyStore;
import com.aoo.bcg.common.idempotency.IdempotencyKey;
import com.aoo.bcg.common.observability.OperationLogContext;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RuleChainExecutor;
import com.aoo.bcg.gamespi.api.DeprecatedEntryPointBlocklist;

import java.util.Objects;
import java.time.Duration;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import com.aoo.bcg.gamespi.time.OperationDeadline;

/** Generic Gateway-to-game adapter; contains no concrete game dependency. */
public final class GameWebSocketRouter {
    private final GameRegistry games;
    private final GameRoomResolver rooms;
    private final WebSocketRequestGuard guard;
    private final IdempotencyStore<GameCommandResult> idempotency;
    private final GameCommandCommitter runtimeCommitter;
    private final Duration retention;
    private final AuthoritativeTimeSource time;
    private final RiskAdmissionAuthority risk;
    private final GameOperationTimeline timeline = new GameOperationTimeline(256);

    public GameWebSocketRouter(GameRegistry games, GameRoomResolver rooms, WebSocketRequestGuard guard,
                               IdempotencyStore<GameCommandResult> idempotency, Duration retention) {
        this(games, rooms, guard, idempotency, retention, GameCommandCommitter.noOp(), AuthoritativeTimeSource.systemUtc(),null);
    }

    public GameWebSocketRouter(GameRegistry games, GameRoomResolver rooms, WebSocketRequestGuard guard,
                               IdempotencyStore<GameCommandResult> idempotency, Duration retention,
                               GameCommandCommitter runtimeCommitter) {
        this(games, rooms, guard, idempotency, retention, runtimeCommitter, AuthoritativeTimeSource.systemUtc(),null);
    }

    GameWebSocketRouter(GameRegistry games, GameRoomResolver rooms, WebSocketRequestGuard guard,
                        IdempotencyStore<GameCommandResult> idempotency, Duration retention,
                        GameCommandCommitter runtimeCommitter, AuthoritativeTimeSource time) { this(games,rooms,guard,idempotency,retention,runtimeCommitter,time,null); }
    GameWebSocketRouter(GameRegistry games, GameRoomResolver rooms, WebSocketRequestGuard guard,
                        IdempotencyStore<GameCommandResult> idempotency, Duration retention,
                        GameCommandCommitter runtimeCommitter, AuthoritativeTimeSource time,RiskAdmissionAuthority risk) {
        this.games = Objects.requireNonNull(games);
        this.rooms = Objects.requireNonNull(rooms);
        this.guard = Objects.requireNonNull(guard);
        this.idempotency = Objects.requireNonNull(idempotency);
        this.runtimeCommitter = Objects.requireNonNull(runtimeCommitter);
        this.time = Objects.requireNonNull(time);
        this.risk = risk;
        if (retention == null || retention.isNegative() || retention.isZero())
            throw new IllegalArgumentException("idempotency retention must be positive");
        this.retention = retention;
    }

    public RoutedResult route(ConnectionSession session, WebSocketFrame frame) {
        DeprecatedEntryPointBlocklist.requireMessageAllowed(frame.msgId());
        long scopedRoomId;
        try{scopedRoomId=Long.parseLong(frame.roomId());}catch(NumberFormatException error){throw new IllegalArgumentException("numeric roomId required",error);}
        IdempotencyKey idempotencyKey=new IdempotencyKey(session.userId(),frame.msgId(),scopedRoomId,frame.roundNo(),frame.requestId());
        GameCommandResult previous = idempotency.find(idempotencyKey).orElse(null);
        if (previous != null) return new RoutedResult(session, previous, true);
        if (!idempotency.acquire(idempotencyKey, retention)) {
            previous = idempotency.find(idempotencyKey).orElse(null);
            if (previous != null) return new RoutedResult(session, previous, true);
            GameRoomHandle pendingRoom=rooms.require(scopedRoomId);
            GameCommandRequest pending=new GameCommandRequest(frame.msgId(),frame.requestId(),frame.seq(),scopedRoomId,
                    frame.roundNo(),frame.playVersion(),session.userId(),session.seatId(),frame.body());
            previous=runtimeCommitter.findCommitted(pendingRoom,pending).orElse(null);
            if(previous!=null){idempotency.save(idempotencyKey,previous,retention);return new RoutedResult(session,previous,true);}
            throw new RequestOutcomeUnknownException(frame.requestId());
        }
        boolean committed = false;
        boolean mutationStarted = false;
        try (OperationLogContext ignored = OperationLogContext.open(frame.traceId(), frame.requestId(),
                scopedRoomId, session.userId(), session.seatId(), session.connectionId())) {
        timeline.begin(scopedRoomId, frame.seq(), frame.requestId(), frame.msgId());
        ConnectionSession accepted = guard.validate(session, frame);
        long roomId=scopedRoomId;
        RiskAdmissionAuthority.Action admissionAction="common.room.join_req".equals(frame.msgId())?RiskAdmissionAuthority.Action.JOIN_ROOM:"room.create".equals(frame.msgId())?RiskAdmissionAuthority.Action.CREATE_ROOM:null;
        if(admissionAction!=null){if(risk==null)throw new IllegalStateException("risk admission authority unavailable");long playerId;try{playerId=Long.parseLong(session.userId());}catch(NumberFormatException e){throw new RiskAdmissionException();}if(!risk.decide(admissionAction,playerId,null,roomId,frame.requestId()).allowed())throw new RiskAdmissionException();}
        GameRoomHandle room = rooms.require(roomId);
        if (!room.playVersion().equals(frame.playVersion())) throw new SecurityException("room play version mismatch");
        room.authoritativeSession().ifPresent(sessionAuthority ->
                ServerAuthorityInputGuard.validate(frame.msgId(), frame.body(), sessionAuthority.stateVersion()));
        GameProvider provider = games.require(room.gameId(),room.playVersion());
        GameCommandHandler handler = provider.commandHandler()
                .orElseThrow(() -> new IllegalStateException("game command handler is unavailable"));
        GameCommandRequest command = new GameCommandRequest(frame.msgId(), frame.requestId(), frame.seq(),
                roomId, frame.roundNo(), frame.playVersion(), session.userId(), session.seatId(), frame.body());
        var ruleResult = new RuleChainExecutor<>(provider.ruleComponents()).execute(command);
        if (!ruleResult.accepted()) throw new IllegalArgumentException(ruleResult.code() + ": " + ruleResult.message());
        mutationStarted = true;
        GameCommandResult handled = handler.handle(room, command);
        OperationDeadline operationDeadline = room.authoritativeSession()
                .map(com.aoo.bcg.gamespi.AuthoritativeGameSession::operationDeadline)
                .orElse(OperationDeadline.none());
        GameCommandResult result = handled.withTiming(time.epochMillis(), operationDeadline);
        if (room.authoritativeSession().isPresent()) result = result.withAuthorityMetadata(
                room.requireAuthoritativeSession().stateVersion(), frame.seq());
        provider.commandCommitter().commit(room, command, result);
        runtimeCommitter.commit(room, command, result);
        committed = true;
        idempotency.save(idempotencyKey, result, retention);
        timeline.complete(scopedRoomId, frame.seq(), frame.requestId(), frame.msgId(), operationDeadline);
        return new RoutedResult(accepted, result, false);
        } catch (RuntimeException | Error failure) {
            // A failed validation/handler/commit may retry. Once durable commit returned,
            // retain PROCESSING if completion persistence fails to prevent double mutation.
            if (!committed) { if(mutationStarted)idempotency.markUnknown(idempotencyKey); else idempotency.release(idempotencyKey); }
            timeline.fail(scopedRoomId, frame.seq(), frame.requestId(), frame.msgId(), failure);
            throw failure;
        }
    }

    public java.util.List<GameOperationTimeline.Entry> operationTimeline(long roomId) {
        return timeline.snapshot(roomId);
    }

    public RequestOutcome queryOutcome(ConnectionSession session, WebSocketFrame frame) {
        long roomId;
        try{roomId=Long.parseLong(frame.roomId());}catch(NumberFormatException error){return new RequestOutcome("INVALID_REQUEST",null,"numeric roomId required");}
        IdempotencyKey key=new IdempotencyKey(session.userId(),frame.msgId(),roomId,frame.roundNo(),frame.requestId());
        GameCommandResult result=idempotency.find(key).orElse(null);
        if(result!=null)return new RequestOutcome("COMPLETED",result,null);
        try {
            GameRoomHandle room=rooms.require(roomId);
            var request=new GameCommandRequest(frame.msgId(),frame.requestId(),frame.seq(),roomId,frame.roundNo(),frame.playVersion(),session.userId(),session.seatId(),frame.body());
            result=runtimeCommitter.findCommitted(room,request).orElse(null);
            if(result!=null){idempotency.save(key,result,retention);return new RequestOutcome("COMPLETED",result,null);}
            return new RequestOutcome("UNKNOWN",null,"REQUEST_OUTCOME_UNKNOWN");
        } catch(IllegalArgumentException missing) { return new RequestOutcome("NOT_FOUND",null,"REQUEST_NOT_FOUND"); }
    }

    public record RoutedResult(ConnectionSession session, GameCommandResult result, boolean replayed) { }
    public record RequestOutcome(String status,GameCommandResult result,String errorCode) { }
    public static final class RequestOutcomeUnknownException extends IllegalStateException {
        public RequestOutcomeUnknownException(String requestId){super("REQUEST_OUTCOME_UNKNOWN:"+requestId);}
    }
}
