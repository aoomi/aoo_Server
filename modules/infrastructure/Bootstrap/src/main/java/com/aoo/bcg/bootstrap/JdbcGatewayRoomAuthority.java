package com.aoo.bcg.bootstrap;

import com.aoo.bcg.config.RoomConfigurationLeaseStore;
import com.aoo.bcg.config.RoomConfigurationSnapshotMetadataProvider;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import com.aoo.bcg.common.authority.ProductionRoomOrchestratorAdapter;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.GameIdentityPolicy;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gamespi.RoomState;
import com.aoo.bcg.gamespi.CommandPayload;
import com.aoo.bcg.gateway.GatewayRuntimeProvider;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.gateway.GatewayRoomBroadcastHub;
import com.aoo.bcg.gamespi.RoomLifecycleAuthority;
import com.aoo.bcg.gateway.WaitingRoomExpirationPolicy;
import com.aoo.bcg.gateway.RoomAuthorityBusinessError;
import com.aoo.bcg.gateway.GatewaySessionRegistry;
import com.aoo.bcg.gateway.RoomOrderedExecutor;
import com.aoo.bcg.gateway.ProtocolValuePolicy;
import com.aoo.bcg.gateway.ConnectionSession;
import com.aoo.bcg.gateway.WebSocketFrame;
import com.aoo.bcg.common.idempotency.IdempotencyKey;
import com.aoo.bcg.common.idempotency.JdbcIdempotencyStore;
import com.aoo.bcg.common.authority.JdbcRoomDissolveAuthorityStore;
import com.aoo.bcg.common.authority.RoomDissolveAuthorityStore;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameCommandHandler;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Persistent, fenced owner of game-room runtime instances. */
final class JdbcGatewayRoomAuthority implements GatewayRuntimeProvider.RoomAuthority,
        ProductionRoomOrchestratorAdapter.AuthorityPort, AutoCloseable {
    /** Gameplay deadlines need frame-scale resolution; durable maintenance remains one-second work. */
    private static final long LIFECYCLE_TICK_MILLIS = 50;
    private static final long MAINTENANCE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private final DataSource dataSource;
    private final RoomConfigurationSnapshotMetadataProvider roomMetadata;
    private final RoomRuntimeIdentityStore roomIdentityStore;
    private final RoomDissolveAuthorityStore dissolveAuthority;
    private final GameRegistry games;
    private final RuntimeGameRoomRegistry rooms;
    private final String nodeId;
    private final String endpoint;
    private final Clock clock;
    private final ObjectMapper json;
    private final JdbcGatewayGameCommandCommitter stateStore;
    private final GatewayRoomBroadcastHub broadcasts;
    private final HallRoomLifecycleClient hallLifecycle;
    private final ProductionRoomOrchestratorAdapter roomOrchestrator;
    private final JdbcIdempotencyStore<GameCommandResult> executeIdempotency;
    private final java.time.Duration executeIdempotencyRetention;
    private final ConcurrentHashMap<Long, Object> roomLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService leaseKeeper;
    private final ScheduledExecutorService lifecycleWorker;
    private final SecureRandom identityRandom = new SecureRandom();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, RoomOrchestrator.LeaveResult>>
            leaveReceipts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Boolean>>
            reconnectReceipts = new ConcurrentHashMap<>();
    /**
     * Process-local handoff lease projection.  There is deliberately no implicit
     * session/generation seed: until a future transport registers the currently
     * owned session, handoff remains fail-closed after restart or on a cold node.
     */
    private final ConcurrentHashMap<Long, HandoffLease> handoffLeases = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, HandoffReceipt>>
            handoffReceipts = new ConcurrentHashMap<>();
    private long nextMaintenanceAtNanos;

    JdbcGatewayRoomAuthority(DataSource dataSource, GameRegistry games, RuntimeGameRoomRegistry rooms,
                             Clock clock, String nodeId, String endpoint, ObjectMapper json,
                             JdbcGatewayGameCommandCommitter stateStore,
                             GatewayRoomBroadcastHub broadcasts,
                             HallRoomLifecycleClient hallLifecycle) {
        this(dataSource, games, rooms, clock, nodeId, endpoint, json, stateStore, broadcasts,
                hallLifecycle, java.time.Duration.ofHours(24), new RoomConfigurationSnapshotMetadataProvider(
                        new RoomConfigurationLeaseStore(dataSource, json)),
                new RoomRuntimeIdentityStore(dataSource));
    }

    JdbcGatewayRoomAuthority(DataSource dataSource, GameRegistry games, RuntimeGameRoomRegistry rooms,
                             Clock clock, String nodeId, String endpoint, ObjectMapper json,
                             JdbcGatewayGameCommandCommitter stateStore,
                             GatewayRoomBroadcastHub broadcasts,
                             HallRoomLifecycleClient hallLifecycle,
                             RoomConfigurationSnapshotMetadataProvider roomMetadata) {
        this(dataSource, games, rooms, clock, nodeId, endpoint, json, stateStore, broadcasts,
                             hallLifecycle, java.time.Duration.ofHours(24), roomMetadata,
                new RoomRuntimeIdentityStore(dataSource),
                new JdbcRoomDissolveAuthorityStore(dataSource, json, clock));
    }

    JdbcGatewayRoomAuthority(DataSource dataSource, GameRegistry games, RuntimeGameRoomRegistry rooms,
                             Clock clock, String nodeId, String endpoint, ObjectMapper json,
                             JdbcGatewayGameCommandCommitter stateStore,
                             GatewayRoomBroadcastHub broadcasts,
                             HallRoomLifecycleClient hallLifecycle,
                             RoomConfigurationSnapshotMetadataProvider roomMetadata,
                             RoomRuntimeIdentityStore roomIdentityStore) {
        this(dataSource, games, rooms, clock, nodeId, endpoint, json, stateStore, broadcasts,
                hallLifecycle, java.time.Duration.ofHours(24), roomMetadata, roomIdentityStore);
    }

    JdbcGatewayRoomAuthority(DataSource dataSource, GameRegistry games, RuntimeGameRoomRegistry rooms,
                             Clock clock, String nodeId, String endpoint, ObjectMapper json,
                             JdbcGatewayGameCommandCommitter stateStore,
                             GatewayRoomBroadcastHub broadcasts,
                             HallRoomLifecycleClient hallLifecycle,
                             java.time.Duration executeIdempotencyRetention,
                             RoomConfigurationSnapshotMetadataProvider roomMetadata,
                             RoomRuntimeIdentityStore roomIdentityStore) {
        this(dataSource, games, rooms, clock, nodeId, endpoint, json, stateStore, broadcasts,
                hallLifecycle, executeIdempotencyRetention, roomMetadata, roomIdentityStore,
                new JdbcRoomDissolveAuthorityStore(dataSource, json, clock));
    }

    JdbcGatewayRoomAuthority(DataSource dataSource, GameRegistry games, RuntimeGameRoomRegistry rooms,
                             Clock clock, String nodeId, String endpoint, ObjectMapper json,
                             JdbcGatewayGameCommandCommitter stateStore,
                             GatewayRoomBroadcastHub broadcasts,
                             HallRoomLifecycleClient hallLifecycle,
                             java.time.Duration executeIdempotencyRetention,
                             RoomConfigurationSnapshotMetadataProvider roomMetadata,
                             RoomRuntimeIdentityStore roomIdentityStore,
                             RoomDissolveAuthorityStore dissolveAuthority) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.roomMetadata = Objects.requireNonNull(roomMetadata);
        this.roomIdentityStore = Objects.requireNonNull(roomIdentityStore);
        this.dissolveAuthority = Objects.requireNonNull(dissolveAuthority);
        this.games = Objects.requireNonNull(games);
        this.rooms = Objects.requireNonNull(rooms);
        this.clock = Objects.requireNonNull(clock);
        this.json = Objects.requireNonNull(json);
        this.stateStore = Objects.requireNonNull(stateStore);
        this.broadcasts = Objects.requireNonNull(broadcasts);
        this.hallLifecycle = Objects.requireNonNull(hallLifecycle);
        this.executeIdempotencyRetention = Objects.requireNonNull(executeIdempotencyRetention);
        if (executeIdempotencyRetention.isZero() || executeIdempotencyRetention.isNegative()) {
            throw new IllegalArgumentException("execute idempotency retention must be positive");
        }
        this.executeIdempotency = new JdbcIdempotencyStore<>(dataSource, json,
                GameCommandResult.class, clock);
        this.roomOrchestrator = new ProductionRoomOrchestratorAdapter(this);
        this.nodeId = required(nodeId, "nodeId");
        this.endpoint = required(endpoint, "endpoint");
        this.leaseKeeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-room-authority-lease");
            thread.setDaemon(true);
            return thread;
        });
        this.lifecycleWorker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-room-authority-lifecycle");
            thread.setDaemon(true);
            return thread;
        });
        recoverDurableRooms();
        finishInterruptedRemovals();
        this.nextMaintenanceAtNanos = System.nanoTime() + MAINTENANCE_INTERVAL_NANOS;
        this.leaseKeeper.scheduleWithFixedDelay(this::renewActiveLeases, 30, 30, TimeUnit.SECONDS);
        this.lifecycleWorker.scheduleWithFixedDelay(
                this::processRoomLifecycles,
                LIFECYCLE_TICK_MILLIS,
                LIFECYCLE_TICK_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> command) {
        return legacyResponse(roomOrchestrator.create(toCreateCommand(command)));
    }

    @Override
    public Map<String, Object> recover(Map<String, Object> command) {
        return legacyResponse(roomOrchestrator.recover(toRecoverCommand(command)));
    }

    @Override
    public Map<String, Object> leave(Map<String, Object> command) {
        return leaveThroughRoomOrchestrator(command);
    }

    /**
     * Bounded, read-only production readiness probe. It deliberately does not
     * inspect or repair room state, leases, snapshots, receipts, or events;
     * unavailable dependencies fail closed without exposing diagnostics.
     */
    @Override
    public GatewayRuntimeProvider.Readiness readiness() {
        return probeReadiness(dataSource, roomOrchestrator != null && rooms != null && games != null);
    }

    static GatewayRuntimeProvider.Readiness probeReadiness(DataSource source, boolean runtimeConstructed) {
        if (!runtimeConstructed || source == null) return GatewayRuntimeProvider.Readiness.notReady();
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
            statement.setQueryTimeout(2);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == 1
                        ? GatewayRuntimeProvider.Readiness.readyResult()
                        : GatewayRuntimeProvider.Readiness.notReady();
            }
        } catch (SQLException | RuntimeException failure) {
            return GatewayRuntimeProvider.Readiness.notReady();
        }
    }

    /** Production membership ingress must cross the single typed leave adapter. */
    Map<String, Object> leaveThroughRoomOrchestrator(Map<String, Object> command) {
        // Only the trusted Hall membership ingress reaches this HTTP-specific entry.
        // Recheck current account-session validity before a prior room receipt can replay.
        requireHttpLeaveActor(dataSource, command);
        return leaveResponse(roomOrchestrator.leave(toLeaveCommand(command)));
    }

    static void requireHttpLeaveActor(DataSource source, Map<String, Object> command) {
        Objects.requireNonNull(source, "source");
        if (command == null || !"ACCOUNT_HTTP".equals(command.get("actorType"))
                || !"AUTHENTICATED_ROOM_MEMBER".equals(command.get("permission"))
                || command.containsKey("sessionId") || command.containsKey("actorSessionId")) {
            throw new SecurityException("ROOM_ORCHESTRATOR_LEAVE_HTTP_ACTOR_REQUIRED");
        }
        long accountId = positive(command, "accountId");
        String accountSessionId = required(command, "accountSessionId");
        String sql = "SELECT s.account_id FROM aoo_account_session s "
                + "JOIN aoo_account a ON a.account_id=s.account_id "
                + "WHERE s.session_id=? AND s.account_id=? AND s.revoked_at IS NULL "
                + "AND s.access_expires_at>CURRENT_TIMESTAMP(3) "
                + "AND s.auth_generation=a.auth_generation "
                + "AND (a.banned_until IS NULL OR a.banned_until<=CURRENT_TIMESTAMP(3))";
        try (Connection connection = source.getConnection();
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, accountSessionId);
            query.setLong(2, accountId);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next() || result.getLong(1) != accountId) {
                    throw new SecurityException("ROOM_ORCHESTRATOR_LEAVE_ACTOR_SESSION_REJECTED");
                }
            }
        } catch (SQLException unavailable) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_ACTOR_SESSION_UNAVAILABLE", unavailable);
        }
    }

    /**
     * Gateway WebSocket room commands enter the same typed authority as HTTP and Hall.
     * The envelope is only used to bind request/room/version/trace values; all runtime,
     * static and snapshot metadata must still be supplied by the command payload and
     * checked against the canonical room authority before a mutation is attempted.
     */
    GameCommandResult routeTypedRoomCommand(ConnectionSession session, WebSocketFrame frame,
                                            GameRoomHandle room) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(room, "room");
        Map<String, Object> command = typedCommandBody(frame);
        requireTypedTransportIdentity(command, session, frame);
        return switch (frame.msgId()) {
            case "room.leave" -> {
                requireWsLeaveIdentityFields(command);
                requireLeaveEventIdentityFields(command);
                RoomOrchestrator.LeaveCommand typed = toLeaveCommand(command);
                requireTypedRuntimeIdentity(typed.expectedIdentity(), room);
                yield typedResponse(frame, leaveResponse(roomOrchestrator.leave(typed)));
            }
            case "room.dissolve_apply" -> {
                RoomOrchestrator.DissolveApplyCommand typed = toDissolveApplyCommand(command, room);
                yield typedResponse(frame, dissolveResponse(
                        roomOrchestrator.dissolveApply(typed), null));
            }
            case "room.dissolve_vote" -> {
                RoomOrchestrator.DissolveVoteCommand typed = toDissolveVoteCommand(command, room);
                yield typedResponse(frame, dissolveResponse(
                        roomOrchestrator.dissolveVote(typed), typed.agree()));
            }
            case "room.handoff" -> {
                RoomOrchestrator.HandoffCommand typed = toHandoffCommand(command, room);
                yield typedResponse(frame, handoffResponse(roomOrchestrator.handoff(typed), typed));
            }
            default -> throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_COMMAND_UNSUPPORTED");
        };
    }

    /**
     * Current production gameplay ingress. The router supplies only the authenticated
     * envelope and room handle; canonical metadata is loaded by the existing execute
     * authority and is never accepted from the client body.
     */
    RoomOrchestrator.ExecuteResult executeGameplayThroughRoomOrchestrator(
            ConnectionSession session, WebSocketFrame frame, GameRoomHandle room) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(room, "room");
        if (room.compatibilityOrigin() != null) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_COMPATIBILITY_ORIGIN_REJECTED");
        }
        if (!session.roomId().equals(frame.roomId())
                || !session.playVersion().equals(frame.playVersion())
                || !room.playVersion().equals(frame.playVersion())) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_TRANSPORT_IDENTITY_CONFLICT");
        }
        GameIdentityPolicy.requireCurrentPublished(room.gameId(), room.playVersion());
        RoomRuntimeIdentity runtime = room.runtimeIdentity();
        if (runtime == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RUNTIME_IDENTITY_REQUIRED");
        }
        long roomId;
        try {
            roomId = Long.parseLong(frame.roomId());
        } catch (NumberFormatException error) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_ROOM_ID_INVALID", error);
        }
        if (roomId != room.roomId()) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_ROOM_ID_CONFLICT");
        }
        GameProvider provider = games.require(room.gameId(), room.playVersion());
        RoomOrchestrator.RoomIdentity expected = new RoomOrchestrator.RoomIdentity(
                room.roomId(), runtime.roomInstanceId(), room.gameId(),
                provider.descriptor().code(), room.playVersion(), runtime.lifecycleEpoch(),
                runtime.fencingToken(), runtime.stateVersion());
        GameCommandRequest request = new GameCommandRequest(frame.msgId(), frame.requestId(), frame.seq(),
                room.roomId(), frame.roundNo(), frame.playVersion(), session.userId(), session.seatId(),
                frame.body());
        RoomOrchestrator.ExecuteResult result = executeThroughRoomOrchestrator(
                new RoomOrchestrator.ExecuteCommand(expected, session.connectionId(), request));
        if (result == null || result.identity() == null || result.commandResult() == null
                || !frame.requestId().equals(result.commandResult().requestId())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RESULT_REQUIRED");
        }
        RoomOrchestrator.RoomIdentity returned = result.identity();
        if (returned.roomId() != expected.roomId()
                || returned.gameId() != expected.gameId()
                || !returned.gameCode().equals(expected.gameCode())
                || !returned.playVersion().equals(expected.playVersion())
                || !returned.roomInstanceId().equals(expected.roomInstanceId())
                || returned.lifecycleEpoch() != expected.lifecycleEpoch()
                || returned.fencingToken() != expected.fencingToken()
                || returned.stateVersion() != Math.addExact(expected.stateVersion(), 1L)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RESULT_IDENTITY_CONFLICT");
        }
        return result;
    }

    /**
     * Current production Ready ingress.  The envelope is converted to the existing typed
     * command only after the durable room-lock projection, runtime identity, seated player
     * and explicit WAITING state have all been checked.  Compatibility-origin rooms never
     * reach this bridge.
     */
    RoomOrchestrator.ReadyResult readyGameplayThroughRoomOrchestrator(
            ConnectionSession session, WebSocketFrame frame, GameRoomHandle room) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(room, "room");
        if (room.compatibilityOrigin() != null) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_COMPATIBILITY_ORIGIN_REJECTED");
        }
        if (!session.roomId().equals(frame.roomId())
                || !session.playVersion().equals(frame.playVersion())
                || !room.playVersion().equals(frame.playVersion())) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_TRANSPORT_IDENTITY_CONFLICT");
        }
        GameIdentityPolicy.requireCurrentPublished(room.gameId(), room.playVersion());
        RoomOrchestrator.ReadyCommand command = toReadyCommand(session, frame, room);
        RoomOrchestrator.ReadyResult result = roomOrchestrator.ready(command);
        if (result == null || result.identity() == null
                || !frame.requestId().equals(result.commandId())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RESULT_REQUIRED");
        }
        return result;
    }

    private static GameCommandResult typedResponse(WebSocketFrame frame, Map<String, Object> body) {
        return new GameCommandResult(frame.msgId(), frame.requestId(), body);
    }

    static Map<String, Object> typedCommandBody(WebSocketFrame frame) {
        Map<String, Object> outer = frame.body();
        Object rawAction = outer.get("action");
        if (!(rawAction instanceof String action) || action.isBlank()) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_ACTION_REQUIRED");
        }
        Object rawPayload = outer.get("payload");
        if (!(rawPayload instanceof Map<?, ?> payload)) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_PAYLOAD_REQUIRED");
        }
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("action", action);
        for (Map.Entry<?, ?> entry : payload.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_PAYLOAD_KEY_INVALID");
            }
            command.put(key, entry.getValue());
        }
        // The frozen protocol keeps canonical command metadata inside payload. Only
        // the explicitly registered top-level dissolve fields remain accepted for
        // compatibility with the existing v2.0 contract; identity/metadata outside
        // payload is rejected instead of being silently trusted.
        Set<String> allowedTopLevel = new LinkedHashSet<>(Set.of("action", "payload"));
        if ("room.dissolve_apply".equals(frame.msgId())) {
            allowedTopLevel.add("reasonCode");
            requireTopLevelString(outer, "reasonCode");
        } else if ("room.dissolve_vote".equals(frame.msgId())) {
            allowedTopLevel.add("voteId");
            allowedTopLevel.add("agree");
            requireTopLevelString(outer, "voteId");
            requireTopLevelBoolean(outer, "agree");
        }
        for (String key : outer.keySet()) {
            if (!allowedTopLevel.contains(key)) {
                throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_TOP_LEVEL_FIELD_REJECTED:" + key);
            }
        }
        for (String key : allowedTopLevel) {
            if ("action".equals(key) || "payload".equals(key) || !outer.containsKey(key)) continue;
            Object topLevel = outer.get(key);
            Object nested = command.get(key);
            if (nested != null && !Objects.equals(nested, topLevel)) {
                throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_" + key.toUpperCase()
                        + "_CONFLICT");
            }
            // These registered dissolve fields are authoritative at the frozen
            // envelope level. A payload copy may only repeat the exact same value;
            // it can never satisfy a missing top-level field.
            command.put(key, topLevel);
        }
        // The WebSocket envelope is authoritative for these transport bindings. A
        // conflicting payload copy is rejected instead of silently preferring one.
        for (Map.Entry<String, String> envelope : Map.of(
                "roomId", frame.roomId(), "playVersion", frame.playVersion(),
                "requestId", frame.requestId(), "traceId", frame.traceId()).entrySet()) {
            Object supplied = outer.get(envelope.getKey());
            if (supplied != null && !String.valueOf(supplied).equals(envelope.getValue())) {
                throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_" + envelope.getKey().toUpperCase()
                        + "_CONFLICT");
            }
            Object payloadValue = command.get(envelope.getKey());
            if (payloadValue != null && !String.valueOf(payloadValue).equals(envelope.getValue())) {
                throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_" + envelope.getKey().toUpperCase()
                        + "_CONFLICT");
            }
            command.putIfAbsent(envelope.getKey(), envelope.getValue());
        }
        return Map.copyOf(command);
    }

    private static void requireTypedTransportIdentity(Map<String, Object> command,
                                                      ConnectionSession session,
                                                      WebSocketFrame frame) {
        String accountKey = switch (frame.msgId()) {
            case "room.leave" -> "accountId";
            case "room.dissolve_apply" -> "applicantAccountId";
            case "room.dissolve_vote" -> "voterAccountId";
            case "room.handoff" -> "ownerId";
            default -> throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_COMMAND_UNSUPPORTED");
        };
        long accountId = positive(command, accountKey);
        final long authenticatedAccount;
        try {
            authenticatedAccount = Long.parseLong(session.userId());
        } catch (NumberFormatException failure) {
            throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_ACCOUNT_INVALID", failure);
        }
        if (accountId != authenticatedAccount) {
            throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_ACCOUNT_CONFLICT");
        }
        String commandSession = "room.handoff".equals(frame.msgId())
                ? required(command, "fromSessionId") : required(command, "sessionId");
        if (!commandSession.equals(session.connectionId())
                || !required(command, "roomId").equals(frame.roomId())
                || !required(command, "playVersion").equals(frame.playVersion())
                || !required(command, "requestId").equals(frame.requestId())
                || !required(command, "traceId").equals(frame.traceId())) {
            throw new SecurityException("ROOM_ORCHESTRATOR_TYPED_TRANSPORT_IDENTITY_CONFLICT");
        }
    }

    private RoomOrchestrator.DissolveApplyCommand toDissolveApplyCommand(
            Map<String, Object> command, GameRoomHandle room) {
        required(command, "reasonCode");
        RoomOrchestrator.RoomIdentity identity = typedIdentity(command, room);
        RoomOrchestrator.RoomStaticIdentity staticIdentity = typedStaticIdentity(command, identity);
        long applicant = positive(command, "applicantAccountId");
        return new RoomOrchestrator.DissolveApplyCommand(identity, staticIdentity,
                required(command, "sessionId"), applicant,
                enumValue(command, "permission", RoomOrchestrator.DissolvePermission.class),
                enumValue(command, "roomState", RoomState.class), required(command, "voteId"),
                memberIds(command, "eligibleMemberIds"), instant(command, "deadline"),
                requiredBoolean(command, "approveOnTimeout"),
                requiredBoolean(command, "applicantAgrees"), nonNegative(command, "stateVersion"),
                required(command, "requestId"), required(command, "idempotencyKey"),
                metadata(command));
    }

    private RoomOrchestrator.DissolveVoteCommand toDissolveVoteCommand(
            Map<String, Object> command, GameRoomHandle room) {
        RoomOrchestrator.RoomIdentity identity = typedIdentity(command, room);
        RoomOrchestrator.RoomStaticIdentity staticIdentity = typedStaticIdentity(command, identity);
        return new RoomOrchestrator.DissolveVoteCommand(identity, staticIdentity,
                required(command, "sessionId"), positive(command, "voterAccountId"),
                enumValue(command, "permission", RoomOrchestrator.DissolvePermission.class),
                enumValue(command, "roomState", RoomState.class), required(command, "voteId"),
                requiredBoolean(command, "agree"), nonNegative(command, "stateVersion"),
                required(command, "requestId"), required(command, "idempotencyKey"),
                metadata(command));
    }

    private RoomOrchestrator.HandoffCommand toHandoffCommand(
            Map<String, Object> command, GameRoomHandle room) {
        RoomOrchestrator.RoomIdentity identity = typedIdentity(command, room);
        long expectedStateVersion = nonNegative(command, "expectedStateVersion");
        long eventSequence = nonNegative(command, "eventSequence");
        String eventId = required(command, "eventId");
        if (!eventId.equals(required(command, "requestId"))) {
            throw new SecurityException("ROOM_ORCHESTRATOR_HANDOFF_EVENT_ID_CONFLICT");
        }
        if (expectedStateVersion != identity.stateVersion()) {
            throw new SecurityException("ROOM_ORCHESTRATOR_HANDOFF_STATE_VERSION_CONFLICT");
        }
        return new RoomOrchestrator.HandoffCommand(identity, positive(command, "ownerId"),
                required(command, "fromSessionId"), required(command, "toSessionId"),
                positive(command, "fromConnectionGeneration"),
                positive(command, "toConnectionGeneration"), required(command, "requestId"),
                expectedStateVersion, metadata(command), eventSequence, eventId);
    }

    private static void requireTopLevelString(Map<String, Object> outer, String key) {
        Object value = outer.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_" + key.toUpperCase()
                    + "_TOP_LEVEL_REQUIRED");
        }
    }

    private static void requireTopLevelBoolean(Map<String, Object> outer, String key) {
        if (!(outer.get(key) instanceof Boolean)) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_" + key.toUpperCase()
                    + "_TOP_LEVEL_REQUIRED");
        }
    }

    private RoomOrchestrator.RoomIdentity typedIdentity(Map<String, Object> command,
                                                        GameRoomHandle room) {
        long roomId = positive(command, "roomId");
        int gameId;
        try {
            gameId = Math.toIntExact(positive(command, "gameId"));
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_TYPED_GAME_ID_INVALID", failure);
        }
        String gameCode = required(command, "gameCode");
        String playVersion = required(command, "playVersion");
        Route route = route(roomId);
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireCanonicalProjection(roomId, route.gameId(), route.playVersion());
        GameProvider provider = games.require(route.gameId(), route.playVersion());
        if (room.roomId() != roomId || !"ACTIVE".equals(route.state())
                || gameId != route.gameId() || gameId != canonical.gameId()
                || !gameCode.equals(provider.descriptor().code())
                || !playVersion.equals(route.playVersion())
                || !playVersion.equals(canonical.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_TYPED_STATIC_IDENTITY_CONFLICT");
        }
        RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(roomId,
                required(command, "roomInstanceId"), gameId, gameCode, playVersion,
                positive(command, "lifecycleEpoch"), positive(command, "fencingToken"),
                nonNegative(command, "stateVersion"));
        requireTypedRuntimeIdentity(identity, room);
        return identity;
    }

    private static RoomOrchestrator.RoomStaticIdentity typedStaticIdentity(
            Map<String, Object> command, RoomOrchestrator.RoomIdentity identity) {
        RoomOrchestrator.RoomStaticIdentity staticIdentity = new RoomOrchestrator.RoomStaticIdentity(
                positive(command, "roomId"), Math.toIntExact(positive(command, "gameId")),
                required(command, "gameCode"), required(command, "playVersion"));
        if (staticIdentity.roomId() != identity.roomId()
                || staticIdentity.gameId() != identity.gameId()
                || !staticIdentity.gameCode().equals(identity.gameCode())
                || !staticIdentity.playVersion().equals(identity.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_TYPED_STATIC_IDENTITY_CONFLICT");
        }
        return staticIdentity;
    }

    private void requireTypedRuntimeIdentity(RoomOrchestrator.RoomIdentity expected,
                                             GameRoomHandle room) {
        RoomRuntimeIdentity runtime = room.runtimeIdentity();
        if (runtime == null || !runtime.equals(new RoomRuntimeIdentity(expected.roomInstanceId(),
                expected.lifecycleEpoch(), expected.fencingToken(), expected.stateVersion()))) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_TYPED_RUNTIME_IDENTITY_CONFLICT");
        }
        if (room.requireAuthoritativeSession().stateVersion() != expected.stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_TYPED_STATE_VERSION_CONFLICT");
        }
    }

    private static RoomSnapshotMetadataSource metadata(Map<String, Object> command) {
        return new RoomSnapshotMetadataSource(positive(command, "releaseId"),
                required(command, "ruleContentHash"), required(command, "componentContentHash"),
                required(command, "cardCodecVersion"), required(command, "eventInterpreterVersion"),
                required(command, "protocolVersion"), required(command, "snapshotSchemaVersion"),
                positive(command, "indexGeneration"), required(command, "regionCode"),
                required(command, "sourceHash"));
    }

    private static Set<Long> memberIds(Map<String, Object> command, String key) {
        Object raw = command.get(key);
        if (!(raw instanceof Collection<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException("ROOM_DISSOLVE_ELIGIBLE_MEMBERS_REQUIRED");
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (!(value instanceof Number number) || number.longValue() <= 0
                    || !result.add(number.longValue())) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_ELIGIBLE_MEMBERS_INVALID");
            }
        }
        return Set.copyOf(result);
    }

    private static Instant instant(Map<String, Object> command, String key) {
        try {
            return Instant.parse(required(command, key));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("ROOM_DISSOLVE_" + key.toUpperCase() + "_INVALID", failure);
        }
    }

    private static boolean requiredBoolean(Map<String, Object> command, String key) {
        Object raw = command.get(key);
        if (!(raw instanceof Boolean value)) {
            throw new IllegalArgumentException("ROOM_DISSOLVE_" + key.toUpperCase() + "_REQUIRED");
        }
        return value;
    }

    private static <E extends Enum<E>> E enumValue(Map<String, Object> command, String key,
                                                    Class<E> type) {
        try {
            return Enum.valueOf(type, required(command, key));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("ROOM_DISSOLVE_" + key.toUpperCase() + "_INVALID", failure);
        }
    }

    private static Map<String, Object> dissolveResponse(RoomOrchestrator.DissolveResult result,
                                                        Boolean agree) {
        Map<String, Object> value = new LinkedHashMap<>();
        RoomOrchestrator.RoomIdentity identity = result.identity();
        value.put("accepted", true);
        value.put("roomId", identity.roomId());
        value.put("gameId", identity.gameId());
        value.put("gameCode", identity.gameCode());
        value.put("playVersion", identity.playVersion());
        value.put("roomInstanceId", identity.roomInstanceId());
        value.put("lifecycleEpoch", identity.lifecycleEpoch());
        value.put("fencingToken", identity.fencingToken());
        value.put("stateVersion", identity.stateVersion());
        value.put("voteId", result.voteId());
        value.put("applicantAccountId", result.applicantAccountId());
        value.put("eligibleMemberIds", result.eligibleMemberIds());
        value.put("expiresAt", result.deadline().toEpochMilli());
        value.put("approveOnTimeout", result.approveOnTimeout());
        value.put("votes", result.votes());
        value.put("decision", result.decision().name());
        value.put("terminalTombstone", result.terminalTombstone());
        if (agree != null) value.put("agree", agree);
        if (result.commandId() != null) value.put("commandId", result.commandId());
        if (result.idempotencyKey() != null) value.put("idempotencyKey", result.idempotencyKey());
        return Map.copyOf(value);
    }

    private static Map<String, Object> handoffResponse(RoomOrchestrator.HandoffResult result,
                                                       RoomOrchestrator.HandoffCommand command) {
        RoomOrchestrator.RoomIdentity identity = result.identity();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("accepted", true);
        value.put("roomId", identity.roomId());
        value.put("gameId", identity.gameId());
        value.put("gameCode", identity.gameCode());
        value.put("playVersion", identity.playVersion());
        value.put("roomInstanceId", identity.roomInstanceId());
        value.put("lifecycleEpoch", Long.toString(identity.lifecycleEpoch()));
        value.put("sessionId", result.sessionId());
        value.put("stateVersion", identity.stateVersion());
        value.put("eventSequence", Long.toString(command.eventSequence()));
        value.put("eventId", command.eventId());
        value.put("connectionGeneration", Long.toString(result.connectionGeneration()));
        value.put("requestId", command.requestId());
        return Map.copyOf(value);
    }

    /** v2.64 durable authority seam; Gateway transport remains a later batch. */
    @Override
    public RoomOrchestrator.DissolveResult dissolveApply(RoomOrchestrator.DissolveApplyCommand command) {
        stateStore.carryPort().requireAvailable(command.expectedIdentity().roomId());
        return dissolveAuthority.apply(command);
    }

    @Override
    public RoomOrchestrator.DissolveResult dissolveVote(RoomOrchestrator.DissolveVoteCommand command) {
        stateStore.carryPort().requireAvailable(command.expectedIdentity().roomId());
        return dissolveAuthority.vote(command);
    }

    @Override
    public RoomOrchestrator.DissolveResult dissolveRecover(RoomOrchestrator.DissolveRecoverCommand command) {
        stateStore.carryPort().requireAvailable(command.expectedIdentity().roomId());
        return dissolveAuthority.recover(command);
    }

    @Override
    public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
        Objects.requireNonNull(command, "command");
        Map<String, Object> response = createGateway(createCommandMap(command));
        return createResult(command, response);
    }

    @Override
    public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
        Objects.requireNonNull(command, "command");
        Map<String, Object> response = recoverGateway(recoverCommandMap(command));
        return recoverResult(command, response);
    }

    /**
     * Executes one gameplay command through the sole production authority.  The method is
     * intentionally bounded to execute: reconnect, handoff and destroy remain separate
     * lifecycle operations.  Every value used for fencing is read from the route, canonical
     * room-lock projection, runtime identity row, or the live authoritative session.
     */
    @Override
    public RoomOrchestrator.ExecuteResult execute(RoomOrchestrator.ExecuteCommand command) {
        Objects.requireNonNull(command, "command");
        long roomId = command.expectedIdentity().roomId();
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                    || !clock.instant().isBefore(route.leaseExpiresAt())) {
                throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_ROOM_STATE_REJECTED");
            }
            GameRoomHandle room = rooms.require(roomId);
            RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(roomId);
            requireExecuteIdentity(command.expectedIdentity(), route, stored);
            if (room.runtimeIdentity() == null || !room.runtimeIdentity().equals(stored.identity())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RUNTIME_IDENTITY_CONFLICT");
            }
            var session = room.requireAuthoritativeSession();
            long expectedStateVersion = command.expectedIdentity().stateVersion();
            if (session.stateVersion() != expectedStateVersion) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_STATE_VERSION_CONFLICT");
            }

            IdempotencyKey key = new IdempotencyKey(command.request().authenticatedUserId(),
                    command.request().msgId(), roomId, command.request().roundNo(),
                    command.request().requestId());
            GameCommandResult committed = stateStore.findCommitted(room, command.request()).orElse(null);
            if (committed != null) {
                return new RoomOrchestrator.ExecuteResult(
                        authorityIdentity(route, stored.identity()), committed);
            }
            requireNoRequestScopeConflict(key);
            if (isTerminalAuthoritySession(session, session.authoritativeState())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_ROOM_STATE_REJECTED");
            }
            requireExecutePermission(session.authoritativeState(), command.request());

            GameProvider provider = games.require(route.gameId(), route.playVersion());
            GameCommandHandler commandHandler = provider.commandHandler().orElseThrow(() ->
                    new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_COMMAND_HANDLER_REQUIRED"));

            if (!executeIdempotency.acquire(key, executeIdempotencyRetention)) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_DUPLICATE_COMMAND");
            }
            if(session instanceof com.aoo.bcg.gamespi.CarryBillingEffectSource carry)carry.proposeCarry(command.request());
            JdbcCarryParticipationPort.Checkpoint carryCheckpoint=beginCarry(room);
            boolean mutationStarted = false;
            RoomRuntimeIdentity pendingIdentity = null;
            GameRoomHandle pendingRoom = null;
            try {
                mutationStarted = true;
                GameCommandResult handled = commandHandler.handle(room, command.request());
                if (handled == null || !command.request().requestId().equals(handled.requestId())) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_RESULT_CONFLICT");
                }
                long nextStateVersion = session.stateVersion();
                if (nextStateVersion != Math.addExact(expectedStateVersion, 1L)) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_STATE_VERSION_CONFLICT");
                }
                GameCommandResult result = handled.serverTimeEpochMillis() == 0
                        ? handled.withTiming(clock.millis(), session.operationDeadline())
                        : handled;
                result = result.withAuthorityMetadata(nextStateVersion, command.request().sequence());
                RoomRuntimeIdentity advanced = new RoomRuntimeIdentity(
                        stored.identity().roomInstanceId(), stored.identity().lifecycleEpoch(),
                        stored.identity().fencingToken(), nextStateVersion);
                GameRoomHandle committedRoom = copyWithIdentity(room, advanced);
                pendingIdentity = advanced;
                pendingRoom = committedRoom;
                GameCommandResult durable = stateStore.commitResult(
                        committedRoom, command.request(), result, stored.identity());
                rooms.updateRuntime(committedRoom);
                stateStore.carryPort().finish(carryCheckpoint,true);
                return new RoomOrchestrator.ExecuteResult(
                        authorityIdentity(route, advanced), durable);
            } catch (RuntimeException | Error failure) {
                // A lost commit acknowledgement is resolved only by the exact durable receipt,
                // identity and snapshot written by that transaction, never by a guessed revision.
                if (pendingIdentity != null && failure instanceof RuntimeException) {
                    try {
                        GameCommandResult resolvedResult = stateStore.findCommitted(room, command.request())
                                .orElse(null);
                        if (resolvedResult != null) {
                            RoomRuntimeIdentity durableIdentity = roomIdentityStore.require(roomId).identity();
                            var snapshot = stateStore.latest(roomId).orElseThrow(() ->
                                    new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_SNAPSHOT_REQUIRED"));
                            if (!durableIdentity.equals(pendingIdentity)
                                    || snapshot.stateVersion() != pendingIdentity.stateVersion()
                                    || snapshot.lastEventSequence() != pendingIdentity.stateVersion()) {
                                throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_COMMIT_CONFLICT");
                            }
                            if(carryCheckpoint!=null)restoreCarryDurable(room,carryCheckpoint);
                            else if (!pendingIdentity.equals(rooms.require(roomId).runtimeIdentity())) rooms.updateRuntime(pendingRoom);
                            stateStore.carryPort().finish(carryCheckpoint,true);
                            return new RoomOrchestrator.ExecuteResult(
                                    authorityIdentity(route, durableIdentity), resolvedResult);
                        }
                    } catch (RuntimeException resolutionFailure) {
                        failure.addSuppressed(resolutionFailure);
                    }
                }
                if(carryCheckpoint!=null){boolean restored=false;try{restoreCarryDurable(room,carryCheckpoint);restored=true;}catch(RuntimeException restoreFailure){failure.addSuppressed(restoreFailure);}stateStore.carryPort().finish(carryCheckpoint,restored);}
                if (mutationStarted) executeIdempotency.markUnknown(key);
                else executeIdempotency.release(key);
                throw failure;
            }
        }
    }

    @Override
    public RoomOrchestrator.ReadyResult ready(RoomOrchestrator.ReadyCommand command) {
        Objects.requireNonNull(command, "command");
        long roomId = command.expectedIdentity().roomId();
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                    || !clock.instant().isBefore(route.leaseExpiresAt())) {
                throw new SecurityException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_REJECTED");
            }
            requireCanonicalRoomLock(route);
            GameProvider provider = games.require(route.gameId(), route.playVersion());
            RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(roomId);
            RoomOrchestrator.RoomIdentity current = authorityIdentity(route, stored.identity());
            if (!stored.active() || !current.equals(command.expectedIdentity())
                    || command.staticIdentity().roomId() != route.roomId()
                    || command.staticIdentity().gameId() != route.gameId()
                    || !command.staticIdentity().gameCode().equals(provider.descriptor().code())
                    || !command.staticIdentity().playVersion().equals(route.playVersion())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_IDENTITY_CONFLICT");
            }
            if (command.permission() != RoomOrchestrator.ReadyPermission.SEATED_MEMBER) {
                throw new SecurityException("ROOM_ORCHESTRATOR_READY_PERMISSION_DENIED");
            }
            if (command.roomState() != RoomState.WAITING) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_REJECTED");
            }
            GameRoomHandle room = rooms.require(roomId);
            if (room.runtimeIdentity() == null || !room.runtimeIdentity().equals(stored.identity())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RUNTIME_IDENTITY_CONFLICT");
            }
            var session = room.requireAuthoritativeSession();
            if (session.stateVersion() != command.expectedIdentity().stateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_STATE_VERSION_CONFLICT");
            }
            requireReadyPermission(session.authoritativeState(), command.accountId(), command.seatNo());
            GameCommandRequest request = new GameCommandRequest("common.room.ready_req",
                    command.commandId(), Math.addExact(session.stateVersion(), 1L), roomId, 0,
                    route.playVersion(), Long.toString(command.accountId()), command.seatNo(),
                    command.body().asMap());
            IdempotencyKey key = new IdempotencyKey(request.authenticatedUserId(), request.msgId(),
                    roomId, request.roundNo(), request.requestId());
            GameCommandResult committed = stateStore.findCommitted(room, request).orElse(null);
            if (committed != null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_ALREADY_COMMITTED");
            }
            if (!executeIdempotency.acquire(key, executeIdempotencyRetention)) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_DUPLICATE_COMMAND");
            }
            JdbcCarryParticipationPort.Checkpoint carryCheckpoint = null;
            boolean mutationStarted = false;
            RoomRuntimeIdentity pendingIdentity = null;
            GameRoomHandle pendingRoom = null;
            try {
                carryCheckpoint = beginCarry(room);
                mutationStarted = true;
                GameCommandResult handled = session.execute(request);
                if (handled == null || !request.requestId().equals(handled.requestId())) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RESULT_CONFLICT");
                }
                long nextStateVersion = session.stateVersion();
                if (nextStateVersion != Math.addExact(command.expectedIdentity().stateVersion(), 1L)) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_STATE_VERSION_CONFLICT");
                }
                GameCommandResult result = handled.serverTimeEpochMillis() == 0
                        ? handled.withTiming(clock.millis(), session.operationDeadline()) : handled;
                result = result.withAuthorityMetadata(nextStateVersion, request.sequence());
                RoomRuntimeIdentity advanced = new RoomRuntimeIdentity(
                        stored.identity().roomInstanceId(), stored.identity().lifecycleEpoch(),
                        stored.identity().fencingToken(), nextStateVersion);
                GameRoomHandle committedRoom = copyWithIdentity(room, advanced);
                pendingIdentity = advanced;
                pendingRoom = committedRoom;
                stateStore.commitResult(committedRoom, request, result, stored.identity());
                rooms.updateRuntime(committedRoom);
                stateStore.carryPort().finish(carryCheckpoint,true);
                return new RoomOrchestrator.ReadyResult(
                        current, command.accountId(), command.seatNo(),
                        RoomOrchestrator.ReadyStatus.ACCEPTED, command.commandId(), nextStateVersion);
            } catch (RuntimeException | Error failure) {
                // A commit acknowledgement may be lost after the exact snapshot, identity and
                // receipt were committed. Resolve only from those durable facts; never execute
                // READY a second time or guess that the transaction succeeded.
                if (pendingIdentity != null && failure instanceof RuntimeException) {
                    try {
                        GameCommandResult resolvedResult = stateStore.findCommitted(room, request)
                                .orElse(null);
                        if (resolvedResult != null) {
                            RoomRuntimeIdentity durableIdentity = roomIdentityStore.require(roomId).identity();
                            var snapshot = stateStore.latest(roomId).orElseThrow(() ->
                                    new IllegalStateException("ROOM_ORCHESTRATOR_READY_SNAPSHOT_REQUIRED"));
                            if (!durableIdentity.equals(pendingIdentity)
                                    || snapshot.stateVersion() != pendingIdentity.stateVersion()
                                    || snapshot.lastEventSequence() != pendingIdentity.stateVersion()) {
                                throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_COMMIT_CONFLICT");
                            }
                            if (carryCheckpoint != null) restoreCarryDurable(room, carryCheckpoint);
                            else if (!pendingIdentity.equals(rooms.require(roomId).runtimeIdentity())) {
                                rooms.updateRuntime(pendingRoom);
                            }
                            stateStore.carryPort().finish(carryCheckpoint, true);
                            return new RoomOrchestrator.ReadyResult(
                                    current, command.accountId(), command.seatNo(),
                                    RoomOrchestrator.ReadyStatus.ACCEPTED, command.commandId(),
                                    pendingIdentity.stateVersion());
                        }
                    } catch (RuntimeException resolutionFailure) {
                        failure.addSuppressed(resolutionFailure);
                    }
                }
                if(carryCheckpoint!=null){boolean restored=false;try{restoreCarryDurable(room,carryCheckpoint);restored=true;}catch(RuntimeException restoreFailure){failure.addSuppressed(restoreFailure);}stateStore.carryPort().finish(carryCheckpoint,restored);}
                if (mutationStarted) {
                    // The authoritative session has already mutated; do not report a successful
                    // result if durable snapshot/idempotency persistence failed.
                    failure.addSuppressed(new IllegalStateException(
                            "ROOM_ORCHESTRATOR_READY_MUTATION_REQUIRES_RECOVERY"));
                    executeIdempotency.markUnknown(key);
                } else {
                    executeIdempotency.release(key);
                }
                throw failure;
            }
        }
    }

    /**
     * Reconnect is a read-only, fenced observation of the same authority used by execute.
     * It never restores from a client payload or the legacy RoomMgr; both the room-lock
     * projection and the durable runtime identity are checked before a snapshot is returned.
     */
    @Override
    public RoomOrchestrator.ReconnectResult reconnect(RoomOrchestrator.ReconnectCommand command) {
        Objects.requireNonNull(command, "command");
        long roomId = command.expectedIdentity().roomId();
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                    || !clock.instant().isBefore(route.leaseExpiresAt())) {
                throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_ROOM_STATE_REJECTED");
            }
            RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                    requireCanonicalRoomLock(route);
            GameProvider provider = games.require(route.gameId(), route.playVersion());
            RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(roomId);
            RoomOrchestrator.RoomIdentity current = authorityIdentity(route, stored.identity());
            requireReconnectIdentity(command, current, canonical.source(), provider);
            if (!stored.active()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_ROOM_DESTROYED");
            }
            GameRoomHandle room = rooms.require(roomId);
            if (room.runtimeIdentity() == null || !room.runtimeIdentity().equals(stored.identity())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_RUNTIME_IDENTITY_CONFLICT");
            }
            var session = room.requireAuthoritativeSession();
            if (isTerminalAuthoritySession(session, session.authoritativeState())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_ROOM_STATE_REJECTED");
            }
            requireReconnectPermission(session.authoritativeState(), command.accountId(), command.seatNo());
            if (session.stateVersion() < command.lastKnownStateVersion()
                    || session.stateVersion() < command.expectedIdentity().stateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_STATE_VERSION_STALE");
            }
            com.aoo.bcg.gamespi.StatePayload snapshot = statePayload(room);
            Object rawSnapshotVersion = snapshot.asMap().get("stateVersion");
            if (!(rawSnapshotVersion instanceof Number number)
                    || number.longValue() < command.lastKnownStateVersion()
                    || number.longValue() != session.stateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_SNAPSHOT_VERSION_CONFLICT");
            }
            RoomRuntimeIdentityStore.Entry after = roomIdentityStore.require(roomId);
            Route afterRoute = route(roomId);
            if (!after.active() || !after.identity().equals(stored.identity())
                    || !"ACTIVE".equals(afterRoute.state())
                    || afterRoute.fencingToken() != route.fencingToken()
                    || !nodeId.equals(afterRoute.nodeId())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_CAS_CONFLICT");
            }
            ConcurrentHashMap<String, Boolean> receipts = reconnectReceipts.computeIfAbsent(
                    roomId, ignored -> new ConcurrentHashMap<>());
            if (receipts.putIfAbsent(command.requestId(), Boolean.TRUE) != null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_DUPLICATE");
            }
            return new RoomOrchestrator.ReconnectResult(
                    authorityIdentity(afterRoute, after.identity()), snapshot);
        }
    }

    /**
     * Performs a room-owner/session handoff as one authority-local CAS.  No
     * account session replacement or Gateway connection registry is involved;
     * a cold node has no lease and therefore rejects the request.
     */
    @Override
    public RoomOrchestrator.HandoffResult handoff(RoomOrchestrator.HandoffCommand command) {
        Objects.requireNonNull(command, "command");
        long roomId = command.expectedIdentity().roomId();
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                    || !clock.instant().isBefore(route.leaseExpiresAt())) {
                throw new SecurityException("ROOM_ORCHESTRATOR_HANDOFF_ROOM_STATE_REJECTED");
            }
            RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                    requireCanonicalRoomLock(route);
            GameProvider provider = games.require(route.gameId(), route.playVersion());
            RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(roomId);
            RoomOrchestrator.RoomIdentity current = authorityIdentity(route, stored.identity());
            requireHandoffIdentity(command, current, canonical.source(), provider);
            if (!stored.active()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_ROOM_DESTROYED");
            }
            GameRoomHandle room = rooms.require(roomId);
            if (room.runtimeIdentity() == null || !room.runtimeIdentity().equals(stored.identity())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_RUNTIME_IDENTITY_CONFLICT");
            }
            var session = room.requireAuthoritativeSession();
            if (isTerminalAuthoritySession(session, session.authoritativeState())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_ROOM_STATE_REJECTED");
            }
            if (session.stateVersion() != command.expectedStateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_STATE_VERSION_CONFLICT");
            }
            requireHandoffOwner(session.authoritativeState(), command.ownerId());

            ConcurrentHashMap<String, HandoffReceipt> receipts = handoffReceipts.computeIfAbsent(
                    roomId, ignored -> new ConcurrentHashMap<>());
            HandoffReceipt prior = receipts.get(command.requestId());
            if (prior != null) {
                if (!prior.command().equals(command)) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_COMMAND_CONFLICT");
                }
                return prior.result();
            }
            HandoffLease lease = handoffLeases.get(roomId);
            if (lease == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_UNREGISTERED");
            }
            if (!lease.identity().equals(current)) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_IDENTITY_CONFLICT");
            }
            if (lease.ownerId() != command.ownerId()
                    || !lease.sessionId().equals(command.fromSessionId())
                    || lease.connectionGeneration() != command.fromConnectionGeneration()) {
                throw new SecurityException("ROOM_ORCHESTRATOR_HANDOFF_CAS_CONFLICT");
            }
            RoomOrchestrator.HandoffResult result = new RoomOrchestrator.HandoffResult(
                    current, command.toSessionId(), command.toConnectionGeneration());
            HandoffReceipt inserted = new HandoffReceipt(command, result);
            if (receipts.putIfAbsent(command.requestId(), inserted) != null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_CAS_CONFLICT");
            }
            handoffLeases.put(roomId, new HandoffLease(current, command.ownerId(),
                    command.toSessionId(), command.toConnectionGeneration()));
            return result;
        }
    }

    /**
     * Registers the transport-owned handoff lease. Registration is monotonic:
     * an exact retry is idempotent, a strictly newer connection generation may
     * replace the prior lease, and every old or conflicting tuple is rejected.
     */
    void registerHandoffOwner(RoomOrchestrator.RoomIdentity expectedIdentity, long ownerId,
                              String sessionId, long connectionGeneration) {
        Objects.requireNonNull(expectedIdentity, "expectedIdentity");
        if (ownerId <= 0 || sessionId == null || sessionId.isBlank() || connectionGeneration <= 0) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_INVALID");
        }
        synchronized (lock(expectedIdentity.roomId())) {
            Route route = route(expectedIdentity.roomId());
            RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(expectedIdentity.roomId());
            RoomOrchestrator.RoomIdentity current = authorityIdentity(route, stored.identity());
            if (!stored.active() || !current.equals(expectedIdentity)
                    || !"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                    || !clock.instant().isBefore(route.leaseExpiresAt())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT");
            }
            GameRoomHandle room = rooms.require(expectedIdentity.roomId());
            requireHandoffOwner(room.requireAuthoritativeSession().authoritativeState(), ownerId);
            HandoffLease replacement = new HandoffLease(current, ownerId, sessionId, connectionGeneration);
            HandoffLease prior = handoffLeases.get(expectedIdentity.roomId());
            if (!shouldReplaceHandoffLease(prior, replacement)) return;
            handoffLeases.put(expectedIdentity.roomId(), replacement);
        }
    }

    static boolean shouldReplaceHandoffLease(HandoffLease prior, HandoffLease replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (prior == null) return true;
        if (!prior.identity().equals(replacement.identity())
                || prior.ownerId() != replacement.ownerId()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT");
        }
        if (prior.sessionId().equals(replacement.sessionId())
                && prior.connectionGeneration() == replacement.connectionGeneration()) {
            return false;
        }
        if (replacement.connectionGeneration() <= prior.connectionGeneration()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT");
        }
        return true;
    }

    /** Production Gateway transport bridge; only the typed adapter may serve reconnect. */
    RoomOrchestrator.ReconnectResult reconnectThroughRoomOrchestrator(
            RoomOrchestrator.ReconnectCommand command) {
        return roomOrchestrator.reconnect(command);
    }

    /** Typed handoff bridge reserved for the later transport contract batch. */
    RoomOrchestrator.HandoffResult handoffThroughRoomOrchestrator(
            RoomOrchestrator.HandoffCommand command) {
        return roomOrchestrator.handoff(command);
    }

    /**
     * Bounded production ingress for gameplay execute.  Callers must use this typed
     * authority bridge; the legacy map-based RoomAuthority has no execute fallback.
     */
    RoomOrchestrator.ExecuteResult executeThroughRoomOrchestrator(
            RoomOrchestrator.ExecuteCommand command) {
        return roomOrchestrator.execute(command);
    }

    @Override
    public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
        Objects.requireNonNull(command, "command");
        long roomId = command.expectedIdentity().roomId();
        synchronized (lock(roomId)) {
            ConcurrentHashMap<String, RoomOrchestrator.LeaveResult> receipts =
                    leaveReceipts.computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>());
            RoomOrchestrator.LeaveResult prior = receipts.get(command.commandId());
            if (prior != null) return requireSameLeaveCommand(command, prior);
            Route route = route(roomId);
            RoomRuntimeIdentityStore.Entry stored = requireLeaveIdentity(
                    command.expectedIdentity(), route);
            if (!stored.active() || !"ACTIVE".equals(route.state())) {
                return rememberLeave(receipts, command,
                        alreadyLeft(command, stored.identity(), -1));
            }
            if (!nodeId.equals(route.nodeId())
                    || !clock.instant().isBefore(route.leaseExpiresAt())) {
                throw new SecurityException("room membership lifecycle owned by another node");
            }
            GameRoomHandle room;
            try {
                room = rooms.require(roomId);
            } catch (IllegalArgumentException missingRoom) {
                return rememberLeave(receipts, command,
                        alreadyLeft(command, stored.identity(), -1));
            }
            if (room.runtimeIdentity() == null || !room.runtimeIdentity().equals(stored.identity())) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_CONFLICT");
            }
            var session = room.requireAuthoritativeSession();
            if (session.stateVersion() < command.expectedIdentity().stateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_STALE");
            }
            if (session.stateVersion() != command.expectedIdentity().stateVersion()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_CONFLICT");
            }
            Map<String, Object> authorityState = session.authoritativeState();
            if (isTerminalAuthoritySession(session, authorityState)) {
                return rememberLeave(receipts, command,
                        alreadyLeft(command, stored.identity(), -1));
            }
            Object rawPlayers = authorityState.get("players");
            if (!(rawPlayers instanceof Map<?, ?> players)) {
                throw new RoomAuthorityBusinessError(409, "ROOM_LEAVE_REJECTED",
                        "room has no seated players");
            }
            int seatNo = -1;
            for (Map.Entry<?, ?> entry : players.entrySet()) {
                if (Long.parseLong(String.valueOf(entry.getValue())) == command.accountId()) {
                    seatNo = Integer.parseInt(String.valueOf(entry.getKey()));
                    break;
                }
            }
            if (seatNo < 0) return rememberLeave(receipts, command,
                    alreadyLeft(command, stored.identity(), -1));
            // Membership leave is completed by the Hall membership authority after this
            // fenced acknowledgement. It must not execute a gameplay command, advance the
            // gameplay stateVersion, persist a gameplay snapshot, or publish gameplay state.
            return rememberLeave(receipts, command, new RoomOrchestrator.LeaveResult(
                    authorityIdentity(route, stored.identity()), command.accountId(),
                    RoomOrchestrator.LeaveStatus.LEFT, seatNo, command.commandId()));
        }
    }

    private void requireExecuteIdentity(RoomOrchestrator.RoomIdentity expected,
                                        Route route,
                                        RoomRuntimeIdentityStore.Entry stored) {
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireCanonicalProjection(route.roomId(), route.gameId(), route.playVersion());
        GameProvider provider = games.require(route.gameId(), route.playVersion());
        boolean staticMatch = expected.roomId() == route.roomId()
                && expected.gameId() == route.gameId()
                && expected.gameId() == canonical.gameId()
                && expected.gameCode().equals(provider.descriptor().code())
                && expected.playVersion().equals(route.playVersion())
                && expected.playVersion().equals(canonical.playVersion());
        if (!staticMatch) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_STATIC_IDENTITY_CONFLICT");
        }
        RoomRuntimeIdentity requested = new RoomRuntimeIdentity(expected.roomInstanceId(),
                expected.lifecycleEpoch(), expected.fencingToken(), expected.stateVersion());
        if (!stored.active()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_ROOM_DESTROYED");
        }
        if (!stored.identity().equals(requested) || stored.identity().fencingToken() != route.fencingToken()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_IDENTITY_CONFLICT");
        }
    }

    private static void requireReconnectIdentity(
            RoomOrchestrator.ReconnectCommand command,
            RoomOrchestrator.RoomIdentity current,
            RoomSnapshotMetadataSource canonical,
            GameProvider provider) {
        RoomOrchestrator.RoomIdentity expected = command.expectedIdentity();
        if (expected.roomId() != current.roomId()
                || expected.gameId() != current.gameId()
                || !expected.gameCode().equals(current.gameCode())
                || !expected.playVersion().equals(current.playVersion())
                || !expected.roomInstanceId().equals(current.roomInstanceId())
                || expected.lifecycleEpoch() != current.lifecycleEpoch()
                || expected.fencingToken() != current.fencingToken()
                || expected.stateVersion() > current.stateVersion()
                || expected.gameId() != provider.descriptor().gameId()
                || !expected.gameCode().equals(provider.descriptor().code())
                || !expected.playVersion().equals(provider.descriptor().version())
                || !command.metadata().equals(canonical)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_IDENTITY_CONFLICT");
        }
        if (command.accountId() <= 0 || command.seatNo() < 0
                || command.sessionId().isBlank() || command.reconnectToken().isBlank()) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_PERMISSION_DENIED");
        }
    }

    private static void requireReconnectPermission(Map<String, Object> authorityState,
                                                   long accountId, int seatNo) {
        if (accountId <= 0 || seatNo < 0 || !seatBelongsToPlayer(authorityState, seatNo, accountId)) {
            throw new SecurityException("ROOM_ORCHESTRATOR_RECONNECT_PERMISSION_DENIED");
        }
    }

    private static void requireHandoffIdentity(RoomOrchestrator.HandoffCommand command,
                                               RoomOrchestrator.RoomIdentity current,
                                               RoomSnapshotMetadataSource canonical,
                                               GameProvider provider) {
        RoomOrchestrator.RoomIdentity expected = command.expectedIdentity();
        if (!expected.equals(current)
                || expected.gameId() != provider.descriptor().gameId()
                || !expected.gameCode().equals(provider.descriptor().code())
                || !expected.playVersion().equals(provider.descriptor().version())
                || !command.metadata().equals(canonical)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_IDENTITY_CONFLICT");
        }
        if (command.expectedStateVersion() != current.stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_STATE_VERSION_CONFLICT");
        }
    }

    private static void requireHandoffOwner(Map<String, Object> authorityState, long ownerId) {
        Object rawOwner = authorityState.get("ownerId");
        if (!(rawOwner instanceof Number owner)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REQUIRED");
        }
        if (owner.longValue() != ownerId) {
            throw new SecurityException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_CONFLICT");
        }
    }

    private static void requireExecutePermission(Map<String, Object> authorityState,
                                                 GameCommandRequest request) {
        if (request.seatId() < 0) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_PERMISSION_DENIED");
        }
        final long playerId;
        try {
            playerId = Long.parseLong(request.authenticatedUserId());
        } catch (NumberFormatException failure) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_PERMISSION_DENIED", failure);
        }
        if (playerId <= 0 || !seatBelongsToPlayer(authorityState, request.seatId(), playerId)) {
            throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_PERMISSION_DENIED");
        }
    }

    private static boolean seatBelongsToPlayer(Map<String, Object> state, int seatId, long playerId) {
        for (String key : List.of("players", "seats")) {
            Object raw = state.get(key);
            if (!(raw instanceof Map<?, ?> seats)) continue;
            Object occupant = seats.get(seatId);
            if (occupant == null) occupant = seats.get(String.valueOf(seatId));
            if (occupant instanceof Map<?, ?> view) occupant = view.get("playerId");
            if (occupant instanceof Number number && number.longValue() == playerId) return true;
            if (occupant != null && String.valueOf(playerId).equals(String.valueOf(occupant))) return true;
        }
        return false;
    }

    private static RoomOrchestrator.LeaveResult rememberLeave(
            ConcurrentHashMap<String, RoomOrchestrator.LeaveResult> receipts,
            RoomOrchestrator.LeaveCommand command, RoomOrchestrator.LeaveResult result) {
        RoomOrchestrator.LeaveResult prior = receipts.putIfAbsent(command.commandId(), result);
        return prior == null ? result : requireSameLeaveCommand(command, prior);
    }

    static RoomOrchestrator.LeaveResult requireSameLeaveCommand(
            RoomOrchestrator.LeaveCommand command, RoomOrchestrator.LeaveResult prior) {
        if (prior.accountId() != command.accountId()
                || !prior.identity().equals(command.expectedIdentity())
                || !prior.commandId().equals(command.commandId())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_COMMAND_CONFLICT");
        }
        return prior;
    }

    private RoomRuntimeIdentityStore.Entry requireLeaveIdentity(
            RoomOrchestrator.RoomIdentity expected, Route route) {
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireCanonicalProjection(route.roomId(), route.gameId(), route.playVersion());
        GameProvider provider = games.require(route.gameId(), route.playVersion());
        if (expected.roomId() != route.roomId() || expected.gameId() != route.gameId()
                || expected.gameId() != canonical.gameId()
                || !expected.gameCode().equals(provider.descriptor().code())
                || !expected.playVersion().equals(route.playVersion())
                || !expected.playVersion().equals(canonical.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT");
        }
        RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(route.roomId());
        if (!stored.identity().equals(new RoomRuntimeIdentity(expected.roomInstanceId(),
                expected.lifecycleEpoch(), expected.fencingToken(), expected.stateVersion()))) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_CONFLICT");
        }
        return stored;
    }

    private static RoomOrchestrator.LeaveResult alreadyLeft(
            RoomOrchestrator.LeaveCommand command, RoomRuntimeIdentity identity, int seatNo) {
        return new RoomOrchestrator.LeaveResult(new RoomOrchestrator.RoomIdentity(
                command.expectedIdentity().roomId(), command.expectedIdentity().roomInstanceId(),
                command.expectedIdentity().gameId(), command.expectedIdentity().gameCode(),
                command.expectedIdentity().playVersion(), identity.lifecycleEpoch(),
                identity.fencingToken(), identity.stateVersion()), command.accountId(),
                RoomOrchestrator.LeaveStatus.ALREADY_LEFT, seatNo, command.commandId());
    }

    private Map<String, Object> createGateway(Map<String, Object> command) {
        long roomId = positive(command, "roomId");
        synchronized (lock(roomId)) {
            return createLocked(command, roomId);
        }
    }

    private Map<String, Object> recoverGateway(Map<String, Object> command) {
        long roomId = positive(command, "roomId");
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            verifyIdentity(route, command);
            if ("REMOVED".equals(route.state()) || "REMOVING".equals(route.state())) {
                throw new IllegalStateException("room authority is closed");
            }
            Route claimed = claimForRecovery(route);
            if (claimed == null) throw new SecurityException("room authority is owned by another node");
            try {
                requireOrCreateRuntime(command, claimed);
                transition(roomId, claimed.fencingToken(), "ACTIVE");
                return response(claimed);
            } catch (RuntimeException failure) {
                failRoute(roomId, claimed.fencingToken(), failure);
                throw failure;
            }
        }
    }

    private RoomOrchestrator.CreateCommand toCreateCommand(Map<String, Object> command) {
        long roomId = positive(command, "roomId");
        long ownerId = positive(command, "ownerId");
        int gameId = Math.toIntExact(positive(command, "gameId"));
        String playVersion = required(command, "playVersion");
        GameProvider provider = games.require(gameId, playVersion);
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireCanonicalProjection(roomId, gameId, playVersion);
        Object rawRules = command.get("rules");
        if (!(rawRules instanceof Map<?, ?> rules)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_CREATE_RULES_REQUIRED");
        }
        Map<String, Object> immutableRules = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rules.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_CREATE_RULE_KEY_INVALID");
            }
            immutableRules.put(key, entry.getValue());
        }
        return new RoomOrchestrator.CreateCommand(required(command, "requestId"),
                new RoomCreationContext(roomId, ownerId, immutableRules, null, canonical.source()),
                provider.descriptor().code(), playVersion, positive(command, "fencingToken"),
                required(command, "traceId"));
    }

    private RoomOrchestrator.RecoverCommand toRecoverCommand(Map<String, Object> command) {
        long roomId = positive(command, "roomId");
        int gameId = Math.toIntExact(positive(command, "gameId"));
        String playVersion = required(command, "playVersion");
        GameProvider provider = games.require(gameId, playVersion);
        RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(roomId);
        if (!stored.active()) throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROYED");
        RoomOrchestrator.RoomIdentity expected = new RoomOrchestrator.RoomIdentity(roomId,
                stored.identity().roomInstanceId(), gameId, provider.descriptor().code(), playVersion,
                stored.identity().lifecycleEpoch(), stored.identity().fencingToken(),
                stored.identity().stateVersion());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("stateVersion", stored.identity().stateVersion());
        return new RoomOrchestrator.RecoverCommand(expected,
                required(command, "sessionId"), required(command, "requestId"),
                com.aoo.bcg.gamespi.StatePayload.copyOf(snapshot), positive(command, "ownerId"));
    }

    private RoomOrchestrator.LeaveCommand toLeaveCommand(Map<String, Object> command) {
        requireLeaveIdentityFields(command);
        long roomId = positive(command, "roomId");
        long accountId = positive(command, "accountId");
        String playVersion = required(command, "playVersion");
        String commandId = required(command, "requestId");
        String traceId = required(command, "traceId");
        String roomInstanceId = required(command, "roomInstanceId");
        long lifecycleEpoch = positive(command, "lifecycleEpoch");
        long fencingToken = positive(command, "fencingToken");
        long stateVersion = nonNegative(command, "stateVersion");
        long eventSequence = command.containsKey("eventSequence")
                ? nonNegative(command, "eventSequence") : stateVersion;
        String eventId = command.containsKey("eventId")
                ? required(command, "eventId") : commandId;
        Route route = route(roomId);
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireCanonicalProjection(roomId, route.gameId(), route.playVersion());
        if (!playVersion.equals(route.playVersion())
                || canonical.gameId() != route.gameId()
                || !canonical.playVersion().equals(route.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT");
        }
        GameProvider provider = games.require(route.gameId(), route.playVersion());
        RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(roomId,
                roomInstanceId, route.gameId(), provider.descriptor().code(), route.playVersion(),
                lifecycleEpoch, fencingToken, stateVersion);
        requireLeaveCanonicalMetadata(command, canonical.source());
        requireLeaveStaticIdentity(command, identity);
        return new RoomOrchestrator.LeaveCommand(identity, accountId, commandId, traceId,
                eventSequence, eventId);
    }

    private RoomOrchestrator.ReadyCommand toReadyCommand(ConnectionSession session,
                                                          WebSocketFrame frame,
                                                          GameRoomHandle room) {
        long roomId;
        try {
            roomId = Long.parseLong(frame.roomId());
        } catch (NumberFormatException invalidRoom) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_ROOM_ID_INVALID", invalidRoom);
        }
        if (roomId != room.roomId()) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_ROOM_ID_CONFLICT");
        }
        Route route = route(roomId);
        if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                || !clock.instant().isBefore(route.leaseExpiresAt())) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_REJECTED");
        }
        requireCanonicalRoomLock(route);
        GameProvider provider = games.require(route.gameId(), route.playVersion());
        RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(roomId);
        if (!stored.active() || room.runtimeIdentity() == null
                || !room.runtimeIdentity().equals(stored.identity())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_RUNTIME_IDENTITY_CONFLICT");
        }
        long accountId;
        try {
            accountId = Long.parseLong(session.userId());
        } catch (NumberFormatException invalidAccount) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_ACCOUNT_INVALID", invalidAccount);
        }
        if (accountId <= 0 || session.seatId() < 0) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_PERMISSION_DENIED");
        }
        RoomOrchestrator.RoomIdentity identity = authorityIdentity(route, stored.identity());
        Map<String, Object> state = room.requireAuthoritativeSession().authoritativeState();
        requireReadyPermission(state, accountId, session.seatId());
        return new RoomOrchestrator.ReadyCommand(identity,
                new RoomOrchestrator.RoomStaticIdentity(route.roomId(), route.gameId(),
                        provider.descriptor().code(), route.playVersion()),
                accountId, session.seatId(), RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                requireReadyRoomState(state), frame.requestId(), CommandPayload.copyOf(frame.body()));
    }

    private static void requireReadyPermission(Map<String, Object> authorityState,
                                               long accountId, int seatNo) {
        if (accountId <= 0 || seatNo < 0 || !seatBelongsToPlayer(authorityState, seatNo, accountId)) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_PERMISSION_DENIED");
        }
    }

    private static RoomState requireReadyRoomState(Map<String, Object> authorityState) {
        Object rawState = authorityState.get("state");
        Object rawPhase = authorityState.get("phase");
        String state = rawState instanceof String value && !value.isBlank() ? value.strip() : null;
        String phase = rawPhase instanceof String value && !value.isBlank() ? value.strip() : null;
        if (state != null && phase != null
                && !state.equalsIgnoreCase(phase)) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_CONFLICT");
        }
        String value = state != null ? state : phase;
        if (value == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_REQUIRED");
        }
        try {
            return RoomState.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_INVALID", invalid);
        }
    }

    static void requireLeaveIdentityFields(Map<String, Object> command) {
        if (command == null) throw new IllegalArgumentException(
                "ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED");
        try {
            required(command, "roomInstanceId");
            positive(command, "lifecycleEpoch");
            positive(command, "fencingToken");
            nonNegative(command, "stateVersion");
            required(command, "requestId");
            required(command, "idempotencyKey");
            positive(command, "gameId");
            required(command, "gameCode");
            positive(command, "releaseId");
            positive(command, "indexGeneration");
            required(command, "regionCode");
            required(command, "sourceHash");
            required(command, "ruleContentHash");
            required(command, "componentContentHash");
            required(command, "cardCodecVersion");
            required(command, "eventInterpreterVersion");
            required(command, "protocolVersion");
            required(command, "snapshotSchemaVersion");
        } catch (RuntimeException missing) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED", missing);
        }
        if (!String.valueOf(command.get("requestId")).equals(String.valueOf(command.get("idempotencyKey")))) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_IDEMPOTENCY_CONFLICT");
        }
    }

    static void requireWsLeaveIdentityFields(Map<String, Object> command) {
        requireLeaveIdentityFields(command);
        required(command, "sessionId");
        if (command.containsKey("accountSessionId") || command.containsKey("actorSessionId")
                || command.containsKey("actorType")) {
            throw new SecurityException("ROOM_ORCHESTRATOR_LEAVE_WS_ACTOR_CONFLICT");
        }
        if (!"SEATED_MEMBER".equals(String.valueOf(command.get("permission")))) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_PERMISSION_REJECTED");
        }
    }

    private static void requireLeaveEventIdentityFields(Map<String, Object> command) {
        long eventSequence = nonNegative(command, "eventSequence");
        String eventId = required(command, "eventId");
        if (!String.valueOf(command.get("requestId")).equals(eventId)) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_EVENT_ID_CONFLICT");
        }
        if (eventSequence < 0) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_EVENT_SEQUENCE_INVALID");
        }
    }

    private static void requireLeaveCanonicalMetadata(
            Map<String, Object> command,
            com.aoo.bcg.gamespi.RoomSnapshotMetadataSource source) {
        if (positive(command, "releaseId") != source.releaseId()
                || positive(command, "indexGeneration") != source.indexGeneration()
                || !required(command, "regionCode").equals(source.regionCode())
                || !required(command, "ruleContentHash").equalsIgnoreCase(source.ruleContentHash())
                || !required(command, "componentContentHash").equalsIgnoreCase(source.componentContentHash())
                || !required(command, "cardCodecVersion").equals(source.cardCodecVersion())
                || !required(command, "eventInterpreterVersion").equals(source.eventInterpreterVersion())
                || !required(command, "protocolVersion").equals(source.protocolVersion())
                || !required(command, "snapshotSchemaVersion").equals(source.snapshotSchemaVersion())
                || !required(command, "sourceHash").equalsIgnoreCase(source.sourceHash())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_CANONICAL_METADATA_CONFLICT");
        }
    }

    private static void requireLeaveStaticIdentity(Map<String, Object> command,
                                                   RoomOrchestrator.RoomIdentity identity) {
        Object gameId = command.get("gameId");
        if (gameId != null && strictLeaveGameId(gameId) != identity.gameId()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT");
        }
        for (Map.Entry<String, String> value : Map.of(
                "gameCode", identity.gameCode(), "playVersion", identity.playVersion(),
                "roomInstanceId", identity.roomInstanceId()).entrySet()) {
            Object supplied = command.get(value.getKey());
            if (supplied != null && (!String.valueOf(supplied).equals(value.getValue()))) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT");
            }
        }
    }

    private static int strictLeaveGameId(Object value) {
        long parsed;
        if (value instanceof String text && text.matches("[1-9][0-9]*")) {
            try {
                parsed = Long.parseLong(text);
            } catch (NumberFormatException overflow) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT", overflow);
            }
        } else if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            parsed = ((Number) value).longValue();
        } else {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT");
        }
        if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT");
        }
        return (int) parsed;
    }

    private Map<String, Object> createCommandMap(RoomOrchestrator.CreateCommand command) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("roomId", command.context().roomId());
        value.put("ownerId", command.context().ownerId());
        value.put("requestId", command.requestId());
        value.put("traceId", command.requireTraceId());
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                roomMetadata.requireProjection(command.context().roomId());
        if (!canonical.playVersion().equals(command.playVersion())) {
            throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_IDENTITY_CONFLICT",
                    "ROOM_LOCK_IDENTITY_CONFLICT");
        }
        GameProvider provider = games.require(canonical.gameId(), command.playVersion());
        if (!provider.descriptor().code().equals(command.gameCode())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_CREATE_IDENTITY_CONFLICT");
        }
        value.put("gameId", canonical.gameId());
        value.put("playVersion", command.playVersion());
        value.put("fencingToken", command.requireFencingToken());
        value.put("rules", command.context().immutableRules().asMap());
        return value;
    }

    private Map<String, Object> recoverCommandMap(RoomOrchestrator.RecoverCommand command) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("roomId", command.expectedIdentity().roomId());
        value.put("gameId", command.expectedIdentity().gameId());
        value.put("playVersion", command.expectedIdentity().playVersion());
        value.put("requestId", command.requestId());
        value.put("sessionId", command.sessionId());
        value.put("ownerId", command.requireOwnerId());
        value.put("fencingToken", command.expectedIdentity().fencingToken());
        value.put("rules", command.snapshot().asMap());
        return value;
    }

    private RoomOrchestrator.CreateResult createResult(RoomOrchestrator.CreateCommand command,
                                                       Map<String, Object> ignoredResponse) {
        GameRoomHandle room = rooms.require(command.context().roomId());
        RoomRuntimeIdentity runtimeIdentity = requireActiveRuntimeIdentity(room);
        RoomOrchestrator.RoomIdentity identity = authorityIdentity(route(room.roomId()), runtimeIdentity);
        if (!identity.gameCode().equals(command.gameCode())
                || !identity.playVersion().equals(command.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_CREATE_IDENTITY_CONFLICT");
        }
        return new RoomOrchestrator.CreateResult(identity, statePayload(room));
    }

    private RoomOrchestrator.RecoverResult recoverResult(RoomOrchestrator.RecoverCommand command,
                                                         Map<String, Object> ignoredResponse) {
        GameRoomHandle room = rooms.require(command.expectedIdentity().roomId());
        RoomRuntimeIdentity runtimeIdentity = requireActiveRuntimeIdentity(room);
        RoomOrchestrator.RoomIdentity identity = authorityIdentity(route(room.roomId()), runtimeIdentity);
        return new RoomOrchestrator.RecoverResult(identity, statePayload(room));
    }

    @Override
    public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
        Objects.requireNonNull(command, "command");
        validateDestroyIdentity(command.expectedIdentity());
        AuthorityDestroyResult result = destroy(command.expectedIdentity().roomId(),
                command.expectedIdentity().fencingToken(),
                new RoomRuntimeIdentity(command.expectedIdentity().roomInstanceId(),
                        command.expectedIdentity().lifecycleEpoch(),
                        command.expectedIdentity().fencingToken(),
                        command.expectedIdentity().stateVersion()),
                command.requestId(), command.reason());
        if (result.identity() == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED");
        }
        if ("DESTROYED".equals(result.status())) {
            return new RoomOrchestrator.DestroyResult(result.identity(), true, "DESTROYED");
        }
        if ("ALREADY_DESTROYED".equals(result.status())) {
            return new RoomOrchestrator.DestroyResult(result.identity(), false, "ALREADY_DESTROYED");
        }
        throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_" + result.status());
    }

    /** Production ingress must cross the single adapter; callers must not invoke the raw port. */
    RoomOrchestrator.DestroyResult destroyThroughRoomOrchestrator(
            RoomOrchestrator.DestroyCommand command) {
        return roomOrchestrator.destroy(command);
    }

    @Override
    public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) {
        if (expectedIdentity == null || expectedIdentity.roomId() <= 0
                || expectedIdentity.gameId() <= 0 || expectedIdentity.gameCode().isBlank()
                || expectedIdentity.playVersion().isBlank()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED");
        }
        final Route route;
        try {
            route = route(expectedIdentity.roomId());
        } catch (RuntimeException missing) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_NOT_FOUND", missing);
        }
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireDestroyCanonical(route);
        final GameProvider provider;
        try {
            provider = games.require(route.gameId(), route.playVersion());
        } catch (RuntimeException missing) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED", missing);
        }
        boolean matches = expectedIdentity.roomId() == route.roomId()
                && expectedIdentity.gameId() == route.gameId()
                && expectedIdentity.gameId() == canonical.gameId()
                && expectedIdentity.playVersion().equals(route.playVersion())
                && expectedIdentity.playVersion().equals(canonical.playVersion())
                && expectedIdentity.gameCode().equals(provider.descriptor().code());
        if (!matches) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_CAS_CONFLICT_STATIC_IDENTITY");
        }
    }

    private RoomConfigurationSnapshotMetadataProvider.CanonicalProjection requireDestroyCanonical(
            Route route) {
        final RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical;
        try {
            canonical = roomMetadata.requireProjection(route.roomId());
        } catch (RuntimeException missing) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED", missing);
        }
        if (canonical.gameId() != route.gameId()
                || !canonical.playVersion().equals(route.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_CAS_CONFLICT_STATIC_IDENTITY");
        }
        return canonical;
    }

    private RoomConfigurationSnapshotMetadataProvider.CanonicalProjection requireCanonicalProjection(
            long roomId, int gameId, String playVersion) {
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                roomMetadata.requireProjection(roomId);
        if (canonical.gameId() != gameId || !canonical.playVersion().equals(playVersion)) {
            throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_IDENTITY_CONFLICT",
                    "ROOM_LOCK_IDENTITY_CONFLICT");
        }
        return canonical;
    }

    private RoomRuntimeIdentity requireActiveRuntimeIdentity(GameRoomHandle room) {
        if (room.runtimeIdentity() == null) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_REQUIRED");
        }
        RoomRuntimeIdentityStore.Entry stored = roomIdentityStore.require(room.roomId());
        if (!stored.active() || !stored.identity().equals(room.runtimeIdentity())) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CONFLICT");
        }
        return stored.identity();
    }

    private com.aoo.bcg.gamespi.StatePayload statePayload(GameRoomHandle room) {
        var session = room.requireAuthoritativeSession();
        Map<String, Object> state = new LinkedHashMap<>(session.authoritativeState());
        Object raw = state.get("stateVersion");
        if (raw != null && (!(raw instanceof Number number)
                || number.longValue() != session.stateVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_STATE_VERSION_CONFLICT");
        }
        state.put("stateVersion", session.stateVersion());
        return com.aoo.bcg.gamespi.StatePayload.copyOf(state);
    }

    private Map<String, Object> legacyResponse(RoomOrchestrator.AuthorityResult result) {
        RoomOrchestrator.RoomIdentity identity = result.identity();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("roomId", identity.roomId());
        value.put("gameId", identity.gameId());
        value.put("playVersion", identity.playVersion());
        value.put("nodeId", nodeId);
        value.put("route", endpoint);
        value.put("fencingToken", identity.fencingToken());
        value.put("roomInstanceId", identity.roomInstanceId());
        value.put("lifecycleEpoch", canonicalLifecycleEpoch(identity.lifecycleEpoch()));
        value.put("stateVersion", identity.stateVersion());
        value.put("state", "ACTIVE");
        return value;
    }

    /**
     * Room creation/join responses cross a JavaScript boundary before a later
     * reconnect or handoff.  Keep the lifecycle epoch as a canonical decimal
     * string so a 19-digit signed-long value cannot be rounded by JSON number
     * parsing in the Client runtime identity mount.
     */
    static String canonicalLifecycleEpoch(long lifecycleEpoch) {
        if (lifecycleEpoch < 0) {
            throw new IllegalArgumentException("ROOM_LIFECYCLE_EPOCH_INVALID");
        }
        return Long.toString(lifecycleEpoch);
    }

@Override
public Map<String, Object> join(Map<String, Object> command) {
    long roomId = positive(command, "roomId");
    long accountId = positive(command, "accountId");
    int seatNo = Math.toIntExact(nonNegative(command, "seatNo"));
    String playVersion = required(command, "playVersion");
    String requestId = required(command, "requestId");
    synchronized (lock(roomId)) {
        Route route = route(roomId);
        if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                || !clock.instant().isBefore(route.leaseExpiresAt())) {
            throw new SecurityException("room authority is not active on this node");
        }
        if (!route.playVersion().equals(playVersion)) {
            throw new SecurityException("room membership playVersion mismatch");
        }
        GameRoomHandle room = rooms.require(roomId);
        RoomRuntimeIdentityStore.Entry stored = requireJoinRuntimeIdentity(room, route);
        var session = room.requireAuthoritativeSession();
        boolean spectatorEntry = usesExplicitSeatSelection(room);
            if(route.firstRoundStartedAt()==null
                && !clock.instant().isBefore(route.lastBusinessActivityAt().plus(WaitingRoomExpirationPolicy.TIMEOUT))) {
            if(!WaitingRoomExpirationPolicy.hasExplicitLifecycleMarker(session.authoritativeState()))
                System.err.println("[RoomLifecycleCompatibility] roomId="+roomId+" playerId="+accountId
                        +" operationId="+requestId+" stateVersion="+session.stateVersion()
                        +" action=RETAIN_UNKNOWN_WAITING_SNAPSHOT");
            if(WaitingRoomExpirationPolicy.expired(route.lastBusinessActivityAt(),clock.instant(),session.authoritativeState())) {
                RoomAuthorityBusinessError expired = expireAndClose(room, route);
                if (expired != null) throw expired;
            }
        }
        // Fence the durable route before mutating the authoritative session.  The
        // row lock serializes this join with automatic expiration on every Gateway
        // node; if expiration already won, the typed 409 is returned before any
        // membership/session state can be changed.
        touchWaitingRoomActivity(roomId, route.fencingToken());
        try {
            if(!spectatorEntry)admit(session,accountId,seatNo,command.get("admission"));
        } catch (IllegalArgumentException | IllegalStateException rejected) {
            if ("invalid admission identity".equals(rejected.getMessage())) {
                throw new RoomAuthorityBusinessError(409,"ROOM_FULL","房间人数已满");
            }
            throw new RoomAuthorityBusinessError(409,"ROOM_JOIN_REJECTED","room rejected membership");
        }
        int roundNo = session.authoritativeState().get("roundNo") instanceof Number number
                ? Math.max(0, number.intValue()) : 0;
        if(!spectatorEntry)session.execute(new com.aoo.bcg.gamespi.GameCommandRequest("common.room.join_req",
                requestId, Math.max(1L, session.stateVersion() + 1L), roomId, roundNo,
                playVersion, Long.toString(accountId), seatNo, Map.of()));
        RoomRuntimeIdentity advanced = advanceIdentityIfRequired(
                roomId, stored.identity(), session.stateVersion());
        GameRoomHandle committedRoom = room;
        if (!advanced.equals(room.runtimeIdentity())) {
            committedRoom = copyWithIdentity(room, advanced);
            rooms.updateRuntime(committedRoom);
        }
        stateStore.saveSnapshot(committedRoom, route.fencingToken());
        // 加入由 Hall 经内部端口完成，不经过玩家 WSS 的 publish 链。如果这里不主动
        // 广播，已在房内的客户端会永久停留在旧成员快照，进而无法满足“满员后准备”。
        // 广播器按接收者重新生成私有视图，不会泄露其他玩家暗牌。
        broadcasts.publishAuthoritativeState(roomId, requestId,
                String.valueOf(command.getOrDefault("traceId", requestId)), session.stateVersion());
        publishWaitingRoomReadyIfFull(route, session.authoritativeState(), requestId,
                String.valueOf(command.getOrDefault("traceId", requestId)));
        return Map.of("roomId", roomId, "accountId", accountId, "seatNo", seatNo,
                "playVersion", playVersion, "stateVersion", session.stateVersion(),
                "participantRole",spectatorEntry?"SPECTATOR":"SEATED");
    }

    }

    /** Fresh-room membership may never mount a room whose durable generation is absent or stale. */
    private RoomRuntimeIdentityStore.Entry requireJoinRuntimeIdentity(GameRoomHandle room, Route route) {
        final RoomRuntimeIdentityStore.Entry stored;
        try {
            stored = roomIdentityStore.require(room.roomId());
        } catch (RuntimeException missing) {
            throw new RoomAuthorityBusinessError(409, "ROOM_JOIN_RUNTIME_IDENTITY_REQUIRED",
                    "room runtime identity is required before membership join");
        }
        if (!stored.active() || room.runtimeIdentity() == null
                || !room.runtimeIdentity().equals(stored.identity())
                || stored.identity().fencingToken() != route.fencingToken()) {
            throw new RoomAuthorityBusinessError(409, "ROOM_JOIN_RUNTIME_IDENTITY_CONFLICT",
                    "room runtime identity does not match the active authority fence");
        }
        return stored;
    }

    private boolean usesExplicitSeatSelection(GameRoomHandle room) {
        String code=games.require(room.gameId(),room.playVersion()).descriptor().code();
        return java.util.Set.of("chengdu2999","quanguo2998","quanguo2997").contains(code);
    }

    private void publishWaitingRoomReadyIfFull(Route route,Map<String,Object> state,
            String requestId,String traceId){
        Object rawPlayers=state.get("players");
        if(!(rawPlayers instanceof Map<?,?> players)||players.isEmpty())return;
        String sql="SELECT g.game_code,CAST(JSON_UNQUOTE(JSON_EXTRACT(r.rules_json,'$.playerCount')) AS UNSIGNED) player_count "
                + "FROM aoo_hall_room r JOIN aoo_game_catalog g ON g.game_id=r.game_id WHERE r.room_id=?";
        try(Connection connection=dataSource.getConnection();PreparedStatement query=connection.prepareStatement(sql)){
            query.setLong(1,route.roomId());
            try(ResultSet row=query.executeQuery()){
                if(!row.next()||!"liangshan2003".equalsIgnoreCase(row.getString("game_code"))
                        ||row.getInt("player_count")<=0||players.size()<row.getInt("player_count"))return;
            }
            List<Long> members=players.values().stream().map(value->Long.parseLong(String.valueOf(value))).toList();
            // Hall confirms JOINING -> JOINED immediately after this authority call. The last
            // joiner's HTTP commit and both lobby sockets can cross this callback, so a single
            // edge is insufficient. Repeat the idempotent wake-up over a short bounded window;
            // clients always re-read authoritative membership before issuing their own ticket.
            for(long delay:new long[]{100L,500L,1500L}){
                lifecycleWorker.schedule(()->{
                    int published=GatewaySessionRegistry.global().publishWaitingRoomReady(
                            members,route.roomId(),requestId,traceId,json,clock);
                    System.out.println("[WaitingRoomReady] roomId="+route.roomId()
                            +" recipients="+members.size()+" published="+published+" delayMs="+delay);
                },delay,TimeUnit.MILLISECONDS);
            }
        }catch(SQLException failure){throw new IllegalStateException("waiting room ready lookup failed",failure);}
    }

    static Map<String, Object> leaveResponse(RoomOrchestrator.LeaveResult result) {
        RoomOrchestrator.RoomIdentity identity = result.identity();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("left", result.status() == RoomOrchestrator.LeaveStatus.LEFT
                || result.status() == RoomOrchestrator.LeaveStatus.ALREADY_LEFT);
        value.put("status", result.status().name());
        value.put("roomId", identity.roomId());
        value.put("accountId", result.accountId());
        value.put("seatNo", result.seatNo());
        value.put("gameId", identity.gameId());
        value.put("gameCode", identity.gameCode());
        value.put("playVersion", identity.playVersion());
        value.put("roomInstanceId", identity.roomInstanceId());
        value.put("lifecycleEpoch", Long.toString(identity.lifecycleEpoch()));
        value.put("fencingToken", identity.fencingToken());
        value.put("stateVersion", identity.stateVersion());
        value.put("eventSequence", Long.toString(result.eventSequence()));
        value.put("eventId", result.eventId());
        value.put("commandId", result.commandId());
        return Map.copyOf(value);
    }

    private static boolean isTerminalAuthoritySession(
            com.aoo.bcg.gamespi.AuthoritativeGameSession session,
            Map<String,Object> authorityState) {
        if (session instanceof RoomLifecycleAuthority lifecycle && lifecycle.isTerminal()) return true;
        if (Boolean.TRUE.equals(authorityState.get("roomTerminal"))
                || Boolean.TRUE.equals(authorityState.get("dissolved"))) return true;
        return "FINISHED".equalsIgnoreCase(String.valueOf(authorityState.get("phase")));
    }

    @Override
    public void remove(long roomId, long fencingToken) {
        AuthorityDestroyResult result = destroy(roomId, fencingToken,
                "legacy-remove-" + roomId, "LEGACY_REMOVE");
        if (!result.success()) {
            throw new IllegalStateException("ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED:" + result.status());
        }
    }

    /**
     * Typed authority destroy used by the next orchestrator adapter. The
     * legacy void interface delegates here but cannot hide CAS/tombstone
     * failures. No room identity is derived when the authority row is absent.
     */
    AuthorityDestroyResult destroy(long roomId, long fencingToken,
                                   String requestId, String reason) {
        return destroy(roomId, fencingToken, null, requestId, reason);
    }

    /**
     * Typed destroy variant used by create-failure compensation. The expected
     * identity is checked before the tombstone CAS so a later generation can
     * never be sealed by an earlier create attempt.
     */
    AuthorityDestroyResult destroy(long roomId, long fencingToken,
                                   RoomRuntimeIdentity expectedIdentity,
                                   String requestId, String reason) {
        return destroyInternal(roomId,fencingToken,expectedIdentity,requestId,reason,null);
    }
    private AuthorityDestroyResult destroyInternal(long roomId,long fencingToken,RoomRuntimeIdentity expectedIdentity,String requestId,String reason,String ownedCreateRequestId) {
        if (roomId <= 0 || fencingToken <= 0 || requestId == null || requestId.isBlank()
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("ROOM_AUTHORITY_DESTROY_REQUEST_INVALID");
        }
        synchronized (lock(roomId)) {
            final Route route;
            try {
                route = route(roomId);
            } catch (RuntimeException missing) {
                return AuthorityDestroyResult.notFound();
            }
            requireDestroyCanonical(route);
            final RoomRuntimeIdentityStore.Entry stored;
            try {
                stored = roomIdentityStore.require(roomId);
            } catch (RuntimeException missing) {
                return AuthorityDestroyResult.notFound();
            }
            if (expectedIdentity != null && !expectedIdentity.equals(stored.identity())) {
                return new AuthorityDestroyResult("CAS_CONFLICT",
                        authorityIdentity(route, stored.identity()));
            }
            RoomOrchestrator.RoomIdentity identity = authorityIdentity(route, stored.identity());
            if (stored.state() == RoomRuntimeIdentityStore.LifecycleState.DESTROYED
                    || "REMOVED".equals(route.state())) {
                handoffReceipts.remove(roomId);
                handoffLeases.remove(roomId);
                return new AuthorityDestroyResult("ALREADY_DESTROYED", identity);
            }
            if (!stored.active() || route.fencingToken() != fencingToken
                    || (!"ACTIVE".equals(route.state())
                    && !("CREATE_RUNTIME_FAILURE".equals(reason)
                    && "CREATING".equals(route.state())))) {
                return new AuthorityDestroyResult("CAS_CONFLICT", identity);
            }
            try {
                try(var connection=dataSource.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        var billingLifecycle=new JdbcRoomBillingLifecyclePort(dataSource,()->{throw new IllegalStateException("destroy cannot allocate billing IDs");},clock);
                        var audit=new RoomRuntimeIdentityStore.AuditContext(requestId,"JdbcGatewayRoomAuthority.destroy."+reason);
                        if(ownedCreateRequestId==null)billingLifecycle.destroy(connection,roomIdentityStore,roomId,stored.identity(),audit);
                        else billingLifecycle.cleanupOwnedCreate(connection,roomIdentityStore,roomId,stored.identity(),ownedCreateRequestId,audit);
                        connection.commit();
                    } catch(Exception failure) { connection.rollback();throw failure; }
                } catch(java.sql.SQLException failure) { throw new IllegalStateException("room destroy receipt persistence failed",failure); }
            } catch (RuntimeException conflict) {
                return new AuthorityDestroyResult(destroyFailureCode(conflict), identity);
            }
            transition(roomId, fencingToken, "REMOVING");
            rooms.remove(roomId);
            transition(roomId, fencingToken, "REMOVED");
            roomLocks.remove(roomId);
            reconnectReceipts.remove(roomId);
            handoffReceipts.remove(roomId);
            handoffLeases.remove(roomId);
            return new AuthorityDestroyResult("DESTROYED", identity);
        }
    }

    private RoomOrchestrator.RoomIdentity authorityIdentity(Route route, RoomRuntimeIdentity identity) {
        var descriptor = games.require(route.gameId(), route.playVersion()).descriptor();
        return new RoomOrchestrator.RoomIdentity(route.roomId(), identity.roomInstanceId(),
                route.gameId(), descriptor.code(), route.playVersion(), identity.lifecycleEpoch(),
                identity.fencingToken(), identity.stateVersion());
    }

    private static String destroyFailureCode(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) return "CAS_CONFLICT";
        if (message.contains("DESTROYED")) return "ALREADY_DESTROYED";
        if (message.contains("NOT_FOUND")) return "NOT_FOUND";
        if (message.contains("CONFLICT")) return "CAS_CONFLICT";
        return "CAS_CONFLICT";
    }

    record AuthorityDestroyResult(String status, RoomOrchestrator.RoomIdentity identity) {
        AuthorityDestroyResult {
            if (status == null || status.isBlank()) throw new IllegalArgumentException("destroy status required");
        }

        static AuthorityDestroyResult notFound() {
            return new AuthorityDestroyResult("NOT_FOUND", null);
        }

        boolean success() {
            return "DESTROYED".equals(status) || "ALREADY_DESTROYED".equals(status);
        }
    }

    void completeTerminal(long roomId,String requestId,String traceId,String reason) {
        synchronized(lock(roomId)) {
            GameRoomHandle room;
            try { room=rooms.require(roomId); } catch(IllegalArgumentException alreadyRemoved) { return; }
            if(!(room.requireAuthoritativeSession() instanceof RoomLifecycleAuthority lifecycle)||!lifecycle.isTerminal())throw new IllegalStateException("room lifecycle is not terminal");
            Route route=route(roomId);
            if("REMOVED".equals(route.state())){rooms.remove(roomId);return;}
            if(!nodeId.equals(route.nodeId()))throw new SecurityException("room lifecycle owned by another node");
            if("ACTIVE".equals(route.state()))transition(roomId,route.fencingToken(),"REMOVING");else if(!"REMOVING".equals(route.state()))throw new IllegalStateException("room lifecycle route is not removable: "+route.state());
            hallLifecycle.close(roomId,requestId,traceId,reason);
            rooms.remove(roomId);
            transition(roomId,route.fencingToken(),"REMOVED");
            roomLocks.remove(roomId);
        }
    }

    void completeMemberExit(long roomId,long accountId,String requestId,String traceId) {
        synchronized(lock(roomId)) {
            GameRoomHandle room=rooms.require(roomId);
            Object rawPlayers=room.requireAuthoritativeSession().authoritativeState().get("players");
            if(rawPlayers instanceof Map<?,?> players&&players.values().stream().anyMatch(value->Long.parseLong(String.valueOf(value))==accountId))throw new IllegalStateException("room member is still seated in authority state");
            Route route=route(roomId);
            if(!"ACTIVE".equals(route.state())||!nodeId.equals(route.nodeId()))throw new SecurityException("room membership lifecycle owned by another node");
            hallLifecycle.memberLeft(roomId,accountId,requestId,traceId);
        }
    }

    private void processRoomLifecycles() {
        if (maintenanceDue()) {
            try{finishInterruptedRemovals();}catch(RuntimeException failure){System.err.println("room removal recovery deferred cause="+failure.getMessage());}
            try{closeDurableInactiveRooms();}catch(RuntimeException failure){System.err.println("inactive room closure recovery deferred cause="+failure.getMessage());}
        }
        for(GameRoomHandle room:rooms.snapshot())try {
            RoomOrderedExecutor.global().execute(room.roomId(),()->{ processRoomLifecycle(room); return null; });
        } catch(RuntimeException failure){System.err.println("room lifecycle processing deferred roomId="+room.roomId()+" cause="+failure.getMessage());}
    }

    private boolean maintenanceDue() {
        long now = System.nanoTime();
        if (now < nextMaintenanceAtNanos) return false;
        nextMaintenanceAtNanos = now + MAINTENANCE_INTERVAL_NANOS;
        return true;
    }

    private void processRoomLifecycle(GameRoomHandle room) {
        try {
            if(expireWaitingRoom(room))return;
            if(expireInactiveRoom(room))return;
            if(!(room.requireAuthoritativeSession() instanceof RoomLifecycleAuthority lifecycle))return;
            if(room.requireAuthoritativeSession() instanceof com.aoo.bcg.gamespi.CarryBillingEffectSource){
                var checkpoint=beginCarry(room);
                try{boolean changed=lifecycle.tickLifecycle(clock.instant());
                    if(changed){var prior=room.runtimeIdentity();var next=new RoomRuntimeIdentity(prior.roomInstanceId(),prior.lifecycleEpoch(),prior.fencingToken(),room.requireAuthoritativeSession().stateVersion());var advanced=copyWithIdentity(room,next);stateStore.saveCarrySnapshot(advanced,prior);rooms.updateRuntime(advanced);}
                    stateStore.carryPort().finish(checkpoint,true);
                    if(changed)broadcasts.publishAuthoritativeState(room.roomId(),"room-lifecycle-"+room.roomId()+"-"+room.requireAuthoritativeSession().stateVersion(),"room-lifecycle-"+room.roomId(),room.requireAuthoritativeSession().stateVersion());
                    if(lifecycle.isTerminal())completeTerminal(room.roomId(),"room-lifecycle-"+room.roomId()+"-"+room.requireAuthoritativeSession().stateVersion(),"room-lifecycle-"+room.roomId(),lifecycle.terminalReason());
                }catch(RuntimeException|Error failure){boolean restored=false;try{restoreCarryDurable(room,checkpoint);restored=true;}catch(RuntimeException restoreFailure){failure.addSuppressed(restoreFailure);}stateStore.carryPort().finish(checkpoint,restored);throw failure;}
                return;
            }
            boolean changed=lifecycle.tickLifecycle(clock.instant());
            Route route=route(room.roomId());
            if(changed){long version=room.requireAuthoritativeSession().stateVersion();stateStore.saveSnapshot(room,route.fencingToken());broadcasts.publishAuthoritativeState(room.roomId(),"room-lifecycle-"+room.roomId()+"-"+version,"room-lifecycle-"+room.roomId(),version);}
            boolean durable=stateStore.latest(room.roomId()).map(snapshot->{Object value=snapshot.authoritativeState().get("stateVersion");return value instanceof Number number&&number.longValue()>=room.requireAuthoritativeSession().stateVersion();}).orElse(false);
            if(lifecycle.isTerminal()&&durable)completeTerminal(room.roomId(),"room-lifecycle-"+room.roomId()+"-"+room.requireAuthoritativeSession().stateVersion(),"room-lifecycle-"+room.roomId(),lifecycle.terminalReason());
        } catch(RuntimeException failure) {
            throw failure;
        }
    }

    /** Closes every inactive room even when a prior process lost its in-memory runtime. */
    private void closeDurableInactiveRooms(){
        List<long[]> expired=new ArrayList<>();
        String sql="SELECT room_id,fencing_token FROM aoo_room_authority_route WHERE node_id=? AND lifecycle_state='ACTIVE' AND last_business_activity_at<=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 12 HOUR) ORDER BY room_id LIMIT 256";
        try(Connection connection=dataSource.getConnection();PreparedStatement query=connection.prepareStatement(sql)){
            query.setString(1,nodeId);
            try(ResultSet rows=query.executeQuery()){while(rows.next())expired.add(new long[]{rows.getLong(1),rows.getLong(2)});}
        }catch(SQLException failure){throw new IllegalStateException("cannot enumerate inactive durable rooms",failure);}
        for(long[] item:expired){long roomId=item[0],fence=item[1];synchronized(lock(roomId)){
            Route route=route(roomId);
            if(!"ACTIVE".equals(route.state())||route.fencingToken()!=fence||!nodeId.equals(route.nodeId()))continue;
            String requestId="room-inactivity-expiration-"+roomId;
            beginInactiveRoomRemoval(roomId,fence,requestId);
            broadcasts.publishInactiveRoomExpired(roomId,requestId,requestId);
            hallLifecycle.close(roomId,requestId,requestId,"ROOM_INACTIVITY_EXPIRED");
            rooms.remove(roomId);
            finishAutomaticRemoval(roomId,fence);
            roomLocks.remove(roomId);
            System.out.println("[RoomInactivityExpiration] roomId="+roomId+" operationId="+requestId+" stateVersion=0 inactivityHours=12 recoverySource=DURABLE_ROUTE");
        }}
    }

    private boolean expireWaitingRoom(GameRoomHandle room){
        synchronized(lock(room.roomId())){
            var session=room.requireAuthoritativeSession();
            synchronized(session){
                Route route=route(room.roomId());
                if(!"ACTIVE".equals(route.state())||!nodeId.equals(route.nodeId())
                        ||route.firstRoundStartedAt()!=null)return false;
                if(!WaitingRoomExpirationPolicy.expired(route.lastBusinessActivityAt(),clock.instant(),session.authoritativeState()))return false;
                return expireAndClose(room, route) != null;
            }
        }
    }

    /**
     * Acquire the durable route fence before a join mutates the authoritative
     * session.  The SELECT FOR UPDATE and the conditional activity update share
     * one transaction, so an expiration either wins first (and the join gets a
     * typed ROOM_ENDED/409) or the join refreshes activity first (and expiration
     * loses its stale-time predicate).  A genuine node/fence/lease violation is
     * deliberately left as SecurityException and therefore remains HTTP 403.
     */
    private void touchWaitingRoomActivity(long roomId,long fence){
        try(Connection connection=dataSource.getConnection()){
            connection.setAutoCommit(false);
            try {
                Route current = selectForUpdate(connection, roomId);
                if (current == null || "REMOVING".equals(current.state())
                        || "REMOVED".equals(current.state())) {
                    throw new RoomAuthorityBusinessError(409, "ROOM_ENDED",
                            "room is no longer joinable");
                }
                if (current.fencingToken() != fence || !nodeId.equals(current.nodeId())
                        || !"ACTIVE".equals(current.state())) {
                    throw new SecurityException("waiting room activity is not owned by this authority");
                }
                if (current.firstRoundStartedAt() != null) {
                    throw new SecurityException("waiting room activity is unavailable after first round");
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE aoo_room_authority_route SET last_business_activity_at=CURRENT_TIMESTAMP(3),updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND fencing_token=? AND lifecycle_state='ACTIVE' AND first_round_started_at IS NULL")) {
                    update.setLong(1, roomId);
                    update.setLong(2, fence);
                    if (update.executeUpdate() != 1) {
                        Route latest = selectForUpdate(connection, roomId);
                        throw waitingRoomJoinFenceFailure(latest);
                    }
                }
                connection.commit();
            } catch (Exception failure) {
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                if (failure instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("waiting room activity persistence failed", failure);
            }
        } catch (RoomAuthorityBusinessError | SecurityException failure) {
            throw failure;
        } catch (SQLException failure) {
            throw new IllegalStateException("waiting room activity persistence failed", failure);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("waiting room activity persistence failed", failure);
        }
    }

    private static RuntimeException waitingRoomJoinFenceFailure(Route route) {
        if (route == null || "REMOVING".equals(route.state()) || "REMOVED".equals(route.state())) {
            return new RoomAuthorityBusinessError(409, "ROOM_ENDED", "room is no longer joinable");
        }
        return new SecurityException("waiting room activity lost authority race");
    }

    private static RoomAuthorityBusinessError waitingRoomEndedIfRemoving(Route route) {
        RuntimeException failure = waitingRoomJoinFenceFailure(route);
        return failure instanceof RoomAuthorityBusinessError business ? business : null;
    }

    private boolean expireInactiveRoom(GameRoomHandle room){
        synchronized(lock(room.roomId())){
            var session=room.requireAuthoritativeSession();
            synchronized(session){
                Route route=route(room.roomId());
                if(!"ACTIVE".equals(route.state())||!nodeId.equals(route.nodeId()))return false;
                if(!WaitingRoomExpirationPolicy.roomInactive(route.lastBusinessActivityAt(),clock.instant()))return false;
                String requestId="room-inactivity-expiration-"+room.roomId();
                beginInactiveRoomRemoval(room.roomId(),route.fencingToken(),requestId);
                broadcasts.publishInactiveRoomExpired(room.roomId(),requestId,requestId);
                hallLifecycle.close(room.roomId(),requestId,requestId,"ROOM_INACTIVITY_EXPIRED");
                rooms.remove(room.roomId());
                finishAutomaticRemoval(room.roomId(),route.fencingToken());
                roomLocks.remove(room.roomId());
                System.out.println("[RoomInactivityExpiration] roomId="+room.roomId()+" operationId="+requestId+" stateVersion="+session.stateVersion()+" inactivityHours=12");
                return true;
            }
        }
    }

    /** 所有到期入口共用同一持久化关闭链，确保成员、票据和可恢复运行态一起释放。 */
    private RoomAuthorityBusinessError expireAndClose(GameRoomHandle room,Route route) {
        String requestId="waiting-room-expiration-"+room.roomId();
        if (!beginAutomaticRemoval(room.roomId(),route.fencingToken(),requestId)) {
            Route latest = route(room.roomId());
            return waitingRoomEndedIfRemoving(latest);
        }
        broadcasts.publishWaitingRoomExpired(room.roomId(),requestId,requestId);
        hallLifecycle.close(room.roomId(),requestId,requestId,"WAITING_ROOM_EXPIRED");
        rooms.remove(room.roomId());
        finishAutomaticRemoval(room.roomId(),route.fencingToken());
        roomLocks.remove(room.roomId());
        return new RoomAuthorityBusinessError(409,"ROOM_ENDED","房间超过2小时未开始，已自动解散");
    }

    private boolean beginAutomaticRemoval(long roomId,long fence,String requestId){try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE aoo_room_authority_route SET lifecycle_state='REMOVING',last_request_id=?,trace_id=?,updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND fencing_token=? AND lifecycle_state='ACTIVE' AND first_round_started_at IS NULL AND last_business_activity_at<=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 2 HOUR)")){p.setString(1,requestId);p.setString(2,requestId);p.setLong(3,roomId);p.setLong(4,fence);return p.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException("waiting room expiration persistence failed",e);}}

    private void beginInactiveRoomRemoval(long roomId,long fence,String requestId){try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE aoo_room_authority_route SET lifecycle_state='REMOVING',last_request_id=?,trace_id=?,updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND fencing_token=? AND lifecycle_state='ACTIVE' AND last_business_activity_at<=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 12 HOUR)")){p.setString(1,requestId);p.setString(2,requestId);p.setLong(3,roomId);p.setLong(4,fence);if(p.executeUpdate()!=1)throw new SecurityException("inactive room expiration lost authority race");}catch(SQLException e){throw new IllegalStateException("inactive room expiration persistence failed",e);}}

    private void finishAutomaticRemoval(long roomId,long fence){try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try(PreparedStatement route=c.prepareStatement("UPDATE aoo_room_authority_route SET lifecycle_state='REMOVED',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND fencing_token=? AND lifecycle_state='REMOVING'");PreparedStatement snapshot=c.prepareStatement("DELETE FROM aoo_room_snapshot WHERE room_id=?")){route.setLong(1,roomId);route.setLong(2,fence);if(route.executeUpdate()!=1)throw new SecurityException("等待房间解散失去权威状态");snapshot.setLong(1,roomId);snapshot.executeUpdate();c.commit();}catch(Exception failure){c.rollback();throw failure;}}catch(Exception failure){if(failure instanceof RuntimeException runtime)throw runtime;throw new IllegalStateException("等待房间可恢复状态清理失败",failure);}}

    private void finishInterruptedRemovals() {
        List<Object[]> pending=new ArrayList<>();
        try(Connection connection=dataSource.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT room_id,fencing_token,last_request_id FROM aoo_room_authority_route WHERE node_id=? AND lifecycle_state='REMOVING' ORDER BY room_id")) {
            statement.setString(1,nodeId);
            try(ResultSet result=statement.executeQuery()){while(result.next())pending.add(new Object[]{result.getLong(1),result.getLong(2),result.getString(3)});}
        } catch(SQLException failure){throw new IllegalStateException("cannot enumerate interrupted room removals",failure);}
        for(Object[] item:pending){long roomId=(long)item[0],fence=(long)item[1];String priorRequest=String.valueOf(item[2]);boolean waitingExpiration=priorRequest.startsWith("waiting-room-expiration-");boolean inactivityExpiration=priorRequest.startsWith("room-inactivity-expiration-");boolean automaticExpiration=waitingExpiration||inactivityExpiration;var snapshot=stateStore.latest(roomId).orElseThrow(()->new IllegalStateException("interrupted room removal has no durable snapshot: "+roomId));if(!automaticExpiration&&!Boolean.TRUE.equals(snapshot.authoritativeState().get("dissolved")))throw new IllegalStateException("interrupted room removal has no terminal authority state: "+roomId);Object version=snapshot.authoritativeState().get("stateVersion");String suffix=version instanceof Number number?String.valueOf(number.longValue()):"unknown";String requestId=automaticExpiration?priorRequest:"room-lifecycle-recover-"+roomId+"-"+suffix;String reason=waitingExpiration?"WAITING_ROOM_EXPIRED":inactivityExpiration?"ROOM_INACTIVITY_EXPIRED":"RECOVERED_ROOM_DISSOLUTION";hallLifecycle.close(roomId,requestId,requestId,reason);rooms.remove(roomId);if(automaticExpiration)finishAutomaticRemoval(roomId,fence);else transition(roomId,fence,"REMOVED");roomLocks.remove(roomId);}
    }

    private Map<String, Object> createLocked(Map<String, Object> command, long roomId) {
        int gameId = Math.toIntExact(positive(command, "gameId"));
        long ownerId = positive(command, "ownerId");
        String playVersion = required(command, "playVersion");
        String requestId = required(command, "requestId");
        String traceId = required(command, "traceId");
        long requestedFence = positive(command, "fencingToken");
        Route route = claim(roomId, gameId, playVersion, requestId, traceId, requestedFence);
        try {
            requireOrCreateRuntime(command, route, ownerId);
            transition(roomId, route.fencingToken(), "ACTIVE");
            return response(route);
        } catch (RuntimeException failure) {
            failRoute(roomId, route.fencingToken(), failure);
            throw failure;
        }
    }

    private void requireOrCreateRuntime(Map<String, Object> command, Route route) {
        requireOrCreateRuntime(command, route, positive(command, "ownerId"));
    }

    private void requireOrCreateRuntime(Map<String, Object> command, Route route, long ownerId) {
        RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical =
                requireCanonicalRoomLock(route);
        var provider = games.require(route.gameId(), route.playVersion());
        HallRuleProjection hallRules = loadHallRoomRuleProjection(dataSource, json, route.roomId(),
                route.gameId(), route.playVersion(), provider.descriptor().code());
        requireCanonicalMatchesHall(canonical, hallRules);
        requireCanonicalSourceHash(route.roomId(), canonical);
        boolean runtimeMissing = false;
        try {
            var existing = rooms.require(route.roomId());
            if (existing.gameId() != route.gameId() || !existing.playVersion().equals(route.playVersion())) {
                throw new SecurityException("runtime room identity conflict");
            }
            var durableIdentity = roomIdentityStore.require(route.roomId());
            if (!durableIdentity.active() || existing.runtimeIdentity() == null
                    || !existing.runtimeIdentity().equals(durableIdentity.identity())) {
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CONFLICT");
            }
            return;
        } catch (IllegalArgumentException missing) {
            runtimeMissing = true;
        }
        if (!runtimeMissing) throw new IllegalStateException("room runtime lookup failed");
        var snapshot = stateStore.latest(route.roomId());
        if (snapshot.isPresent()) {
            var durable = snapshot.orElseThrow();
            if (durable.gameId() != route.gameId()
                    || !durable.playVersion().equals(route.playVersion())
                    || durable.fencingToken() > route.fencingToken()) {
                throw new SecurityException("durable room identity conflict");
            }
            long durableStateVersion = requireStateVersion(durable.authoritativeState());
            var storedIdentity = roomIdentityStore.require(route.roomId());
            if (!storedIdentity.active()) throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROYED");
            RoomRuntimeIdentity identity = advanceIdentityIfRequired(
                    route.roomId(), storedIdentity.identity(), durableStateVersion);
            var authority = provider.restoreAuthoritativeSession(durable.authoritativeState())
                    .orElseThrow(() -> new IllegalStateException(
                            "game provider cannot restore authoritative state"));
            GameRoomHandle restored = rooms.bind(new GameRoomHandle(route.roomId(), route.gameId(),
                    route.playVersion(), authority, null, canonical.source(), identity));
            stateStore.saveCreateSnapshot(restored, route.fencingToken(),required(command,"requestId"),ownerId);
            return;
        }
        if ("PLAYING".equals(stateStore.hallState(route.roomId()))) {
            throw new IllegalStateException("playing room has no durable snapshot");
        }
        RoomRuntimeIdentityStore.Acquisition acquisition = roomIdentityStore.createOrReuse(
                route.roomId(), issueIdentity(route, 0),
                identityAuditContext(command, route));
        RoomRuntimeIdentity identity = acquisition.entry().identity();
        boolean identityCreated = acquisition.created();
        try {
        GameRoomHandle created = rooms.create(identityBoundProvider(provider, identity),
                new RoomCreationContext(route.roomId(), ownerId, hallRules.rules(), null, canonical.source()));
        var session = created.requireAuthoritativeSession();
        admit(session,ownerId,0,command.get("admission"));
        // Catalog Mahjong sessions do not implement RoomAdmissionAuthority. Seed
        // the creator through the same authoritative join command used for guests
        // before persisting the first snapshot, so Hall and game seats agree.
        if (session.authoritativeState().containsKey("mahjongAuthority")) {
            session.execute(new com.aoo.bcg.gamespi.GameCommandRequest("common.room.join_req",
                    required(command, "requestId") + ":owner", Math.max(1L, session.stateVersion() + 1L),
                    route.roomId(), 0, route.playVersion(), Long.toString(ownerId), 0, Map.of()));
        }
        RoomRuntimeIdentity advanced = advanceIdentityIfRequired(
                route.roomId(), identity, session.stateVersion());
        if (!advanced.equals(identity)) {
            identity = advanced;
            created = copyWithIdentity(created, identity);
            rooms.updateRuntime(created);
        }
        stateStore.saveCreateSnapshot(created, route.fencingToken(),required(command,"requestId"),ownerId);
        } catch (RuntimeException failure) {
            if (identityCreated) {
                closeFailedCreateRuntime(route, identity, required(command, "requestId"), failure);
            }
            throw failure;
        }
    }

    /**
     * Closes only the identity written by this create attempt. The original
     * provider/runtime failure remains the primary cause; cleanup conflicts are
     * surfaced as a machine-readable fail-closed wrapper.
     */
    private void closeFailedCreateRuntime(Route route, RoomRuntimeIdentity identity,
                                          String requestId, RuntimeException originalFailure) {
        final AuthorityDestroyResult result;
        try {
            result = destroyInternal(route.roomId(), identity.fencingToken(), identity,
                    "create-failure-tombstone-" + requestId, "CREATE_RUNTIME_FAILURE",requestId);
        } catch (RuntimeException cleanupFailure) {
            throw createFailureCleanupException(originalFailure,
                    "ROOM_RUNTIME_CREATE_TOMBSTONE_FAILED", cleanupFailure);
        }
        if (!"DESTROYED".equals(result.status())) {
            throw createFailureCleanupException(originalFailure,
                    "ROOM_RUNTIME_CREATE_TOMBSTONE_" + result.status(),
                    new IllegalStateException("ROOM_RUNTIME_CREATE_TOMBSTONE_" + result.status()));
        }
    }

    private static IllegalStateException createFailureCleanupException(
            RuntimeException originalFailure, String code, RuntimeException cleanupFailure) {
        IllegalStateException failure = new IllegalStateException(code, originalFailure);
        failure.addSuppressed(cleanupFailure);
        return failure;
    }

    private RoomRuntimeIdentity issueIdentity(Route route, long stateVersion) {
        if (stateVersion < 0) throw new IllegalArgumentException("ROOM_RUNTIME_STATE_VERSION_INVALID");
        return new RoomRuntimeIdentity(UUID.randomUUID().toString(), positiveRandom(),
                route.fencingToken(), stateVersion);
    }

    private static RoomRuntimeIdentityStore.AuditContext identityAuditContext(
            Map<String, Object> command, Route route) {
        String caller = "JdbcGatewayRoomAuthority."
                + ("RECOVERING".equals(route.state()) ? "recover" : "create")
                + ".requireOrCreateRuntime";
        return new RoomRuntimeIdentityStore.AuditContext(required(command, "requestId"), caller);
    }

    private long positiveRandom() {
        long value;
        do {
            value = identityRandom.nextLong();
        } while (value <= 0);
        return value;
    }

    private RoomRuntimeIdentity advanceIdentityIfRequired(long roomId,
                                                           RoomRuntimeIdentity current,
                                                           long stateVersion) {
        if (stateVersion < 0) throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_INVALID");
        if (stateVersion < current.stateVersion()) {
            throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_STALE");
        }
        if (stateVersion == current.stateVersion()) return current;
        RoomRuntimeIdentity replacement = new RoomRuntimeIdentity(current.roomInstanceId(),
                current.lifecycleEpoch(), current.fencingToken(), stateVersion);
        return roomIdentityStore.compareAndSet(roomId, current, replacement).identity();
    }

    private static long requireStateVersion(Map<String, Object> state) {
        Object value = state.get("stateVersion");
        if (!(value instanceof Number number) || number.longValue() < 0) {
            throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_REQUIRED");
        }
        return number.longValue();
    }

    private static GameProvider identityBoundProvider(GameProvider delegate,
                                                       RoomRuntimeIdentity identity) {
        return new GameProvider() {
            @Override public com.aoo.bcg.gamespi.GameDescriptor descriptor() {
                return delegate.descriptor();
            }

            @Override public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() {
                return context -> copyWithIdentity(delegate.roomFactory().create(context), identity);
            }

            @Override public List<com.aoo.bcg.gamespi.RuleComponent<com.aoo.bcg.gamespi.GameCommandRequest>>
            ruleComponents() {
                return delegate.ruleComponents();
            }
        };
    }

    private static GameRoomHandle copyWithIdentity(GameRoomHandle room, RoomRuntimeIdentity identity) {
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(identity, "identity");
        RoomRuntimeIdentity current = room.runtimeIdentity();
        if (current != null) {
            if (!current.sameGeneration(identity)) {
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CONFLICT");
            }
            if (identity.stateVersion() < current.stateVersion()) {
                throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_STALE");
            }
            if (identity.stateVersion() == current.stateVersion()
                    && !current.equals(identity)) {
                throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_CONFLICT");
            }
        }
        return new GameRoomHandle(room.roomId(), room.gameId(), room.playVersion(), room.legacyRoom(),
                room.compatibilityOrigin(), room.snapshotMetadataSource(), identity);
    }

    private RoomConfigurationSnapshotMetadataProvider.CanonicalProjection requireCanonicalRoomLock(Route route) {
        final RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical;
        try {
            canonical = roomMetadata.requireProjection(route.roomId());
        } catch (RuntimeException failure) {
            String reason = failure.getMessage();
            String code = reason != null && reason.startsWith("ROOM_")
                    ? reason : "ROOM_LOCK_METADATA_UNAVAILABLE";
            throw new RoomAuthorityBusinessError(409, code, code);
        }
        if (canonical.gameId() != route.gameId()
                || !canonical.playVersion().equals(route.playVersion())) {
            throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_IDENTITY_CONFLICT",
                    "ROOM_LOCK_IDENTITY_CONFLICT");
        }
        return canonical;
    }

    private static void requireCanonicalMatchesHall(
            RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical,
            HallRuleProjection hall) {
        if (canonical.source().releaseId() <= 0
                || canonical.source().indexGeneration() != hall.indexGeneration()
                || canonical.source().releaseId() != hall.releaseId()
                || !canonical.regionCode().equals(hall.regionCode())) {
            throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_INDEX_REGION_VERSION_CONFLICT",
                    "ROOM_LOCK_INDEX_REGION_VERSION_CONFLICT");
        }
    }

    private void requireCanonicalSourceHash(long roomId,
            RoomConfigurationSnapshotMetadataProvider.CanonicalProjection canonical) {
        String sql = "SELECT i.ui_schema FROM aoo_hall_room h "
                + "JOIN aoo_compiled_room_create_index i "
                + "ON i.game_id=h.game_id AND i.region_code=h.classification_region_code "
                + "AND i.play_version=h.play_version AND i.index_generation=h.index_generation "
                + "AND i.release_id=h.release_id WHERE h.room_id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, roomId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_SOURCE_HASH_UNAVAILABLE",
                            "ROOM_LOCK_SOURCE_HASH_UNAVAILABLE");
                }
                Map<String, Object> ui = json.readValue(row.getString("ui_schema"),
                        new TypeReference<Map<String, Object>>() { });
                Object rawSourceHash = ui == null ? null : ui.get("roomRuleSourceHash");
                if (!(rawSourceHash instanceof String sourceHash) || sourceHash.isBlank()) {
                    throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_SOURCE_HASH_UNAVAILABLE",
                            "ROOM_LOCK_SOURCE_HASH_UNAVAILABLE");
                }
                if (!canonical.source().sourceHash().equalsIgnoreCase(sourceHash)) {
                    throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_SOURCE_HASH_CONFLICT",
                            "ROOM_LOCK_SOURCE_HASH_CONFLICT");
                }
                if (row.next()) {
                    throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_SOURCE_HASH_AMBIGUOUS",
                            "ROOM_LOCK_SOURCE_HASH_AMBIGUOUS");
                }
            }
        } catch (RoomAuthorityBusinessError failure) {
            throw failure;
        } catch (SQLException | java.io.IOException failure) {
            throw new RoomAuthorityBusinessError(409, "ROOM_LOCK_SOURCE_HASH_UNAVAILABLE",
                    "ROOM_LOCK_SOURCE_HASH_UNAVAILABLE");
        }
    }

    /**
     * Reads the immutable, schema-validated rules persisted when Hall registered this room.
     * The active published index is deliberately not consulted: a later workbook publication
     * must not change the rules of an already-created room.
     */
    static Map<String, Object> loadHallRoomRuleSnapshot(DataSource dataSource, ObjectMapper json,
            long roomId, int expectedGameId, String expectedPlayVersion, String providerGameCode) {
        return loadHallRoomRuleProjection(dataSource, json, roomId, expectedGameId,
                expectedPlayVersion, providerGameCode).rules();
    }

    private static HallRuleProjection loadHallRoomRuleProjection(DataSource dataSource, ObjectMapper json,
            long roomId, int expectedGameId, String expectedPlayVersion, String providerGameCode) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(json, "json");
        String sql = """
                SELECT h.game_id,h.play_version,h.release_id,h.index_generation,
                       h.classification_region_code,h.rules_json,g.game_code,
                       i.component_chain AS index_component_chain,
                       i.rule_validator AS index_rule_validator,
                       i.bundle_hash AS index_bundle_hash,
                       r.rule_hash AS release_rule_hash,
                       r.component_hash AS release_component_hash,
                       r.bundle_hash AS release_bundle_hash,
                       l.component_chain AS lock_component_chain,
                       l.immutable_rules AS lock_immutable_rules,
                       l.bundle_hash AS lock_bundle_hash,
                       l.rule_hash AS lock_rule_hash,
                       l.component_hash AS lock_component_hash
                FROM aoo_hall_room h
                JOIN aoo_game_catalog g ON g.game_id=h.game_id
                JOIN aoo_compiled_room_create_index i
                  ON i.game_id=h.game_id AND i.region_code=h.classification_region_code
                 AND i.play_version=h.play_version AND i.index_generation=h.index_generation
                 AND i.release_id=h.release_id
                JOIN aoo_game_release r
                  ON r.release_id=h.release_id AND r.game_id=h.game_id
                 AND r.play_version=h.play_version AND r.status='ACTIVE'
                JOIN aoo_game_release_region rr
                  ON rr.release_id=r.release_id AND rr.region_code=h.classification_region_code
                 AND rr.status='ACTIVE'
                JOIN aoo_room_rule_lock l
                  ON l.room_id=h.room_id AND l.game_id=h.game_id
                 AND l.region_code=h.classification_region_code
                 AND l.play_version=h.play_version
                 AND l.index_generation=h.index_generation
                 AND l.release_id=h.release_id
                WHERE h.room_id=? AND i.lifecycle_state='ACTIVE'
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, roomId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new IllegalStateException("Hall room rule snapshot is unavailable");
                }
                int storedGameId = row.getInt("game_id");
                String storedPlayVersion = row.getString("play_version");
                long releaseId = row.getLong("release_id");
                long indexGeneration = row.getLong("index_generation");
                String regionCode = row.getString("classification_region_code");
                String catalogGameCode = row.getString("game_code");
                if (storedGameId != expectedGameId
                        || !Objects.equals(storedPlayVersion, expectedPlayVersion)) {
                    throw new SecurityException("Hall room rule snapshot identity mismatch");
                }
                if (releaseId <= 0 || indexGeneration <= 0) {
                    throw new IllegalStateException("Hall room rule snapshot version is invalid");
                }
                if (!Objects.equals(catalogGameCode, providerGameCode)) {
                    throw new SecurityException("Hall catalog and runtime gameCode mismatch");
                }
                Map<String, Object> rules = json.readValue(row.getString("rules_json"),
                        new TypeReference<Map<String, Object>>() { });
                if (rules == null) {
                    throw new IllegalStateException("Hall room rule snapshot is invalid");
                }
                requireCanonicalHashCrossChecks(json, row, rules);
                if (row.next()) {
                    throw new IllegalStateException("Hall room rule snapshot is ambiguous");
                }
                if (regionCode == null || regionCode.isBlank()) {
                    throw new IllegalStateException("Hall room rule snapshot region is invalid");
                }
                return new HallRuleProjection(releaseId, indexGeneration, regionCode,
                        catalogGameCode, Map.copyOf(rules));
            }
        } catch (SQLException | java.io.IOException failure) {
            throw new IllegalStateException("cannot load Hall room rule snapshot", failure);
        }
    }

    private static void requireCanonicalHashCrossChecks(ObjectMapper json, ResultSet row,
            Map<String, Object> rules) throws SQLException {
        String indexComponentChain = row.getString("index_component_chain");
        String indexRuleValidator = row.getString("index_rule_validator");
        String indexBundleHash = row.getString("index_bundle_hash");
        String releaseRuleHash = row.getString("release_rule_hash");
        String releaseComponentHash = row.getString("release_component_hash");
        String releaseBundleHash = row.getString("release_bundle_hash");
        String lockComponentChain = row.getString("lock_component_chain");
        String lockImmutableRules = row.getString("lock_immutable_rules");
        String lockBundleHash = row.getString("lock_bundle_hash");
        String lockRuleHash = row.getString("lock_rule_hash");
        String lockComponentHash = row.getString("lock_component_hash");
        if (indexComponentChain == null || indexComponentChain.isBlank()
                || indexRuleValidator == null || indexRuleValidator.isBlank()
                || lockComponentChain == null || lockComponentChain.isBlank()
                || lockImmutableRules == null || lockImmutableRules.isBlank()
                || !validHash(indexBundleHash) || !validHash(releaseRuleHash)
                || !validHash(releaseComponentHash) || !validHash(releaseBundleHash)
                || !validHash(lockBundleHash) || !validHash(lockRuleHash)
                || !validHash(lockComponentHash)) {
            throw new IllegalStateException("ROOM_LOCK_HASH_UNAVAILABLE");
        }
        if (!releaseBundleHash.equalsIgnoreCase(indexBundleHash)
                || !releaseBundleHash.equalsIgnoreCase(lockBundleHash)) {
            throw new IllegalStateException("ROOM_LOCK_BUNDLE_HASH_CONFLICT");
        }
        if (!releaseRuleHash.equalsIgnoreCase(lockRuleHash)
                || !releaseRuleHash.equalsIgnoreCase(sha256(indexRuleValidator))) {
            throw new IllegalStateException("ROOM_LOCK_RULE_HASH_CONFLICT");
        }
        if (!releaseComponentHash.equalsIgnoreCase(lockComponentHash)
                || !releaseComponentHash.equalsIgnoreCase(sha256(indexComponentChain))) {
            throw new IllegalStateException("ROOM_LOCK_COMPONENT_HASH_CONFLICT");
        }
        if (!sameJson(json, indexComponentChain, lockComponentChain)
                || !sameJson(json, rules, lockImmutableRules)) {
            throw new IllegalStateException("ROOM_LOCK_COMPONENT_OR_RULE_SNAPSHOT_CONFLICT");
        }
    }

    private static boolean validHash(String value) {
        return value != null && value.matches("^[0-9a-fA-F]{64}$");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException("ROOM_LOCK_HASH_COMPUTATION_UNAVAILABLE", failure);
        }
    }

    private static boolean sameJson(ObjectMapper json, Object left, Object right) {
        try {
            Object leftValue = left instanceof String text
                    ? json.readValue(text, Object.class) : left;
            Object rightValue = right instanceof String text
                    ? json.readValue(text, Object.class) : right;
            return Objects.equals(leftValue, rightValue);
        } catch (Exception ignored) {
            return false;
        }
    }

    private record HallRuleProjection(long releaseId, long indexGeneration, String regionCode,
                                      String gameCode, Map<String, Object> rules) {
        private HallRuleProjection {
            if (releaseId <= 0 || indexGeneration <= 0 || regionCode == null || regionCode.isBlank()
                    || gameCode == null || gameCode.isBlank() || rules == null) {
                throw new IllegalArgumentException("HALL_ROOM_RULE_PROJECTION_INVALID");
            }
        }
    }

    private static void admit(com.aoo.bcg.gamespi.AuthoritativeGameSession session,long playerId,
            int seatId,Object raw){
        if(!(session instanceof com.aoo.bcg.gamespi.RoomAdmissionAuthority authority))return;
        Map<?,?> value=raw instanceof Map<?,?> map?map:Map.of();
        Object ip=value.get("ipAddress"),latitude=value.get("latitude"),longitude=value.get("longitude");
        authority.admitParticipant(playerId,seatId,new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                ip==null?null:String.valueOf(ip),latitude instanceof Number n?n.doubleValue():null,
                longitude instanceof Number n?n.doubleValue():null));
    }

    private void recoverDurableRooms() {
        List<StartupRoom> candidates = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT a.room_id,a.game_id,g.game_code,a.play_version,a.last_request_id,a.trace_id,
                            h.owner_account_id,h.rules_json
                     FROM aoo_room_authority_route a
                     JOIN aoo_hall_room h ON h.room_id=a.room_id
                     JOIN aoo_game_catalog g ON g.game_id=a.game_id
                     WHERE h.state IN ('OPEN','PLAYING')
                       AND a.lifecycle_state IN ('CREATING','ACTIVE','RECOVERING','FAILED')
                       AND (a.node_id=? OR a.lease_expires_at<=CURRENT_TIMESTAMP(3))
                     ORDER BY a.room_id LIMIT 10000
                     """)) {
            statement.setString(1, nodeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    candidates.add(new StartupRoom(result.getLong(1), result.getInt(2),
                            result.getString(3), result.getString(4), result.getString(5),
                            result.getString(6), result.getLong(7), json.readValue(result.getString(8),
                            new TypeReference<Map<String, Object>>() { })));
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException("cannot enumerate durable rooms", failure);
        }
        List<String> failures = new ArrayList<>();
        for (StartupRoom candidate : candidates) {
            synchronized (lock(candidate.roomId())) {
                Route route = route(candidate.roomId());
                Route claimed = claimForRecovery(route);
                if (claimed == null) continue;
                Map<String, Object> command = new LinkedHashMap<>();
                command.put("roomId", candidate.roomId());
                command.put("gameId", candidate.gameId());
                command.put("playVersion", candidate.playVersion());
                command.put("requestId", candidate.requestId());
                command.put("sessionId", candidate.requestId());
                command.put("traceId", candidate.traceId());
                command.put("ownerId", candidate.ownerId());
                command.put("fencingToken", claimed.fencingToken());
                command.put("rules", candidate.rules());
                try {
                    rooms.remove(candidate.roomId());
                    requireOrCreateRuntime(command, claimed, candidate.ownerId());
                    transition(candidate.roomId(), claimed.fencingToken(), "ACTIVE");
                } catch (RuntimeException failure) {
                    failRoute(candidate.roomId(), claimed.fencingToken(), failure);
                    var failedSnapshot=stateStore.latest(candidate.roomId());
                    long stateVersion=failedSnapshot.map(snapshot ->
                            ((Number)snapshot.authoritativeState().getOrDefault("stateVersion",0)).longValue()).orElse(0L);
                    Object roundNo=failedSnapshot.map(snapshot ->
                            snapshot.authoritativeState().getOrDefault("roundNo",0)).orElse(0);
                    Object phase=failedSnapshot.map(snapshot -> {
                        Object state=snapshot.authoritativeState().get("state");
                        return state instanceof Map<?,?> map
                                ? (Boolean.TRUE.equals(map.get("finished"))?"FINISHED":"PLAYING")
                                : String.valueOf(state);
                    }).orElse("MISSING");
                    int operationCount=failedSnapshot.map(snapshot -> {
                        Object operations=snapshot.authoritativeState().get("tableOperations");
                        return operations instanceof Collection<?> values?values.size():0;
                    }).orElse(0);
                    String reason=failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());
                    String quarantineRequest="room-recovery-quarantine-"+candidate.roomId()+"-"+stateVersion;
                    try {
                        hallLifecycle.close(candidate.roomId(),quarantineRequest,candidate.traceId(),
                                "UNRECOVERABLE_AUTHORITY_SNAPSHOT");
                        rooms.remove(candidate.roomId());
                        transition(candidate.roomId(),claimed.fencingToken(),"REMOVED");
                        roomLocks.remove(candidate.roomId());
                        System.err.println("gateway room recovery quarantined roomId="+candidate.roomId()
                                +" gameCode="+candidate.gameCode()+" stateVersion="+stateVersion
                                +" roundNo="+roundNo+" phase="+phase+" operationCount="+operationCount
                                +" traceId="+candidate.traceId()
                                +" reason="+reason.replace('\n',' ').replace('\r',' '));
                    } catch (RuntimeException quarantineFailure) {
                        failure.addSuppressed(quarantineFailure);
                        failures.add(candidate.roomId()+":"+reason.replace(',',';'));
                    }
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("durable room recovery failed: " + String.join(",", failures));
        }
    }

    private Route claimForRecovery(Route expected) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            Route current = selectForUpdate(connection, expected.roomId());
            if (current == null) throw new IllegalArgumentException("authority route missing");
            if (current.gameId() != expected.gameId()
                    || !current.playVersion().equals(expected.playVersion())
                    || !current.requestId().equals(expected.requestId())) {
                throw new SecurityException("room authority recovery identity conflict");
            }
            if (!nodeId.equals(current.nodeId())
                    && current.leaseExpiresAt().isAfter(clock.instant())) {
                connection.commit();
                return null;
            }
            long fence = Math.addExact(current.fencingToken(), 1);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE aoo_room_authority_route
                    SET node_id=?,authority_endpoint=?,fencing_token=?,lifecycle_state='RECOVERING',
                        lease_expires_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND),
                        updated_at=CURRENT_TIMESTAMP(3)
                    WHERE room_id=? AND fencing_token=?
                    """)) {
                statement.setString(1, nodeId);
                statement.setString(2, endpoint);
                statement.setLong(3, fence);
                statement.setLong(4, current.roomId());
                statement.setLong(5, current.fencingToken());
                if (statement.executeUpdate() != 1) throw new SecurityException("stale fencing token");
            }
            connection.commit();
            return new Route(current.roomId(), current.gameId(), current.playVersion(), fence,
                    "RECOVERING", current.requestId(), nodeId, clock.instant().plusSeconds(90),
                    current.createdAt(),current.firstRoundStartedAt(),current.lastBusinessActivityAt());
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot claim durable room", failure);
        }
    }

    private Route claim(long roomId, int gameId, String playVersion, String requestId,
                        String traceId, long requestedFence) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            Route existing = selectForUpdate(connection, roomId);
            long fence = requestedFence;
            Instant createdAt = clock.instant();
            if (existing != null) {
                if (existing.gameId() != gameId || !existing.playVersion().equals(playVersion)) {
                    throw new SecurityException("room authority identity conflict");
                }
                if (!existing.requestId().equals(requestId)) {
                    throw new SecurityException("room authority request conflict");
                }
                if (!"FAILED".equals(existing.state())) {
                    connection.commit();
                    return existing;
                }
                createdAt = existing.createdAt();
                fence = Math.max(requestedFence, Math.addExact(existing.fencingToken(), 1));
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO aoo_room_authority_route(
                      room_id,game_id,play_version,node_id,authority_endpoint,fencing_token,
                      lifecycle_state,last_request_id,trace_id,lease_expires_at,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,'CREATING',?,?,DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND),
                      CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
                    ON DUPLICATE KEY UPDATE
                      node_id=VALUES(node_id),authority_endpoint=VALUES(authority_endpoint),
                      fencing_token=VALUES(fencing_token),lifecycle_state='CREATING',
                      last_request_id=VALUES(last_request_id),trace_id=VALUES(trace_id),
                      lease_expires_at=VALUES(lease_expires_at),updated_at=CURRENT_TIMESTAMP(3)
                    """)) {
                int index = 1;
                statement.setLong(index++, roomId);
                statement.setInt(index++, gameId);
                statement.setString(index++, playVersion);
                statement.setString(index++, nodeId);
                statement.setString(index++, endpoint);
                statement.setLong(index++, fence);
                statement.setString(index++, requestId);
                statement.setString(index, traceId);
                statement.executeUpdate();
            }
            connection.commit();
            return new Route(roomId, gameId, playVersion, fence, "CREATING", requestId,
                    nodeId, clock.instant().plusSeconds(90), createdAt,
                    existing==null?null:existing.firstRoundStartedAt(),
                    existing==null?clock.instant():existing.lastBusinessActivityAt());
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("authority route persistence failed", failure);
        }
    }

    private Route route(long roomId) {
        stateStore.carryPort().requireAvailable(roomId);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT game_id,play_version,fencing_token,lifecycle_state,last_request_id,
                            node_id,lease_expires_at,created_at,first_round_started_at,last_business_activity_at
                     FROM aoo_room_authority_route WHERE room_id=?
                     """)) {
            statement.setLong(1, roomId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("authority route missing");
                return readRoute(roomId, result);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("authority route lookup failed", failure);
        }
    }

    private Route selectForUpdate(Connection connection, long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT game_id,play_version,fencing_token,lifecycle_state,last_request_id,
                       node_id,lease_expires_at,created_at,first_round_started_at,last_business_activity_at
                FROM aoo_room_authority_route WHERE room_id=? FOR UPDATE
                """)) {
            statement.setLong(1, roomId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readRoute(roomId, result) : null;
            }
        }
    }

    private static Route readRoute(long roomId, ResultSet result) throws SQLException {
        java.sql.Timestamp firstRoundStartedAt=result.getTimestamp(9);
        return new Route(roomId, result.getInt(1), result.getString(2), result.getLong(3),
                result.getString(4), result.getString(5), result.getString(6),
                result.getTimestamp(7).toInstant(),result.getTimestamp(8).toInstant(),
                firstRoundStartedAt==null?null:firstRoundStartedAt.toInstant(),result.getTimestamp(10).toInstant());
    }

    private void transition(long roomId, long fence, String state) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE aoo_room_authority_route
                     SET lifecycle_state=?,updated_at=CURRENT_TIMESTAMP(3),
                         lease_expires_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND)
                     WHERE room_id=? AND fencing_token=?
                     """)) {
            statement.setString(1, state);
            statement.setLong(2, roomId);
            statement.setLong(3, fence);
            if (statement.executeUpdate() != 1) throw new SecurityException("stale fencing token");
        } catch (SQLException failure) {
            throw new IllegalStateException("authority state persistence failed", failure);
        }
    }

    private void failRoute(long roomId, long fence, RuntimeException original) {
        try {
            if ("REMOVED".equals(route(roomId).state())) return;
            transition(roomId, fence, "FAILED");
        } catch (RuntimeException persistenceFailure) {
            original.addSuppressed(persistenceFailure);
        }
    }

    private void renewActiveLeases() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE aoo_room_authority_route
                     SET lease_expires_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND),
                         updated_at=CURRENT_TIMESTAMP(3)
                     WHERE node_id=? AND lifecycle_state='ACTIVE'
                     """)) {
            statement.setString(1, nodeId);
            statement.executeUpdate();
        } catch (SQLException failure) {
            System.err.println("room-authority lease renewal failed: " + failure.getSQLState());
        }
    }

    @Override public void close(){leaseKeeper.shutdownNow();lifecycleWorker.shutdownNow();roomLocks.clear();reconnectReceipts.clear();handoffReceipts.clear();handoffLeases.clear();}

    private void verifyIdentity(Route route, Map<String, Object> command) {
        if (route.gameId() != Math.toIntExact(positive(command, "gameId"))
                || !route.playVersion().equals(required(command, "playVersion"))
                || !route.requestId().equals(required(command, "requestId"))) {
            throw new SecurityException("room authority recovery identity conflict");
        }
    }

    void presence(long roomId,long accountId,boolean online){
        synchronized(lock(roomId)){
            var room=rooms.require(roomId);var session=room.requireAuthoritativeSession();
            if(!(session instanceof com.aoo.bcg.gamespi.ParticipantPresenceAuthority presence))return;
            var checkpoint=beginCarry(room);
            try{presence.participantPresence(accountId,online,clock.instant());
                if(checkpoint!=null){var before=room.runtimeIdentity();var next=new RoomRuntimeIdentity(before.roomInstanceId(),before.lifecycleEpoch(),before.fencingToken(),session.stateVersion());var updated=copyWithIdentity(room,next);stateStore.saveCarrySnapshot(updated,before);rooms.updateRuntime(updated);}
                stateStore.carryPort().finish(checkpoint,true);
            }catch(RuntimeException|Error failure){if(checkpoint!=null){boolean restored=false;try{restoreCarryDurable(room,checkpoint);restored=true;}catch(RuntimeException restoreFailure){failure.addSuppressed(restoreFailure);}stateStore.carryPort().finish(checkpoint,restored);}throw failure;}
        }
    }

    private JdbcCarryParticipationPort.Checkpoint beginCarry(GameRoomHandle room){return room.requireAuthoritativeSession() instanceof com.aoo.bcg.gamespi.CarryBillingEffectSource?stateStore.carryPort().begin(room,stateStore.latest(room.roomId()).orElse(null)):null;}

    private void restoreCarryDurable(GameRoomHandle old,JdbcCarryParticipationPort.Checkpoint checkpoint) {
        if(checkpoint==null||checkpoint.finished||checkpoint.roomId()!=old.roomId())
            throw new IllegalStateException("CARRY_RECOVERY_CHECKPOINT_CONFLICT");
        final RoomRuntimeIdentity durable;
        final com.aoo.bcg.gamespi.AuthoritativeGameSession restored;
        try(Connection connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try(PreparedStatement query=connection.prepareStatement("SELECT i.room_instance_id,i.lifecycle_epoch,i.fencing_token,i.state_version,i.lifecycle_state,s.room_id,s.game_id,s.play_version,s.fencing_token,s.state_version,s.last_event_sequence,s.state_payload FROM aoo_room_runtime_identity_authority i JOIN aoo_room_snapshot s ON s.room_id=i.room_id WHERE i.room_id=? FOR UPDATE")) {
                query.setLong(1,old.roomId());
                try(ResultSet row=query.executeQuery()) {
                    if(!row.next())throw new IllegalStateException("CARRY_DURABLE_RECOVERY_REQUIRED");
                    durable=new RoomRuntimeIdentity(row.getString(1),row.getLong(2),row.getLong(3),row.getLong(4));
                    if(!"ACTIVE".equals(row.getString(5))||!checkpoint.identity().sameGeneration(durable)
                            ||row.getLong(6)!=old.roomId()||row.getInt(7)!=old.gameId()||!old.playVersion().equals(row.getString(8))
                            ||row.getLong(9)!=durable.fencingToken()||row.getLong(10)!=durable.stateVersion()||row.getLong(11)!=durable.stateVersion())
                        throw new IllegalStateException("CARRY_RECOVERY_IDENTITY_CONFLICT");
                    Map<String,Object> state=json.readValue(row.getString(12),new TypeReference<Map<String,Object>>(){});
                    restored=games.require(old.gameId(),old.playVersion()).restoreAuthoritativeSession(state)
                            .orElseThrow(()->new IllegalStateException("CARRY_NATIVE_RESTORE_REQUIRED"));
                    if(restored.stateVersion()!=durable.stateVersion())throw new IllegalStateException("CARRY_NATIVE_RESTORE_VERSION_CONFLICT");
                }
                connection.commit();
            } catch(Exception failure) {connection.rollback();throw failure;}
        } catch(Exception failure) {
            if(failure instanceof RuntimeException runtime)throw runtime;
            throw new IllegalStateException("CARRY_DURABLE_RECOVERY_READ_FAILED",failure);
        }
        rooms.restoreRuntimeFromDurable(old,new GameRoomHandle(old.roomId(),old.gameId(),old.playVersion(),restored,
                old.compatibilityOrigin(),old.snapshotMetadataSource(),durable));
    }

    private Object lock(long roomId) {
        stateStore.carryPort().requireAvailable(roomId);
        return roomLocks.computeIfAbsent(roomId, ignored -> new Object());
    }

    private void requireNoRequestScopeConflict(IdempotencyKey expected) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement("""
                     SELECT user_id,operation,round_no
                     FROM aoo_business_idempotency
                     WHERE room_id=? AND client_request_id=?
                     """)) {
            query.setLong(1, expected.roomId());
            query.setString(2, expected.requestId());
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    if (!expected.userId().equals(rows.getString(1))
                            || !expected.operation().equals(rows.getString(2))
                            || expected.roundNo() != rows.getInt(3)) {
                        throw new SecurityException("ROOM_ORCHESTRATOR_EXECUTE_REQUEST_SCOPE_CONFLICT");
                    }
                }
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_REQUEST_SCOPE_CHECK_FAILED", failure);
        }
    }

    private Map<String, Object> response(Route route) {
        return Map.of(
                "roomId", route.roomId(),
                "gameId", route.gameId(),
                "playVersion", route.playVersion(),
                "nodeId", nodeId,
                "route", endpoint,
                "fencingToken", route.fencingToken(),
                "state", "ACTIVE");
    }

    private static long positive(Map<String, Object> command, String key) {
        return ProtocolValuePolicy.parseCanonicalLong(command.get(key), key, true);
    }

    private static long nonNegative(Map<String, Object> command, String key) {
        return ProtocolValuePolicy.parseCanonicalLong(command.get(key), key, false);
    }

    private static String required(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return text;
    }

    private static String required(String value, String key) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private record Route(long roomId, int gameId, String playVersion, long fencingToken,
                         String state, String requestId, String nodeId, Instant leaseExpiresAt,
                         Instant createdAt,Instant firstRoundStartedAt,Instant lastBusinessActivityAt) { }
    private record StartupRoom(long roomId, int gameId, String gameCode, String playVersion, String requestId,
                               String traceId, long ownerId, Map<String, Object> rules) {
        private StartupRoom { rules = Map.copyOf(rules); }
    }

    record HandoffLease(RoomOrchestrator.RoomIdentity identity, long ownerId,
                        String sessionId, long connectionGeneration) {
        HandoffLease {
            Objects.requireNonNull(identity, "identity");
            if (ownerId <= 0 || sessionId == null || sessionId.isBlank() || connectionGeneration <= 0) {
                throw new IllegalArgumentException("ROOM_ORCHESTRATOR_HANDOFF_LEASE_INVALID");
            }
        }
    }

    private record HandoffReceipt(RoomOrchestrator.HandoffCommand command,
                                  RoomOrchestrator.HandoffResult result) {
        private HandoffReceipt {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(result, "result");
        }
    }
}
