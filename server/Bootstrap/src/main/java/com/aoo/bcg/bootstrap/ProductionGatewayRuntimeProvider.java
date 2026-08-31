package com.aoo.bcg.bootstrap;

import com.aoo.bcg.common.id.DistributedIdGenerator;
import com.aoo.bcg.common.idempotency.JdbcIdempotencyStore;
import com.aoo.bcg.common.settlement.JdbcSettlementRepository;
import com.aoo.bcg.common.settlement.SettlementExecutor;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gateway.ConnectionSession;
import com.aoo.bcg.gateway.GatewayRuntimeProvider;
import com.aoo.bcg.gateway.GatewayWebSocketFrameHandler;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.gateway.UnifiedGameRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.aoo.bcg.gateway.GatewayRoomBroadcastHub;
import java.net.URI;

/** Production SPI assembly used by the standalone Gateway process. */
public final class ProductionGatewayRuntimeProvider implements GatewayRuntimeProvider {
    @Override
    public Runtime create(DataSource source, ObjectMapper json, Clock clock) {
        GameRegistry games = loadGames();
        
        long nodeId = Long.parseLong(System.getenv().getOrDefault("AOO_GATEWAY_NODE_ID", "1"));
        var ids = new DistributedIdGenerator(nodeId, clock);
        var idempotency = new JdbcIdempotencyStore<GameCommandResult>(
                source, json, GameCommandResult.class, clock);
        var settlements = new SettlementExecutor(
                new JdbcSettlementRepository(source, json, ids, clock));
        var durableSettlements = new DurableGameSettlementService(games, settlements);
        var rooms = RuntimeGameRoomRegistry.global();
        Duration idempotencyRetention = Duration.ofHours(24);
        var commandCommitter = new JdbcGatewayGameCommandCommitter(source, json, clock,
                idempotencyRetention, durableSettlements);
        commandCommitter.recoverPendingSettlements(256);
        ScheduledExecutorService recovery = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "settlement-outbox-recovery");thread.setDaemon(true);return thread;
        });
        recovery.scheduleWithFixedDelay(() -> {
            try { commandCommitter.recoverPendingSettlements(64); }
            catch (RuntimeException failure) { System.err.println("settlement outbox recovery failed: "+failure.getMessage()); }
        },5,5,TimeUnit.SECONDS);
        GatewayWebSocketFrameHandler.SessionResolver sessions = (identity, frame) -> {
            var room = rooms.require(Long.parseLong(frame.roomId()));
            Object rawPlayers = room.requireAuthoritativeSession().authoritativeState().get("players");
            if (!(rawPlayers instanceof Map<?, ?> players)) {
                throw new SecurityException("room has no authoritative seats");
            }
            Integer seated = players.entrySet().stream()
                    .filter(entry -> Long.parseLong(String.valueOf(entry.getValue())) == identity.userId())
                    .map(entry -> Integer.parseInt(String.valueOf(entry.getKey())))
                    .findFirst().orElse(null);
            Object rawObservers = room.requireAuthoritativeSession().authoritativeState().get("observers");
            Integer observing = rawObservers instanceof Map<?,?> observers ? observers.entrySet().stream()
                    .filter(entry -> Long.parseLong(String.valueOf(entry.getValue())) == identity.userId())
                    .map(entry -> Integer.parseInt(String.valueOf(entry.getKey())))
                    .findFirst().orElse(null) : null;
            int seatId=seated!=null?seated:observing!=null?observing:
                    throwSecurity("account is not a room member");
            return new GatewayWebSocketFrameHandler.SessionBinding(identity.userId(),
                    new ConnectionSession(Long.toString(identity.userId()), frame.roomId(), seatId,
                            frame.playVersion(), frame.seq() - 1));
        };
        var broadcasts = new GatewayRoomBroadcastHub((roomId, playerId) -> rooms.require(roomId)
                .requireAuthoritativeSession().viewFor(playerId),json,clock);
        var hallLifecycle = new HallRoomLifecycleClient(URI.create(environment("HALL_INTERNAL_URL","GATEWAY_HALL_URL")),environment("HALL_INTERNAL_TOKEN"),json);
        var authority = new JdbcGatewayRoomAuthority(source,games,rooms,
                clock,System.getenv().getOrDefault("AOO_GATEWAY_NODE_ID","gateway-1"),
                System.getenv().getOrDefault("AOO_GATEWAY_PUBLIC_ENDPOINT",
                        "ws://127.0.0.1:8080/api/v2/gateway/ws"),json,commandCommitter,broadcasts,hallLifecycle);
        broadcasts.configureLifecycleCompletion(authority::completeTerminal);
        broadcasts.configureMembershipCompletion(authority::completeMemberExit);
        broadcasts.configurePresenceSink((roomId,accountId,online) -> {
            var session=rooms.require(roomId).requireAuthoritativeSession();
            if(session instanceof com.aoo.bcg.gamespi.ParticipantPresenceAuthority presence)
                presence.participantPresence(accountId,online,clock.instant());
        });
        var runtime = new UnifiedGameRuntime(games, rooms, idempotency, settlements, clock,
                Duration.ofSeconds(30), idempotencyRetention, commandCommitter);
        GatewayRuntimeProvider.RoomAuthority managedAuthority = new GatewayRuntimeProvider.RoomAuthority() {
            @Override public Map<String,Object> create(Map<String,Object> command) { return authority.create(command); }
            @Override public Map<String,Object> recover(Map<String,Object> command) { return authority.recover(command); }
            @Override public Map<String,Object> join(Map<String,Object> command) { return authority.join(command); }
            @Override public void remove(long roomId,long fencingToken) { authority.remove(roomId,fencingToken); }
            @Override public void close() { recovery.shutdownNow();authority.close(); }
        };
        return new Runtime(runtime.router(), sessions, broadcasts,
                new JdbcHallWebSocketDispatcher(source),managedAuthority);
    }

    static GameRegistry loadGames() {
        GameRegistry games = new GameRegistry();
        ServiceLoader.load(GameProvider.class).forEach(games::register);
        GameCatalogLoader.registerMissing(games);
        if (games.descriptors().isEmpty()) {
            throw new IllegalStateException("no production GameProvider is available to Gateway");
        }
        return games;
    }
    private static int throwSecurity(String message){throw new SecurityException(message);}
    private static long number(Map<String,Object> v,String k){Object x=v.get(k);if(!(x instanceof Number n)||n.longValue()<=0)throw new IllegalArgumentException(k+" must be positive");return n.longValue();}
    private static String text(Map<String,Object> v,String k){Object x=v.get(k);if(!(x instanceof String s)||s.isBlank())throw new IllegalArgumentException(k+" is required");return s;}
    private static String environment(String... keys){for(String key:keys){String value=System.getenv(key);if(value!=null&&!value.isBlank())return value.strip();}throw new IllegalStateException(String.join(" or ",keys)+" is required");}
}
