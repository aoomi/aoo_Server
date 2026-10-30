package com.aoo.bcg.bootstrap;

import com.aoo.bcg.config.RoomConfigurationLeaseStore;
import com.aoo.bcg.config.RoomConfigurationSnapshotMetadataProvider;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import com.aoo.bcg.common.authority.JdbcRoomDissolveAuthorityStore;
import com.aoo.bcg.common.id.DistributedIdGenerator;
import com.aoo.bcg.common.idempotency.JdbcIdempotencyStore;
import com.aoo.bcg.common.settlement.JdbcSettlementRepository;
import com.aoo.bcg.common.settlement.SettlementExecutor;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gateway.ConnectionSessionFactory;
import com.aoo.bcg.gateway.GatewayRuntimeProvider;
import com.aoo.bcg.gateway.GatewayWebSocketFrameHandler;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.gateway.UnifiedGameRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.aoo.bcg.gateway.GatewayRoomBroadcastHub;
import com.aoo.bcg.gateway.JdbcConnectionGenerationStore;
import java.net.URI;

/** Production SPI assembly used by the standalone Gateway process. */
public final class ProductionGatewayRuntimeProvider implements GatewayRuntimeProvider {
    @Override
    public Runtime create(DataSource source, ObjectMapper json, Clock clock) {
        GameRegistry games = loadGames(new JdbcParticipantProfileSource(source));
        var runtimeMetadataProjection=games.requireRuntimeMetadataProjection();

        long nodeId = Long.parseLong(System.getenv().getOrDefault("AOO_GATEWAY_NODE_ID", "1"));
        var ids = new DistributedIdGenerator(nodeId, clock);
        var idempotency = new JdbcIdempotencyStore<GameCommandResult>(
                source, json, GameCommandResult.class, clock);
        var settlements = new SettlementExecutor(
                new JdbcSettlementRepository(source, json, ids, clock));
        var durableSettlements = new DurableGameSettlementService(games, settlements);
        var rooms = RuntimeGameRoomRegistry.configureGlobal(runtimeMetadataProjection);
        Duration idempotencyRetention = Duration.ofHours(24);
        var commandCommitter = new JdbcGatewayGameCommandCommitter(source, json, clock,
                idempotencyRetention, durableSettlements);
        commandCommitter.carryPort().configureCharges(ids,clock);
        commandCommitter.recoverPendingSettlements(256);
        commandCommitter.recoverCompletedClubMatches(256);
        ScheduledExecutorService recovery = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "settlement-outbox-recovery");thread.setDaemon(true);return thread;
        });
        Runnable recoverSettlements = () -> {
            try { commandCommitter.recoverPendingSettlements(64); }
            catch (RuntimeException failure) { System.err.println("settlement outbox recovery failed: "+failure.getMessage()); }
            try { commandCommitter.recoverCompletedClubMatches(64); }
            catch (RuntimeException failure) { System.err.println("club match settlement recovery failed: "+failure.getMessage()); }
        };
        commandCommitter.onSettlementQueued(() -> recovery.execute(recoverSettlements));
        recovery.scheduleWithFixedDelay(() -> {
            recoverSettlements.run();
        },5,5,TimeUnit.SECONDS);
        var connectionSessions = new ConnectionSessionFactory(new JdbcConnectionGenerationStore(source));
        GatewayWebSocketFrameHandler.SessionResolver sessions = (identity, frame) -> commandCommitter.carryPort().read(Long.parseLong(frame.roomId()),()-> {
            var room = rooms.require(Long.parseLong(frame.roomId()));
            Map<String,Object> authority = room.requireAuthoritativeSession().authoritativeState();
            Integer seated = authoritativeSeat(authority,identity.userId());
            Object rawObservers = authority.get("observers");
            Integer observing = rawObservers instanceof Map<?,?> observers ? observers.entrySet().stream()
                    .filter(entry -> Long.parseLong(String.valueOf(entry.getValue())) == identity.userId())
                    .map(entry -> Integer.parseInt(String.valueOf(entry.getKey())))
                    .findFirst().orElse(null) : null;
            Integer hallSeat=joinedHallSeat(source,Long.parseLong(frame.roomId()),identity.userId());
            int seatId=seated!=null?seated:observing!=null?observing:hallSeat!=null?hallSeat:
                    throwSecurity("account is not a room member");
            return new GatewayWebSocketFrameHandler.SessionBinding(identity.userId(),
                    connectionSessions.open(Long.toString(identity.userId()), frame.roomId(), seatId,
                            frame.playVersion(), frame.seq() - 1));
        });
        var broadcasts = new GatewayRoomBroadcastHub((roomId, playerId) -> commandCommitter.carryPort().read(roomId,()-> {
            var room = rooms.require(roomId);
            var session = room.requireAuthoritativeSession();
            var descriptor = games.require(room.gameId(),room.playVersion()).descriptor();
            Map<String,Object> body = commandCommitter.decorateReplayCode(session.viewFor(playerId));
            return new GatewayRoomBroadcastHub.Perspective(room.playVersion(),
                    GatewayRoomBroadcastHub.withCommonRoom(body,session,playerId,
                            requiresCommonRoomPublicView(descriptor.code(),descriptor.family())));
        }),json,clock);
        var hallLifecycle = new HallRoomLifecycleClient(URI.create(environment("HALL_INTERNAL_URL","GATEWAY_HALL_URL")),environment("HALL_INTERNAL_TOKEN"),json);
        var roomMetadata = new RoomConfigurationSnapshotMetadataProvider(
                new RoomConfigurationLeaseStore(source, json));
        var roomIdentityStore = new RoomRuntimeIdentityStore(source);
        var dissolveAuthority = new JdbcRoomDissolveAuthorityStore(source, json, clock);
        var authority = new JdbcGatewayRoomAuthority(source,games,rooms,
                clock,System.getenv().getOrDefault("AOO_GATEWAY_NODE_ID","gateway-1"),
                System.getenv().getOrDefault("AOO_GATEWAY_PUBLIC_ENDPOINT",
                        "ws://127.0.0.1:8080/api/v2/gateway/ws"),json,commandCommitter,broadcasts,
                hallLifecycle, idempotencyRetention, roomMetadata, roomIdentityStore,
                dissolveAuthority);
        broadcasts.configureLifecycleCompletion(authority::completeTerminal);
        broadcasts.configureMembershipCompletion(authority::completeMemberExit);
        broadcasts.configurePresenceSink((roomId,accountId,online) -> {
            com.aoo.bcg.gateway.RoomOrderedExecutor.global().execute(roomId,()->{
                authority.presence(roomId,accountId,online);
                return null;
            });
        });
        var runtime = new UnifiedGameRuntime(games, rooms, idempotency, settlements, clock,
                Duration.ofSeconds(30), idempotencyRetention, commandCommitter);
        runtime.router().configureRoomAvailability(commandCommitter.carryPort()::requireAvailable,(roomId,work)->commandCommitter.carryPort().read(roomId,work));
        runtime.router().configureReconnectAuthority(authority::reconnectThroughRoomOrchestrator);
        runtime.router().configureRoomCommandAuthority(authority::routeTypedRoomCommand);
        runtime.router().configureReadyAuthority(authority::readyGameplayThroughRoomOrchestrator);
        runtime.router().configureExecuteAuthority(authority::executeGameplayThroughRoomOrchestrator);
        GatewayWebSocketFrameHandler.HandoffOwnerRegistrar handoffOwnerRegistrar = (identity, session) -> {
            final long ownerId;
            try {
                ownerId = Long.parseLong(session.userId());
            } catch (NumberFormatException invalidAccount) {
                throw new IllegalStateException(
                        "ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_ACCOUNT_INVALID", invalidAccount);
            }
            var room = rooms.require(identity.roomId());
            Object rawOwner = room.requireAuthoritativeSession().authoritativeState().get("ownerId");
            if (!(rawOwner instanceof Number owner) || owner.longValue() <= 0) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REQUIRED");
            }
            // Observers and joined non-owner seats remain authenticated room
            // sockets, but only the authoritative owner owns the handoff lease.
            if (owner.longValue() != ownerId) return;
            var runtimeIdentity = room.runtimeIdentity();
            if (runtimeIdentity == null) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_RUNTIME_IDENTITY_REQUIRED");
            }
            var descriptor = games.require(room.gameId(), room.playVersion()).descriptor();
            var expectedIdentity = new com.aoo.bcg.gamespi.RoomOrchestrator.RoomIdentity(
                    room.roomId(), runtimeIdentity.roomInstanceId(), room.gameId(), descriptor.code(),
                    room.playVersion(), runtimeIdentity.lifecycleEpoch(), runtimeIdentity.fencingToken(),
                    runtimeIdentity.stateVersion());
            authority.registerHandoffOwner(expectedIdentity, ownerId,
                    session.connectionId(), session.generation());
        };
        GatewayRuntimeProvider.RoomAuthority managedAuthority = new GatewayRuntimeProvider.RoomAuthority() {
            @Override public Map<String,Object> create(Map<String,Object> command) {
                return addGameCode(games, command, authority.create(command));
            }
            @Override public Map<String,Object> recover(Map<String,Object> command) {
                return addGameCode(games, command, authority.recover(command));
            }
            @Override public Map<String,Object> destroy(Map<String,Object> command) {
                return destroyThroughOrchestrator(authority, command);
            }
            @Override public Map<String,Object> join(Map<String,Object> command) { return authority.join(command); }
            @Override public Map<String,Object> leave(Map<String,Object> command) {
                return authority.leaveThroughRoomOrchestrator(command);
            }
            @Override public void remove(long roomId,long fencingToken) {
                throw new IllegalStateException("ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED");
            }
            @Override public GatewayRuntimeProvider.Readiness readiness() { return authority.readiness(); }
            @Override public void close() { recovery.shutdownNow();authority.close(); }
        };
        class DirectHallAuthority implements com.aoo.bcg.lobby.LobbyRoomCreateSaga.AuthorityPort,
                com.aoo.bcg.lobby.LobbyMembershipAuthorityPort {
            private Map<String,Object> body(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command,long fence){
                var value=new java.util.LinkedHashMap<String,Object>();value.put("roomId",command.roomId());
                value.put("requestId",command.requestId());value.put("traceId",command.traceId());
                value.put("sessionId",command.requestId());
                value.put("ownerId",command.accountId());value.put("gameId",command.gameId());
                value.put("playVersion",command.playVersion());value.put("fencingToken",fence);
                value.put("rules",command.rules());value.put("scope",command.scope().asMap());
                value.put("admission",command.admission());return value;
            }
            public Map<String,Object> create(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command){return withGameCode(command,managedAuthority.create(body(command,command.stateVersion())));}
            public Map<String,Object> recover(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command){return withGameCode(command,managedAuthority.recover(body(command,command.stateVersion())));}
            public Map<String,Object> destroy(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command,
                                               Map<String,Object> expectedLease){
                Map<String,Object> value = body(command, command.stateVersion());
                value.put("reason", "HALL_CREATE_COMPENSATION");
                value.put("sessionId", command.requestId());
                copyExpectedIdentity(value, expectedLease, command);
                return requireDestroySuccess(managedAuthority.destroy(value));
            }
            public void remove(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command,long fence){
                throw new IllegalStateException("ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED");
            }
            private Map<String,Object> withGameCode(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command,
                                                    Map<String,Object> response){
                if (response == null) throw new IllegalStateException("ROOM_AUTHORITY_RESULT_REQUIRED");
                Map<String,Object> value = new java.util.LinkedHashMap<>(response);
                value.put("gameCode", games.require(command.gameId(), command.playVersion()).descriptor().code());
                return Map.copyOf(value);
            }
            public Map<String,Object> join(long roomId,long accountId,int seatNo,String playVersion,String requestId,String traceId,Map<String,Object> admission){
                return managedAuthority.join(Map.of("roomId",roomId,"accountId",accountId,"seatNo",seatNo,
                        "playVersion",playVersion,"requestId",requestId,"traceId",traceId,"admission",admission));
            }
            public Map<String,Object> leave(long roomId,long accountId,String playVersion,String requestId,String traceId,Map<String,Object> completeIdentity){
                if(completeIdentity==null||completeIdentity.isEmpty())throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED");
                Map<String,Object> value=new java.util.LinkedHashMap<>(completeIdentity);
                value.put("roomId",roomId);value.put("accountId",accountId);value.put("playVersion",playVersion);
                value.put("requestId",requestId);value.put("idempotencyKey",requestId);value.put("traceId",traceId);
                return managedAuthority.leave(Map.copyOf(value));
            }
        }
        var directAuthority=new DirectHallAuthority();
        var hallRepository=new com.aoo.bcg.lobby.JdbcLobbyRepository(source,json,clock,directAuthority,runtimeMetadataProjection);
        var sagaStore=new com.aoo.bcg.lobby.LobbyRoomCreateSagaStore(source,json);
        var sagaBilling=BootstrapAPP.roomBillingLifecycle(source,new DistributedIdGenerator(39,clock),clock);
        var roomCreate=new com.aoo.bcg.lobby.LobbyRoomCreateSaga(sagaStore,sagaBilling,
                new com.aoo.bcg.lobby.LobbyRoomCreateSaga.RoomPort(){
                    public void register(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command){hallRepository.registerRoom(command);}
                    public Map<String,Object> confirm(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command,long version){return hallRepository.confirmRoom(command.accountId(),command.roomId());}
                    public com.aoo.bcg.lobby.LobbyRoomCreateSaga.RoomConfirmation confirmCompletion(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command,long version){return hallRepository.confirmRoomCompletion(command,version);}
                    public void remove(com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command){hallRepository.compensateCreate(command.accountId(),command.requestId(),command.roomId());}
                },hallRepository::normalizeCreateCommand,directAuthority);
        var clubCreateAction=CanonicalPricingPolicy.load().requireClubCreate();
        var clubCreatePolicy=new com.aoo.bcg.club.ClubCreatePolicy(clubCreateAction.currency(),
                clubCreateAction.eligibilityEnabled(),clubCreateAction.minimumRoomCardBalance(),
                clubCreateAction.chargeEnabled(),clubCreateAction.chargeRoomCardAmount(),
                clubCreateAction.policyVersion(),clubCreateAction.contentHash());
        return new Runtime(runtime.router(), sessions, broadcasts,
                new JdbcHallWebSocketDispatcher(source,json,clock,hallRepository,roomCreate,clubCreatePolicy),
                handoffOwnerRegistrar, managedAuthority);
    }

    private static Map<String,Object> destroyThroughOrchestrator(
            JdbcGatewayRoomAuthority authority, Map<String,Object> command) {
        RoomOrchestrator.DestroyCommand typed = destroyCommand(command);
        try {
            RoomOrchestrator.DestroyResult result = authority.destroyThroughRoomOrchestrator(typed);
            return destroyResponse(result);
        } catch (IllegalStateException failure) {
            String status = destroyStatus(failure.getMessage());
            if (status == null) throw failure;
            return Map.of("status", status, "destroyed", false, "reason", status);
        }
    }

    private static Map<String,Object> requireDestroySuccess(Map<String,Object> result) {
        if (result == null) throw new IllegalStateException("ROOM_AUTHORITY_DESTROY_RESULT_REQUIRED");
        String status = String.valueOf(result.get("status"));
        if ("DESTROYED".equals(status) || "ALREADY_DESTROYED".equals(status)) return result;
        throw new IllegalStateException("ROOM_AUTHORITY_DESTROY_" + status);
    }

    private static Map<String,Object> addGameCode(GameRegistry games, Map<String,Object> command,
                                                  Map<String,Object> response) {
        if (response == null) throw new IllegalStateException("ROOM_AUTHORITY_RESULT_REQUIRED");
        Map<String,Object> value = new java.util.LinkedHashMap<>(response);
        value.put("gameCode", games.require(Math.toIntExact(number(command, "gameId")),
                text(command, "playVersion")).descriptor().code());
        return Map.copyOf(value);
    }

    private static RoomOrchestrator.DestroyCommand destroyCommand(Map<String,Object> command) {
        if (command == null) throw new IllegalArgumentException("destroy command required");
        long roomId = number(command, "roomId");
        int gameId = Math.toIntExact(number(command, "gameId"));
        String gameCode = text(command, "gameCode");
        String playVersion = text(command, "playVersion");
        long lifecycleEpoch = positiveDestroy(command, "lifecycleEpoch");
        long fencingToken = positiveDestroy(command, "fencingToken");
        long stateVersion = nonNegativeDestroy(command, "stateVersion");
        RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(roomId,
                text(command, "roomInstanceId"), gameId, gameCode, playVersion,
                lifecycleEpoch, fencingToken, stateVersion);
        return new RoomOrchestrator.DestroyCommand(identity, text(command, "sessionId"),
                text(command, "requestId"), text(command, "reason"));
    }

    private static Map<String,Object> destroyResponse(RoomOrchestrator.DestroyResult result) {
        if (result == null || result.identity() == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED");
        }
        RoomOrchestrator.RoomIdentity identity = result.identity();
        Map<String,Object> response = new java.util.LinkedHashMap<>();
        response.put("status", result.destroyed() ? "DESTROYED" : result.reason());
        response.put("destroyed", result.destroyed());
        response.put("reason", result.reason());
        response.put("roomId", identity.roomId());
        response.put("gameId", identity.gameId());
        response.put("gameCode", identity.gameCode());
        response.put("playVersion", identity.playVersion());
        response.put("roomInstanceId", identity.roomInstanceId());
        response.put("lifecycleEpoch", identity.lifecycleEpoch());
        response.put("fencingToken", identity.fencingToken());
        response.put("stateVersion", identity.stateVersion());
        return Map.copyOf(response);
    }

    private static String destroyStatus(String message) {
        if (message == null) return null;
        if (message.contains("DESTROY_CAS_CONFLICT")) return "CAS_CONFLICT";
        if (message.contains("DESTROY_IDENTITY_REQUIRED")
                || message.contains("DESTROY_NOT_FOUND")) return "NOT_FOUND";
        if (message.contains("DESTROY_ALREADY_DESTROYED")) return "ALREADY_DESTROYED";
        return null;
    }

    private static void copyExpectedIdentity(Map<String,Object> value,
                                             Map<String,Object> expectedLease,
                                             com.aoo.bcg.lobby.LobbyRoomCreateSaga.Command command) {
        if (expectedLease == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED");
        }
        if (number(expectedLease, "roomId") != command.roomId()
                || number(expectedLease, "gameId") != command.gameId()
                || !command.playVersion().equals(text(expectedLease, "playVersion"))) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_CONFLICT");
        }
        long expectedFence = positiveDestroy(expectedLease, "fencingToken");
        if (expectedFence != command.stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_CONFLICT");
        }
        value.put("gameCode", text(expectedLease, "gameCode"));
        value.put("roomInstanceId", text(expectedLease, "roomInstanceId"));
        value.put("lifecycleEpoch", positiveDestroy(expectedLease, "lifecycleEpoch"));
        value.put("fencingToken", expectedFence);
        value.put("stateVersion", nonNegativeDestroy(expectedLease, "stateVersion"));
    }

    private static long positiveDestroy(Map<String,Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof Number number) || number.longValue() <= 0) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_DESTROY_" + key.toUpperCase()
                    + "_REQUIRED");
        }
        return number.longValue();
    }

    private static long nonNegativeDestroy(Map<String,Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof Number number) || number.longValue() < 0) {
            throw new IllegalArgumentException("ROOM_ORCHESTRATOR_DESTROY_" + key.toUpperCase()
                    + "_REQUIRED");
        }
        return number.longValue();
    }

    static GameRegistry loadGames() {
        return loadGames(null);
    }
    static boolean requiresCommonRoomPublicView(String gameCode,String family) {
        String code=gameCode==null?"":gameCode.trim();
        if(java.util.Set.of("chengdu2999","quanguo2998","quanguo2997","chengdu1001").contains(code))return true;
        // CommonRoom is a family contract. Any catalog-routed Mahjong or PDK
        // profile must publish the same public seat projection; individual
        // profile codes must never decide whether Head/chat/voice are mounted.
        return family != null && (family.startsWith("mahjong:") || "poker:pao-de-kuai".equals(family));
    }
    private static GameRegistry loadGames(com.aoo.bcg.gamespi.ParticipantProfileSource profileSource) {
        return ProviderCatalogAssembler.production(profileSource);
    }
    /**
     * Framework snapshots historically expose {@code players: seat -> playerId}; newer
     * poker aggregates expose {@code seats: seat -> {playerId,...}}. Both are stable SPI
     * shapes, so connection authentication must resolve either without game-specific code.
     */
    static Integer authoritativeSeat(Map<String,Object> authority,long playerId) {
        Object rawPlayers=authority.get("players");
        if(rawPlayers instanceof Map<?,?> players){
            Integer seat=players.entrySet().stream()
                    .filter(entry -> playerIdValue(entry.getValue()) == playerId)
                    .map(entry -> Integer.parseInt(String.valueOf(entry.getKey())))
                    .findFirst().orElse(null);
            if(seat!=null)return seat;
        }
        Object rawSeats=authority.get("seats");
        if(rawSeats instanceof Map<?,?> seats)return seats.entrySet().stream()
                .filter(entry -> playerIdValue(entry.getValue()) == playerId)
                .map(entry -> Integer.parseInt(String.valueOf(entry.getKey())))
                .findFirst().orElse(null);
        return null;
    }
    private static long playerIdValue(Object value) {
        Object raw=value instanceof Map<?,?> seat?seat.get("playerId"):value;
        if(raw instanceof Number number)return number.longValue();
        try{return Long.parseLong(String.valueOf(raw));}catch(RuntimeException ignored){return Long.MIN_VALUE;}
    }
    private static Integer joinedHallSeat(DataSource source,long roomId,long accountId) {
        String sql="SELECT m.seat_no FROM aoo_hall_room_member m JOIN aoo_hall_room r ON r.room_id=m.room_id "
                +"JOIN aoo_room_authority_route a ON a.room_id=m.room_id "
                +"WHERE m.room_id=? AND m.account_id=? AND m.status='JOINED' "
                +"AND r.state IN('OPEN','PLAYING') AND a.lifecycle_state='ACTIVE' LIMIT 1";
        try(var connection=source.getConnection();var query=connection.prepareStatement(sql)){
            query.setLong(1,roomId);query.setLong(2,accountId);
            try(var result=query.executeQuery()){return result.next()?result.getInt(1):null;}
        }catch(java.sql.SQLException failure){throw new IllegalStateException("spectator membership lookup failed",failure);}
    }
    private static int throwSecurity(String message){throw new SecurityException(message);}
    private static long number(Map<String,Object> v,String k){Object x=v.get(k);if(!(x instanceof Number n)||n.longValue()<=0)throw new IllegalArgumentException(k+" must be positive");return n.longValue();}
    private static String text(Map<String,Object> v,String k){Object x=v.get(k);if(!(x instanceof String s)||s.isBlank())throw new IllegalArgumentException(k+" is required");return s;}
    private static String environment(String... keys){for(String key:keys){String value=System.getenv(key);if(value!=null&&!value.isBlank())return value.strip();}throw new IllegalStateException(String.join(" or ",keys)+" is required");}
}
