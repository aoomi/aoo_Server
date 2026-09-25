package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.InMemoryIdempotencyStore;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameWebSocketRouterTest {
    @Test void readOnlyStateRequestDoesNotCommitOrBroadcast() {
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();
        GameRoomHandle room = new GameRoomHandle(101L, 62, "zypk-v1.0.0", new Object());
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() { return new GameDescriptor(62, "zypk", "ZYPK",
                    GameCategory.POKER, "configurable", RegionScope.NATIONAL, "", "", "zypk-v1.0.0"); }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult("common.room.state_resp",
                        request.requestId(), Map.of("execution", executions.incrementAndGet())));
            }
            public com.aoo.bcg.gamespi.GameCommandCommitter commandCommitter() {
                return (ignoredRoom, ignoredRequest, ignoredResult) -> commits.incrementAndGet();
            }
        };
        GameRegistry registry = new GameRegistry(); registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        ConnectionSession session = new ConnectionSession("7", "101", 0, "zypk-v1.0.0", 0);
        WebSocketFrame first = new WebSocketFrame("common.room.state_req", "state-1", 1,
                "101", 1, "zypk-v1.0.0", now.toEpochMilli(), Map.of());
        WebSocketFrame second = new WebSocketFrame("common.room.state_req", "state-2", 2,
                "101", 1, "zypk-v1.0.0", now.toEpochMilli(), Map.of());

        var one = router.route(session, first);
        var two = router.route(one.session(), second);
        assertFalse(one.broadcast());
        assertFalse(two.broadcast());
        assertFalse(one.replayed());
        assertEquals(2, executions.get(), "reads are observed afresh rather than idempotency-cached");
        assertEquals(0, commits.get(), "reads never persist snapshots or replay entries");
    }

    @Test void executesOnceAndReturnsCachedResultForRetry() {
        Instant now = Instant.parse("2026-08-22T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();
        GameRoomHandle room = new GameRoomHandle(100L, 62, "zypk-v1.0.0", new Object());
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() { return new GameDescriptor(62, "zypk", "ZYPK",
                    GameCategory.POKER, "configurable", RegionScope.NATIONAL, "", "", "zypk-v1.0.0"); }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult("poker.zypk.operate_resp",
                        request.requestId(), Map.of("execution", executions.incrementAndGet())));
            }
            public com.aoo.bcg.gamespi.GameCommandCommitter commandCommitter() {
                return (ignoredRoom, ignoredRequest, ignoredResult) -> commits.incrementAndGet();
            }
        };
        GameRegistry registry = new GameRegistry(); registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        ConnectionSession initial = new ConnectionSession("7", "100", 0, "zypk-v1.0.0", 0);
        WebSocketFrame frame = new WebSocketFrame("poker.zypk.operate_req", "same-request", 1,
                "100", 1, "zypk-v1.0.0", now.toEpochMilli(), Map.of());

        var first = router.route(initial, frame);
        var retry = router.route(first.session(), frame);
        assertFalse(first.replayed());
        assertTrue(retry.replayed());
        assertEquals(first.result(), retry.result());
        assertEquals(1, executions.get());
        assertEquals(1, commits.get());
        assertEquals(1, retry.session().lastSequence());
    }

    @Test void rejectsForgedRoomAndVersionBeforeGameExecution() {
        Instant now = Instant.parse("2026-08-22T10:00:00Z");
        WebSocketRequestGuard guard = new WebSocketRequestGuard(Clock.fixed(now, ZoneOffset.UTC), Duration.ofSeconds(30));
        ConnectionSession session = new ConnectionSession("7", "100", 0, "v1", 0);
        WebSocketFrame wrongRoom = new WebSocketFrame("poker.test.play_req", "r1", 1, "101", 1, "v1",
                now.toEpochMilli(), Map.of());
        WebSocketFrame wrongVersion = new WebSocketFrame("poker.test.play_req", "r2", 1, "100", 1, "v2",
                now.toEpochMilli(), Map.of());
        assertThrows(SecurityException.class, () -> guard.validate(session, wrongRoom));
        assertThrows(SecurityException.class, () -> guard.validate(session, wrongVersion));
    }

    @Test void reconnectMustUseFreshRequestIdToObservePostMutationAuthorityState() {
        Instant now = Instant.parse("2026-09-19T08:05:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AtomicInteger authorityVersion = new AtomicInteger();
        GameRoomHandle room = new GameRoomHandle(647383L, 298, "cn298-v1.0.0", new Object());
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() { return new GameDescriptor(298, "CN298", "CN298",
                    GameCategory.POKER, "poker:betting", RegionScope.NATIONAL, "", "", "cn298-v1.0.0"); }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult("poker.CN298.dispatch",
                        request.requestId(), Map.of("stateVersion", authorityVersion.get())));
            }
        };
        GameRegistry registry = new GameRegistry(); registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        ConnectionSession firstConnection = new ConnectionSession("617", "647383", 1, "cn298-v1.0.0", 0);
        WebSocketFrame initialState = new WebSocketFrame("poker.CN298.dispatch", "cn298-page-a-1", 1,
                "647383", 0, "cn298-v1.0.0", now.toEpochMilli(), Map.of());
        assertEquals(0, router.route(firstConnection, initialState).result().body().asMap().get("stateVersion"));

        authorityVersion.set(1);
        ConnectionSession reconnect = new ConnectionSession("617", "647383", 1, "cn298-v1.0.0", 0);
        assertEquals(0, router.route(reconnect, initialState).result().body().asMap().get("stateVersion"),
                "reusing the prior page request ID correctly replays its old response");
        WebSocketFrame freshReconnectState = new WebSocketFrame("poker.CN298.dispatch", "cn298-page-b-1", 1,
                "647383", 0, "cn298-v1.0.0", now.toEpochMilli(), Map.of());
        assertEquals(1, router.route(reconnect, freshReconnectState).result().body().asMap().get("stateVersion"),
                "a reconnect-scoped request ID reaches the current authority state");
    }

    @Test void outerDispatchSitRebindsConnectionFromNestedActionAndPayload() {
        Instant now=Instant.parse("2026-09-19T10:40:00Z"); Clock clock=Clock.fixed(now,ZoneOffset.UTC);
        GameRoomHandle room=new GameRoomHandle(322118L,630,"cd299-v1.0.0",new Object());
        GameProvider provider=new GameProvider(){
            public GameDescriptor descriptor(){return new GameDescriptor(630,"CD299","CD299",GameCategory.POKER,"poker:cd299",RegionScope.CITY,"","","cd299-v1.0.0");}
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory(){return ignored->room;}
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->new GameCommandResult("poker.CD299.dispatch",request.requestId(),Map.of("viewerSeat",0,"viewerStatus","SEATED","players",Map.of(0,599L),"stateVersion",1)));}
        };
        GameRegistry registry=new GameRegistry();registry.register(provider);
        GameWebSocketRouter router=new GameWebSocketRouter(registry,ignored->room,new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofMinutes(10));
        ConnectionSession spectator=new ConnectionSession("599","322118",7,"cd299-v1.0.0",0);
        WebSocketFrame frame=new WebSocketFrame("poker.CD299.dispatch","10000000-0000-0000-0000-000000000001",1,"322118",0,"cd299-v1.0.0",now.toEpochMilli(),Map.of("action","poker.cd299.sit_req","payload",Map.of("seatId",7)));
        assertEquals(0,router.route(spectator,frame).session().seatId());
    }

    @Test void sitRebindAcceptsSameAuthoritativeSeatAndReplayKeepsBinding() {
        AtomicInteger executions=new AtomicInteger();
        var first=routeSit("700",3,Map.of("viewerSeat",3,"viewerRole","SEATED","players",Map.of("3",700L)),"20000000-0000-0000-0000-000000000001",executions);
        assertEquals(3,first.session().seatId());
        ConnectionSession originalSpectator=new ConnectionSession("700","880001",7,"cd299-v1.0.0",0);
        var replay=first.router().route(originalSpectator,first.frame());
        assertTrue(replay.replayed());
        assertEquals(3,replay.session().seatId());
        assertEquals(1,executions.get(),"replay rebinds from the cached authority result without mutating authority again");
        ConnectionSession wrongRoom=new ConnectionSession("700","880002",7,"cd299-v1.0.0",0);
        assertThrows(SecurityException.class,()->first.router().route(wrongRoom,first.frame()));
        ConnectionSession wrongVersion=new ConnectionSession("700","880001",7,"cd299-v2.0.0",0);
        assertThrows(SecurityException.class,()->first.router().route(wrongVersion,first.frame()));
        WebSocketFrame sameWrongVersion=new WebSocketFrame(first.frame().msgId(),first.frame().requestId(),
                first.frame().seq(),first.frame().roomId(),first.frame().roundNo(),"cd299-v2.0.0",
                first.frame().timestamp(),first.frame().body());
        assertThrows(SecurityException.class,()->first.router().route(wrongVersion,sameWrongVersion));
        assertEquals(1,executions.get(),"replay scope rejection must happen without re-executing authority");
    }

    @Test void sitRebindSupportsCn297SeatViewAndRejectsUnownedAuthorityResults() {
        var zjh=routeSit("701",7,Map.of("viewerRole","SEATED","seats",Map.of(2,Map.of("playerId",701L))),"30000000-0000-0000-0000-000000000001");
        assertEquals(2,zjh.session().seatId());
        AtomicInteger invalidSequence=new AtomicInteger(10);
        for(Map<String,Object> invalid:java.util.List.of(
                Map.of("viewerSeat",2,"viewerRole","SPECTATOR","players",Map.of(2,701L)),
                Map.of("viewerRole","SEATED","players",Map.of()),
                Map.of("viewerSeat",-1,"viewerRole","SEATED","players",Map.of()),
                Map.of("viewerSeat",2,"viewerRole","SEATED","players",Map.of(2,999L)))){
            String requestId="40000000-0000-0000-0000-"+String.format("%012d",invalidSequence.incrementAndGet());
            assertThrows(SecurityException.class,()->routeSit("701",7,invalid,requestId));
        }
    }

    private static SitRoute routeSit(String userId,int requestedSeat,Map<String,Object> authoritativeView,String requestId) {
        return routeSit(userId,requestedSeat,authoritativeView,requestId,new AtomicInteger());
    }

    private static SitRoute routeSit(String userId,int requestedSeat,Map<String,Object> authoritativeView,String requestId,
            AtomicInteger executions) {
        Instant now=Instant.parse("2026-09-25T10:00:00Z");
        GameRoomHandle room=new GameRoomHandle(880001L,630,"cd299-v1.0.0",new Object());
        GameProvider provider=new GameProvider(){
            public GameDescriptor descriptor(){return new GameDescriptor(630,"CD299","CD299",GameCategory.POKER,"poker:cd299",RegionScope.CITY,"","","cd299-v1.0.0");}
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory(){return ignored->room;}
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->{executions.incrementAndGet();return new GameCommandResult("poker.CD299.dispatch",request.requestId(),authoritativeView);});}
        };
        GameRegistry registry=new GameRegistry();registry.register(provider);
        GameWebSocketRouter router=new GameWebSocketRouter(registry,ignored->room,
                new WebSocketRequestGuard(Clock.fixed(now,ZoneOffset.UTC),Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(Clock.fixed(now,ZoneOffset.UTC)),Duration.ofMinutes(10));
        ConnectionSession spectator=new ConnectionSession(userId,"880001",requestedSeat,"cd299-v1.0.0",0);
        WebSocketFrame frame=new WebSocketFrame("poker.CD299.dispatch",requestId,1,"880001",0,
                "cd299-v1.0.0",now.toEpochMilli(),Map.of("action","poker.cd299.sit_req","payload",Map.of("seatId",requestedSeat)));
        return new SitRoute(router,frame,router.route(spectator,frame));
    }

    private record SitRoute(GameWebSocketRouter router,WebSocketFrame frame,GameWebSocketRouter.RoutedResult result) {
        ConnectionSession session(){return result.session();}
    }
}
