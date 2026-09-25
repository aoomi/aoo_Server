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
import java.util.Map;
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
        return RoomOrderedExecutor.global().execute(scopedRoomId,()->routeOrdered(session,frame,scopedRoomId));
    }

    private RoutedResult routeOrdered(ConnectionSession session,WebSocketFrame frame,long scopedRoomId) {
        if(isReadOnly(frame))return routeReadOnly(session,frame,scopedRoomId);
        IdempotencyKey idempotencyKey=new IdempotencyKey(session.userId(),frame.msgId(),scopedRoomId,frame.roundNo(),frame.requestId());
        GameCommandResult previous = idempotency.find(idempotencyKey).orElse(null);
        if (previous != null) return replay(session,frame,scopedRoomId,previous);
        if (!idempotency.acquire(idempotencyKey, retention)) {
            previous = idempotency.find(idempotencyKey).orElse(null);
            if (previous != null) return replay(session,frame,scopedRoomId,previous);
            GameRoomHandle pendingRoom=rooms.require(scopedRoomId);
            GameCommandRequest pending=new GameCommandRequest(frame.msgId(),frame.requestId(),frame.seq(),scopedRoomId,
                    frame.roundNo(),frame.playVersion(),session.userId(),session.seatId(),frame.body());
            previous=runtimeCommitter.findCommitted(pendingRoom,pending).orElse(null);
            if(previous!=null){idempotency.save(idempotencyKey,previous,retention);return replay(session,frame,scopedRoomId,previous);}
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
        java.util.Map<String,Object> commandBody=frame.body();
        if("common.room.dispatch".equals(frame.msgId())&&"voice".equals(String.valueOf(frame.body().get("action"))))
            commandBody=GatewayVoiceAssetAuthority.authorize(frame.body(),Long.parseLong(session.userId()),
                    room.requireAuthoritativeSession().authoritativeState());
        GameCommandRequest command = new GameCommandRequest(frame.msgId(), frame.requestId(), frame.seq(),
                roomId, frame.roundNo(), frame.playVersion(), session.userId(), session.seatId(), commandBody);
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
        result = runtimeCommitter.commitResult(room, command, result);
        committed = true;
        // Production persistence completes the response in the same transaction as the room
        // snapshot. In-memory/test committers still use the router-owned idempotency store.
        if (!runtimeCommitter.persistsCommandResult()) idempotency.save(idempotencyKey, result, retention);
        timeline.complete(scopedRoomId, frame.seq(), frame.requestId(), frame.msgId(), operationDeadline);
        return new RoutedResult(rebindAfterSit(accepted,frame,result), result, false, true);
        } catch (RuntimeException | Error failure) {
            // A failed validation/handler/commit may retry. Once durable commit returned,
            // retain PROCESSING if completion persistence fails to prevent double mutation.
            if (!committed) { if(mutationStarted)idempotency.markUnknown(idempotencyKey); else idempotency.release(idempotencyKey); }
            timeline.fail(scopedRoomId, frame.seq(), frame.requestId(), frame.msgId(), failure);
            throw failure;
        }
    }

    /**
     * State and hint queries observe the room mailbox but never create durable command work.
     * They cannot change authority state, so snapshot, replay, idempotency and room broadcast
     * writes would only amplify a successful push into another room-wide synchronization cycle.
     */
    private RoutedResult routeReadOnly(ConnectionSession session,WebSocketFrame frame,long roomId) {
        try(OperationLogContext ignored=OperationLogContext.open(frame.traceId(),frame.requestId(),roomId,
                session.userId(),session.seatId(),session.connectionId())){
            timeline.begin(roomId,frame.seq(),frame.requestId(),frame.msgId());
            ConnectionSession accepted=guard.validate(session,frame);
            GameRoomHandle room=rooms.require(roomId);
            if(!room.playVersion().equals(frame.playVersion()))throw new SecurityException("room play version mismatch");
            room.authoritativeSession().ifPresent(authority->
                    ServerAuthorityInputGuard.validate(frame.msgId(),frame.body(),authority.stateVersion()));
            GameProvider provider=games.require(room.gameId(),room.playVersion());
            GameCommandRequest command=new GameCommandRequest(frame.msgId(),frame.requestId(),frame.seq(),roomId,
                    frame.roundNo(),frame.playVersion(),session.userId(),session.seatId(),frame.body());
            var ruleResult=new RuleChainExecutor<>(provider.ruleComponents()).execute(command);
            if(!ruleResult.accepted())throw new IllegalArgumentException(ruleResult.code()+": "+ruleResult.message());
            GameCommandResult result=provider.commandHandler()
                    .orElseThrow(()->new IllegalStateException("game command handler is unavailable"))
                    .handle(room,command);
            OperationDeadline deadline=room.authoritativeSession()
                    .map(com.aoo.bcg.gamespi.AuthoritativeGameSession::operationDeadline)
                    .orElse(OperationDeadline.none());
            result=result.withTiming(time.epochMillis(),deadline);
            if(room.authoritativeSession().isPresent())result=result.withAuthorityMetadata(
                    room.requireAuthoritativeSession().stateVersion(),frame.seq());
            timeline.complete(roomId,frame.seq(),frame.requestId(),frame.msgId(),deadline);
            return new RoutedResult(accepted,result,false,false);
        }catch(RuntimeException|Error failure){
            timeline.fail(roomId,frame.seq(),frame.requestId(),frame.msgId(),failure);
            throw failure;
        }
    }

    private static boolean isReadOnly(WebSocketFrame frame) {
        String action=String.valueOf(frame.body().getOrDefault("action",frame.msgId()))
                .strip().toLowerCase(java.util.Locale.ROOT);
        return action.endsWith(".state_req")||action.equals("state")
                ||action.endsWith(".hint_req")||action.equals("hint");
    }

    private static ConnectionSession rebindAfterSit(ConnectionSession session,WebSocketFrame frame,
            GameCommandResult result) {
        Map<String,Object> command=frame.body();
        String action=frame.msgId();
        Object nestedAction=command.get("action");
        if(nestedAction instanceof String value&&!value.isBlank())action=value;
        if(!action.toLowerCase(java.util.Locale.ROOT).endsWith(".sit_req"))return session;
        Map<String,Object> view=result.body().asMap();
        Object rawViewerSeat=view.get("viewerSeat");
        int confirmed=rawViewerSeat instanceof Number number?number.intValue()
                : authoritativeSeatForViewer(view,session.userId());
        String role=String.valueOf(view.getOrDefault("viewerRole",view.getOrDefault("viewerStatus","")));
        if(confirmed<0||!("SEATED".equals(role))||!authoritativeSeatBelongsToViewer(view,confirmed,session.userId()))
            throw new SecurityException("sit result did not confirm the authenticated viewer's authoritative seat");
        return session.withSeatId(confirmed);
    }

    private RoutedResult replay(ConnectionSession session,WebSocketFrame frame,long roomId,GameCommandResult result) {
        if(!session.roomId().equals(frame.roomId()))throw new SecurityException("room mismatch");
        if(!session.playVersion().equals(frame.playVersion()))throw new SecurityException("play version mismatch");
        GameRoomHandle room=rooms.require(roomId);
        if(!room.playVersion().equals(frame.playVersion()))throw new SecurityException("room play version mismatch");
        return new RoutedResult(rebindAfterSit(session,frame,result),result,true,false);
    }

    private static int authoritativeSeatForViewer(Map<String,Object> view,String userId) {
        Object seats=view.containsKey("players")?view.get("players"):view.get("seats");
        if(!(seats instanceof Map<?,?> map))return -1;
        for(Map.Entry<?,?> entry:map.entrySet()){
            Object occupant=entry.getValue();
            if(occupant instanceof Map<?,?> seatView)occupant=seatView.get("playerId");
            if(sameIdentity(occupant,userId)){
                try{return Integer.parseInt(String.valueOf(entry.getKey()));}
                catch(NumberFormatException ignored){return -1;}
            }
        }
        return -1;
    }

    private static boolean authoritativeSeatBelongsToViewer(Map<String,Object> view,int seat,String userId) {
        Object seats=view.containsKey("players")?view.get("players"):view.get("seats");
        if(!(seats instanceof Map<?,?> map))return false;
        Object occupant=map.get(seat);
        if(occupant==null)occupant=map.get(String.valueOf(seat));
        if(occupant instanceof Map<?,?> seatView)occupant=seatView.get("playerId");
        return sameIdentity(occupant,userId);
    }

    private static boolean sameIdentity(Object occupant,String userId) {
        if(occupant instanceof Number number){
            try{return number.longValue()==Long.parseLong(userId);}
            catch(NumberFormatException ignored){return false;}
        }
        return occupant!=null&&String.valueOf(occupant).equals(userId);
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

    public record RoutedResult(ConnectionSession session,GameCommandResult result,boolean replayed,boolean broadcast) { }
    public record RequestOutcome(String status,GameCommandResult result,String errorCode) { }
    public static final class RequestOutcomeUnknownException extends IllegalStateException {
        public RequestOutcomeUnknownException(String requestId){super("REQUEST_OUTCOME_UNKNOWN:"+requestId);}
    }
}
