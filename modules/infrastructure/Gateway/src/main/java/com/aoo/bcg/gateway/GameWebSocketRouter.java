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
import com.aoo.bcg.gamespi.GameIdentityPolicy;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gamespi.RuleChainExecutor;
import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionValidator;
import com.aoo.bcg.gamespi.api.DeprecatedEntryPointBlocklist;

import java.util.Objects;
import java.util.Map;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
    private final RuntimeMetadataProjection runtimeMetadataProjection;
    private final AtomicReference<ReconnectAuthority> reconnectAuthority = new AtomicReference<>();
    private volatile java.util.function.LongConsumer roomAvailability = roomId -> {};
    private volatile java.util.function.BiFunction<Long,java.util.function.Supplier<RoutedResult>,RoutedResult> roomReadAvailability=(id,work)->work.get();
    public void configureRoomAvailability(java.util.function.LongConsumer guard,java.util.function.BiFunction<Long,java.util.function.Supplier<RoutedResult>,RoutedResult> read){roomAvailability=Objects.requireNonNull(guard);roomReadAvailability=Objects.requireNonNull(read);}
    private final AtomicReference<RoomCommandAuthority> roomCommandAuthority = new AtomicReference<>();
    private final AtomicReference<ReadyAuthority> readyAuthority = new AtomicReference<>();
    private final AtomicReference<ExecuteAuthority> executeAuthority = new AtomicReference<>();
    private final GameOperationTimeline timeline = new GameOperationTimeline(256);

    @FunctionalInterface
    public interface ReconnectAuthority {
        RoomOrchestrator.ReconnectResult reconnect(RoomOrchestrator.ReconnectCommand command);
    }

    /**
     * Typed authority for room membership and dissolve commands.  The callback is
     * deliberately supplied by Bootstrap so this router cannot manufacture room
     * identity or fall back to a gameplay handler.
     */
    @FunctionalInterface
    public interface RoomCommandAuthority {
        GameCommandResult route(ConnectionSession session, WebSocketFrame frame,
                                GameRoomHandle room);
    }

    /** Typed authority for the existing common.room.ready_req ingress. */
    @FunctionalInterface
    public interface ReadyAuthority {
        RoomOrchestrator.ReadyResult ready(ConnectionSession session, WebSocketFrame frame,
                                           GameRoomHandle room);
    }

    /**
     * Typed authority for current production gameplay execute. Compatibility-origin
     * rooms are deliberately excluded before this callback is reached.
     */
    @FunctionalInterface
    public interface ExecuteAuthority {
        RoomOrchestrator.ExecuteResult execute(ConnectionSession session, WebSocketFrame frame,
                                                GameRoomHandle room);
    }

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
        this(games, rooms, guard, idempotency, retention, runtimeCommitter, time, risk, null);
    }
    /** Release-wired constructor carrying the immutable canonical projection. */
    public GameWebSocketRouter(GameRegistry games, GameRoomResolver rooms, WebSocketRequestGuard guard,
                               IdempotencyStore<GameCommandResult> idempotency, Duration retention,
                               GameCommandCommitter runtimeCommitter, AuthoritativeTimeSource time,
                               RiskAdmissionAuthority risk, RuntimeMetadataProjection runtimeMetadataProjection) {
        this.games = Objects.requireNonNull(games);
        this.rooms = Objects.requireNonNull(rooms);
        this.guard = Objects.requireNonNull(guard);
        this.idempotency = Objects.requireNonNull(idempotency);
        this.runtimeCommitter = Objects.requireNonNull(runtimeCommitter);
        this.time = Objects.requireNonNull(time);
        this.risk = risk;
        RuntimeMetadataProjection boundProjection = games.runtimeMetadataProjection().orElse(null);
        if (runtimeMetadataProjection != null) {
            RuntimeMetadataProjectionValidator.validate(runtimeMetadataProjection);
            if (boundProjection != null && !boundProjection.equals(runtimeMetadataProjection)) {
                throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_SOURCE_CONFLICT");
            }
        }
        this.runtimeMetadataProjection = runtimeMetadataProjection != null
                ? runtimeMetadataProjection : boundProjection;
        if (retention == null || retention.isNegative() || retention.isZero())
            throw new IllegalArgumentException("idempotency retention must be positive");
        this.retention = retention;
    }

    public RuntimeMetadataProjection requireRuntimeMetadataProjection() {
        if (runtimeMetadataProjection == null) throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_REQUIRED");
        return runtimeMetadataProjection;
    }

    public RoutedResult route(ConnectionSession session, WebSocketFrame frame) {
        DeprecatedEntryPointBlocklist.requireMessageAllowed(frame.msgId());
        long scopedRoomId;
        try{scopedRoomId=Long.parseLong(frame.roomId());}catch(NumberFormatException error){throw new IllegalArgumentException("numeric roomId required",error);}
        return RoomOrderedExecutor.global().execute(scopedRoomId,()->routeOrdered(session,frame,scopedRoomId));
    }

    /** Configure the sole reconnect authority exactly once during production assembly. */
    public void configureReconnectAuthority(ReconnectAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        if (!reconnectAuthority.compareAndSet(null, authority)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_AUTHORITY_ALREADY_CONFIGURED");
        }
    }

    /** Configure the sole typed room-command authority exactly once during production assembly. */
    public void configureRoomCommandAuthority(RoomCommandAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        if (!roomCommandAuthority.compareAndSet(null, authority)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_ROOM_COMMAND_AUTHORITY_ALREADY_CONFIGURED");
        }
    }

    /** Configure the sole current-production Ready authority exactly once. */
    public void configureReadyAuthority(ReadyAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        if (!readyAuthority.compareAndSet(null, authority)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_AUTHORITY_ALREADY_CONFIGURED");
        }
    }

    /** Configure the sole current-gameplay execute authority exactly once. */
    public void configureExecuteAuthority(ExecuteAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        if (!executeAuthority.compareAndSet(null, authority)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_AUTHORITY_ALREADY_CONFIGURED");
        }
    }

    private RoutedResult routeOrdered(ConnectionSession session,WebSocketFrame frame,long scopedRoomId) {
        if(isReadOnly(frame))return roomReadAvailability.apply(scopedRoomId,()->routeAvailable(session,frame,scopedRoomId));
        roomAvailability.accept(scopedRoomId);return routeAvailable(session,frame,scopedRoomId);
    }
    private RoutedResult routeAvailable(ConnectionSession session,WebSocketFrame frame,long scopedRoomId) {
        if ("room.reconnect".equals(frame.msgId())) {
            return routeReconnect(session, frame, scopedRoomId);
        }
        if (isTypedRoomCommand(frame.msgId())) {
            return routeTypedRoomCommand(session, frame, scopedRoomId);
        }
        if (isCurrentGameplayReady(frame, scopedRoomId)) {
            return routeGameplayReady(session, frame, scopedRoomId);
        }
        if (isCurrentGameplayExecute(frame, scopedRoomId)) {
            return routeGameplayExecute(session, frame, scopedRoomId);
        }
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
        requireRuntimeMetadata(provider);
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

    private void requireRuntimeMetadata(GameProvider provider) {
        if (runtimeMetadataProjection == null) return;
        RuntimeMetadataProjectionValidator.validate(provider.descriptor(), runtimeMetadataProjection);
    }

    private RoutedResult routeTypedRoomCommand(ConnectionSession session, WebSocketFrame frame,
                                               long roomId) {
        try (OperationLogContext ignored = OperationLogContext.open(frame.traceId(), frame.requestId(),
                roomId, session.userId(), session.seatId(), session.connectionId())) {
            timeline.begin(roomId, frame.seq(), frame.requestId(), frame.msgId());
            ConnectionSession accepted = guard.validate(session, frame);
            GameRoomHandle room = rooms.require(roomId);
            if (!room.playVersion().equals(frame.playVersion())) {
                throw new SecurityException("room play version mismatch");
            }
            RoomCommandAuthority authority = roomCommandAuthority.get();
            if (authority == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_ROOM_COMMAND_AUTHORITY_UNAVAILABLE");
            }
            GameCommandResult result = authority.route(accepted, frame, room);
            if (result == null || !frame.requestId().equals(result.requestId())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_ROOM_COMMAND_RESULT_REQUIRED");
            }
            timeline.complete(roomId, frame.seq(), frame.requestId(), frame.msgId(),
                    room.authoritativeSession()
                            .map(com.aoo.bcg.gamespi.AuthoritativeGameSession::operationDeadline)
                            .orElse(OperationDeadline.none()));
            return new RoutedResult(accepted, result, false, false);
        } catch (RuntimeException | Error failure) {
            timeline.fail(roomId, frame.seq(), frame.requestId(), frame.msgId(), failure);
            throw failure;
        }
    }

    private static boolean isTypedRoomCommand(String msgId) {
        return "room.leave".equals(msgId)
                || "room.dissolve_apply".equals(msgId)
                || "room.dissolve_vote".equals(msgId)
                || "room.handoff".equals(msgId);
    }

    /**
     * The common Ready message is selected before the generic provider handler only for
     * production-origin rooms.  Compatibility-origin rooms retain their explicit legacy
     * handler and are never allowed to reach the current typed authority.
     */
    private boolean isCurrentGameplayReady(WebSocketFrame frame, long roomId) {
        if (!"common.room.ready_req".equals(frame.msgId())) return false;
        GameRoomHandle room = rooms.require(roomId);
        return games.isProduction() && room.compatibilityOrigin() == null;
    }

    private RoutedResult routeGameplayReady(ConnectionSession session, WebSocketFrame frame,
                                            long roomId) {
        try (OperationLogContext ignored = OperationLogContext.open(frame.traceId(), frame.requestId(),
                roomId, session.userId(), session.seatId(), session.connectionId())) {
            timeline.begin(roomId, frame.seq(), frame.requestId(), frame.msgId());
            ConnectionSession accepted = guard.validate(session, frame);
            GameRoomHandle room = rooms.require(roomId);
            if (!room.playVersion().equals(frame.playVersion())) {
                throw new SecurityException("room play version mismatch");
            }
            ReadyAuthority authority = readyAuthority.get();
            if (authority == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_AUTHORITY_UNAVAILABLE");
            }
            RoomRuntimeIdentity expected = room.runtimeIdentity();
            if (expected == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RUNTIME_IDENTITY_REQUIRED");
            }
            RoomOrchestrator.ReadyResult result = authority.ready(accepted, frame, room);
            if (result == null || result.identity() == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RESULT_REQUIRED");
            }
            RoomOrchestrator.RoomIdentity identity = result.identity();
            long accountId;
            try {
                accountId = Long.parseLong(accepted.userId());
            } catch (NumberFormatException invalidAccount) {
                throw new SecurityException("ROOM_ORCHESTRATOR_READY_ACCOUNT_INVALID", invalidAccount);
            }
            if (identity.roomId() != roomId
                    || identity.gameId() != room.gameId()
                    || !identity.playVersion().equals(room.playVersion())
                    || !identity.roomInstanceId().equals(expected.roomInstanceId())
                    || identity.lifecycleEpoch() != expected.lifecycleEpoch()
                    || identity.fencingToken() != expected.fencingToken()
                    || identity.stateVersion() != expected.stateVersion()
                    || result.accountId() != accountId
                    || result.seatNo() != accepted.seatId()
                    || !frame.requestId().equals(result.commandId())
                    || result.expectedNextStateVersion() != Math.addExact(expected.stateVersion(), 1L)) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RESULT_IDENTITY_CONFLICT");
            }
            var authoritative = room.requireAuthoritativeSession();
            if (authoritative.stateVersion() != result.expectedNextStateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_STATE_VERSION_CONFLICT");
            }
            GameCommandResult response = new GameCommandResult(
                    "common.room.ready_resp", frame.requestId(), authoritative.viewFor(accountId))
                    .withTiming(time.epochMillis(), authoritative.operationDeadline());
            timeline.complete(roomId, frame.seq(), frame.requestId(), frame.msgId(),
                    authoritative.operationDeadline());
            return new RoutedResult(accepted, response, false, true);
        } catch (RuntimeException | Error failure) {
            timeline.fail(roomId, frame.seq(), frame.requestId(), frame.msgId(), failure);
            throw failure;
        }
    }

    /**
     * The production execute seam is selected only for a published current identity.
     * All compatibility-origin and lifecycle/read-only messages remain on their existing
     * explicit paths. A current room without the required runtime identity is routed to
     * the typed boundary so that it fails closed instead of falling through to a handler.
     */
    private boolean isCurrentGameplayExecute(WebSocketFrame frame, long roomId) {
        if (isReadOnly(frame)
                || frame.msgId().startsWith("room.")
                || "common.room.join_req".equals(frame.msgId())
                || "common.room.ready_req".equals(frame.msgId())
                || "room.create".equals(frame.msgId())) {
            return false;
        }
        GameRoomHandle room = rooms.require(roomId);
        return games.isProduction()
                && room.compatibilityOrigin() == null
                && GameIdentityPolicy.isCurrentPublished(room.gameId(), room.playVersion());
    }

    private RoutedResult routeGameplayExecute(ConnectionSession session, WebSocketFrame frame,
                                              long roomId) {
        try (OperationLogContext ignored = OperationLogContext.open(frame.traceId(), frame.requestId(),
                roomId, session.userId(), session.seatId(), session.connectionId())) {
            timeline.begin(roomId, frame.seq(), frame.requestId(), frame.msgId());
            ConnectionSession accepted = guard.validate(session, frame);
            GameRoomHandle room = rooms.require(roomId);
            if (!room.playVersion().equals(frame.playVersion())) {
                throw new SecurityException("room play version mismatch");
            }
            ExecuteAuthority authority = executeAuthority.get();
            if (authority == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_AUTHORITY_UNAVAILABLE");
            }
            RoomRuntimeIdentity expected = room.runtimeIdentity();
            if (expected == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RUNTIME_IDENTITY_REQUIRED");
            }
            RoomOrchestrator.ExecuteResult authorityResult = authority.execute(accepted, frame, room);
            if (authorityResult == null || authorityResult.identity() == null
                    || authorityResult.commandResult() == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RESULT_REQUIRED");
            }
            RoomOrchestrator.RoomIdentity identity = authorityResult.identity();
            GameCommandResult result = authorityResult.commandResult();
            if (!frame.requestId().equals(result.requestId())
                    || identity.roomId() != roomId
                    || identity.gameId() != room.gameId()
                    || !identity.playVersion().equals(room.playVersion())
                    || !identity.roomInstanceId().equals(expected.roomInstanceId())
                    || identity.lifecycleEpoch() != expected.lifecycleEpoch()
                    || identity.fencingToken() != expected.fencingToken()
                    || identity.stateVersion() != Math.addExact(expected.stateVersion(), 1L)) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RESULT_IDENTITY_CONFLICT");
            }
            OperationDeadline deadline = room.authoritativeSession()
                    .map(com.aoo.bcg.gamespi.AuthoritativeGameSession::operationDeadline)
                    .orElse(OperationDeadline.none());
            timeline.complete(roomId, frame.seq(), frame.requestId(), frame.msgId(), deadline);
            return new RoutedResult(accepted, result, false, true);
        } catch (RuntimeException | Error failure) {
            timeline.fail(roomId, frame.seq(), frame.requestId(), frame.msgId(), failure);
            throw failure;
        }
    }

    private RoutedResult routeReconnect(ConnectionSession session, WebSocketFrame frame, long roomId) {
        try (OperationLogContext ignored = OperationLogContext.open(frame.traceId(), frame.requestId(), roomId,
                session.userId(), session.seatId(), session.connectionId())) {
            timeline.begin(roomId, frame.seq(), frame.requestId(), frame.msgId());
            ConnectionSession accepted = guard.validate(session, frame);
            GameRoomHandle room = rooms.require(roomId);
            if (!room.playVersion().equals(frame.playVersion())) {
                throw new SecurityException("room play version mismatch");
            }
            ReconnectAuthority authority = reconnectAuthority.get();
            if (authority == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_AUTHORITY_UNAVAILABLE");
            }
            RoomOrchestrator.ReconnectCommand command = reconnectCommand(accepted, frame, room);
            RoomOrchestrator.ReconnectResult result = authority.reconnect(command);
            if (result == null || result.identity() == null || result.state() == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_RESULT_REQUIRED");
            }
            if (result.identity().roomId() != roomId
                    || result.identity().gameId() != room.gameId()
                    || !result.identity().playVersion().equals(room.playVersion())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_RESULT_IDENTITY_CONFLICT");
            }
            Object rawStateVersion = result.state().asMap().get("stateVersion");
            long snapshotStateVersion;
            try {
                snapshotStateVersion = ProtocolValuePolicy.parseCanonicalLong(
                        rawStateVersion, "stateVersion", false);
            } catch (IllegalArgumentException invalidStateVersion) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_SNAPSHOT_VERSION_INVALID",
                        invalidStateVersion);
            }
            if (snapshotStateVersion != result.identity().stateVersion()
                    || snapshotStateVersion < command.lastKnownStateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_SNAPSHOT_VERSION_CONFLICT");
            }
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("viewerSnapshot", result.state().asMap());
            body.put("events", List.of());
            body.put("serverSeq", result.identity().stateVersion());
            body.put("roomInstanceId", result.identity().roomInstanceId());
            body.put("lifecycleEpoch", Long.toString(result.identity().lifecycleEpoch()));
            body.put("sessionId", command.sessionId());
            body.put("stateVersion", result.identity().stateVersion());
            body.put("eventSequence", Long.toString(command.eventSequence()));
            body.put("eventId", command.eventId());
            body.put("hasMore", false);
            GameCommandResult response = new GameCommandResult(
                    "room.reconnect_resp", frame.requestId(), body);
            timeline.complete(roomId, frame.seq(), frame.requestId(), frame.msgId(),
                    room.authoritativeSession().map(com.aoo.bcg.gamespi.AuthoritativeGameSession::operationDeadline)
                            .orElse(OperationDeadline.none()));
            return new RoutedResult(accepted, response, false, false);
        } catch (RuntimeException | Error failure) {
            timeline.fail(roomId, frame.seq(), frame.requestId(), frame.msgId(), failure);
            throw failure;
        }
    }

    private static RoomOrchestrator.ReconnectCommand reconnectCommand(
            ConnectionSession session, WebSocketFrame frame, GameRoomHandle room) {
        Map<String, Object> body = frame.body();
        Map<String, Object> payload = requiredObject(body, "payload");
        String action = requiredText(body, "action");
        if (!frame.msgId().equals(action)) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_RECONNECT_ACTION_CONFLICT");
        }
        String bodyRoomId = requiredText(body, "roomId");
        if (!frame.roomId().equals(bodyRoomId)) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_ROOM_CONFLICT");
        }
        String sessionId = requiredText(payload, "sessionId");
        if (!session.connectionId().equals(sessionId)) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_SESSION_CONFLICT");
        }
        long gameId = requiredLong(payload, "gameId", false);
        String gameCode = requiredText(payload, "gameCode");
        String roomInstanceId = requiredText(payload, "roomInstanceId");
        long lifecycleEpoch = requiredLong(payload, "lifecycleEpoch", true);
        long fencingToken = requiredLong(payload, "fencingToken", true);
        long stateVersion = requiredLong(payload, "stateVersion", false);
        long lastKnownStateVersion = requiredLong(body, "lastServerSeq", false);
        long eventSequence = requiredLong(payload, "eventSequence", false);
        String eventId = requiredText(payload, "eventId");
        if (!eventId.equals(frame.requestId())) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_EVENT_ID_CONFLICT");
        }
        if (eventSequence != lastKnownStateVersion) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_EVENT_SEQUENCE_CONFLICT");
        }
        String reconnectToken = requiredText(body, "reconnectToken");
        RoomSnapshotMetadataSource metadata = new RoomSnapshotMetadataSource(
                requiredLong(payload, "releaseId", true),
                requiredText(payload, "ruleContentHash"),
                requiredText(payload, "componentContentHash"),
                requiredText(payload, "cardCodecVersion"),
                requiredText(payload, "eventInterpreterVersion"),
                requiredText(payload, "protocolVersion"),
                requiredText(payload, "snapshotSchemaVersion"),
                requiredLong(payload, "indexGeneration", true),
                requiredText(payload, "regionCode"),
                requiredText(payload, "sourceHash"));
        long accountId;
        try {
            accountId = Long.parseLong(session.userId());
        } catch (NumberFormatException failure) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_ACCOUNT_INVALID", failure);
        }
        if (accountId <= 0 || session.seatId() < 0) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_PERMISSION_DENIED");
        }
        if (gameId != room.gameId() || !gameCode.equals(
                payload.get("gameCode")) || !frame.playVersion().equals(room.playVersion())) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_STATIC_IDENTITY_CONFLICT");
        }
        RoomOrchestrator.RoomIdentity expected = new RoomOrchestrator.RoomIdentity(
                room.roomId(), roomInstanceId, Math.toIntExact(gameId), gameCode,
                frame.playVersion(), lifecycleEpoch, fencingToken, stateVersion);
        return new RoomOrchestrator.ReconnectCommand(expected, sessionId, frame.requestId(),
                accountId, session.seatId(), reconnectToken, lastKnownStateVersion, metadata,
                eventSequence, eventId);
    }

    private static Map<String, Object> requiredObject(Map<String, Object> source, String key) {
        Object raw = source.get(key);
        if (!(raw instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_RECONNECT_" + key.toUpperCase() + "_REQUIRED");
        }
        Map<String, Object> copy = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : object.entrySet()) {
            if (!(entry.getKey() instanceof String name)) {
                throw new IllegalArgumentException("ROOM_ORCHESTRATOR_RECONNECT_PAYLOAD_KEY_INVALID");
            }
            copy.put(name, entry.getValue());
        }
        return Map.copyOf(copy);
    }

    private static String requiredText(Map<String, Object> source, String key) {
        Object raw = source.get(key);
        if (!(raw instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_RECONNECT_" + key.toUpperCase() + "_REQUIRED");
        }
        return text;
    }

    private static long requiredLong(Map<String, Object> source, String key, boolean positive) {
        Object raw = source.get(key);
        try {
            return ProtocolValuePolicy.parseCanonicalLong(raw,
                    "ROOM_ORCHESTRATOR_RECONNECT_" + key.toUpperCase(), positive);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_RECONNECT_" + key.toUpperCase()
                    + "_INVALID", invalid);
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
            requireRuntimeMetadata(provider);
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
        return action.endsWith(".state_req")||action.equals("state");
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
