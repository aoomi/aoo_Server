package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.InMemoryIdempotencyStore;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RegionScope;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameWebSocketRouterTest {
    @Test void currentReadyUsesTypedAuthorityInsteadOfProviderHandler() {
        Instant now = Instant.parse("2026-10-08T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        String playVersion = "chengdu1001-v1.0.0";
        AtomicInteger gameplayCalls = new AtomicInteger();
        AtomicInteger authorityCalls = new AtomicInteger();
        ReadySession readySession = new ReadySession(1L);
        GameRoomHandle room = new GameRoomHandle(9010L, 1001, playVersion, readySession,
                new RoomRuntimeIdentity("room-9010", 1L, 9L, 0L));
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() {
                return new GameDescriptor(1001, "chengdu1001", "CD1001", GameCategory.MAHJONG,
                        "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", playVersion);
            }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> {
                    gameplayCalls.incrementAndGet();
                    throw new AssertionError("current Ready entered generic provider handler");
                });
            }
        };
        GameRegistry registry = GameRegistryFactory.production();
        registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        router.configureReadyAuthority((session, frame, resolvedRoom) -> {
            authorityCalls.incrementAndGet();
            assertSame(room, resolvedRoom);
            readySession.advanceTo(1L);
            RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(
                    room.roomId(), "room-9010", room.gameId(), "chengdu1001", playVersion,
                    1L, 9L, 0L);
            return new RoomOrchestrator.ReadyResult(identity, 7L, 0,
                    RoomOrchestrator.ReadyStatus.ACCEPTED, frame.requestId(), 1L);
        });
        WebSocketFrame frame = new WebSocketFrame("common.room.ready_req", "ready-9010", 1,
                "9010", 0, playVersion, now.toEpochMilli(), Map.of());

        var routed = router.route(new ConnectionSession("7", "9010", 0, playVersion, 0), frame);
        assertEquals("common.room.ready_resp", routed.result().msgId());
        assertEquals(List.of(0), routed.result().body().asMap().get("readySeats"));
        assertEquals(1, authorityCalls.get());
        assertEquals(0, gameplayCalls.get());
    }

    @Test void currentReadyWithoutTypedAuthorityFailsClosed() {
        Instant now = Instant.parse("2026-10-08T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        String playVersion = "chengdu1001-v1.0.0";
        GameRoomHandle room = new GameRoomHandle(9011L, 1001, playVersion, new ReadySession(0L),
                new RoomRuntimeIdentity("room-9011", 1L, 10L, 0L));
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() {
                return new GameDescriptor(1001, "chengdu1001", "CD1001", GameCategory.MAHJONG,
                        "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", playVersion);
            }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult(
                        request.msgId(), request.requestId(), Map.of("accepted", true)));
            }
        };
        GameRegistry registry = GameRegistryFactory.production();
        registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        WebSocketFrame frame = new WebSocketFrame("common.room.ready_req", "ready-9011", 1,
                "9011", 0, playVersion, now.toEpochMilli(), Map.of());

        IllegalStateException rejected = assertThrows(IllegalStateException.class,
                () -> router.route(new ConnectionSession("7", "9011", 0, playVersion, 0), frame));
        assertEquals("ROOM_ORCHESTRATOR_READY_AUTHORITY_UNAVAILABLE", rejected.getMessage());
    }

    @Test void currentGameplayUsesTypedExecuteAuthorityInsteadOfProviderHandler() {
        Instant now = Instant.parse("2026-10-08T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AtomicInteger gameplayCalls = new AtomicInteger();
        AtomicInteger authorityCalls = new AtomicInteger();
        String playVersion = "chengdu1001-v1.0.0";
        GameRoomHandle room = new GameRoomHandle(9001L, 1001, playVersion, new Object(),
                new RoomRuntimeIdentity("room-9001", 1L, 9L, 0L));
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() {
                return new GameDescriptor(1001, "chengdu1001", "CD1001", GameCategory.MAHJONG,
                        "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", playVersion);
            }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> {
                    gameplayCalls.incrementAndGet();
                    throw new AssertionError("current gameplay entered provider handler");
                });
            }
        };
        GameRegistry registry = GameRegistryFactory.production();
        registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        router.configureExecuteAuthority((session, frame, resolvedRoom) -> {
            authorityCalls.incrementAndGet();
            assertSame(room, resolvedRoom);
            RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(
                    room.roomId(), "room-9001", room.gameId(), "chengdu1001", playVersion,
                    1L, 9L, 1L);
            return new RoomOrchestrator.ExecuteResult(identity,
                    new GameCommandResult(frame.msgId(), frame.requestId(), Map.of("accepted", true)));
        });
        ConnectionSession session = new ConnectionSession("7", "9001", 0, playVersion, 0);
        WebSocketFrame frame = new WebSocketFrame("mahjong.CD101.dispatch", "execute-9001", 1,
                "9001", 1, playVersion, now.toEpochMilli(), Map.of("action", "play"));

        assertEquals(true, router.route(session, frame).result().body().asMap().get("accepted"));
        assertEquals(1, authorityCalls.get());
        assertEquals(0, gameplayCalls.get());
    }

    @Test void currentGameplayWithoutExecuteAuthorityFailsClosed() {
        Instant now = Instant.parse("2026-10-08T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        String playVersion = "chengdu1001-v1.0.0";
        GameRoomHandle room = new GameRoomHandle(9002L, 1001, playVersion, new Object(),
                new RoomRuntimeIdentity("room-9002", 1L, 10L, 0L));
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() {
                return new GameDescriptor(1001, "chengdu1001", "CD1001", GameCategory.MAHJONG,
                        "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", playVersion);
            }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult(
                        request.msgId(), request.requestId(), Map.of()));
            }
        };
        GameRegistry registry = GameRegistryFactory.production();
        registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        WebSocketFrame frame = new WebSocketFrame("mahjong.CD101.dispatch", "execute-9002", 1,
                "9002", 1, playVersion, now.toEpochMilli(), Map.of("action", "play"));

        IllegalStateException rejected = assertThrows(IllegalStateException.class,
                () -> router.route(new ConnectionSession("7", "9002", 0, playVersion, 0), frame));
        assertEquals("ROOM_ORCHESTRATOR_EXECUTE_AUTHORITY_UNAVAILABLE", rejected.getMessage());
    }

    @Test void typedRoomCommandsUseConfiguredAuthorityAndNeverGameplayHandler() {
        Instant now = Instant.parse("2026-10-08T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AtomicInteger gameplayCalls = new AtomicInteger();
        AtomicInteger authorityCalls = new AtomicInteger();
        GameRoomHandle room = new GameRoomHandle(801L, 62, "zypk-v1.0.0", new Object());
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() { return new GameDescriptor(62, "zypk", "ZYPK",
                    GameCategory.POKER, "configurable", RegionScope.NATIONAL, "", "", "zypk-v1.0.0"); }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> {
                    gameplayCalls.incrementAndGet();
                    throw new AssertionError("typed room command entered gameplay handler");
                });
            }
        };
        GameRegistry registry = GameRegistryFactory.compatibility();
        registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        router.configureRoomCommandAuthority((session, frame, resolvedRoom) -> {
            authorityCalls.incrementAndGet();
            assertSame(room, resolvedRoom);
            return new GameCommandResult(frame.msgId(), frame.requestId(), Map.of("accepted", true));
        });
        ConnectionSession session = new ConnectionSession("7", "801", 0, "zypk-v1.0.0", 0);
        for (String msgId : java.util.List.of("room.leave", "room.dissolve_apply", "room.dissolve_vote")) {
            WebSocketFrame frame = new WebSocketFrame(msgId, msgId + "-1", 1,
                    "801", 1, "zypk-v1.0.0", now.toEpochMilli(), Map.of());
            assertEquals(true, router.route(session, frame).result().body().asMap().get("accepted"));
        }
        assertEquals(3, authorityCalls.get());
        assertEquals(0, gameplayCalls.get());
    }

    @Test void typedRoomCommandFailsClosedWhenAuthorityIsNotConfigured() {
        Instant now = Instant.parse("2026-10-08T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        GameRoomHandle room = new GameRoomHandle(802L, 62, "zypk-v1.0.0", new Object());
        GameRegistry registry = GameRegistryFactory.compatibility();
        registry.register(new GameProvider() {
            public GameDescriptor descriptor() { return new GameDescriptor(62, "zypk", "ZYPK",
                    GameCategory.POKER, "configurable", RegionScope.NATIONAL, "", "", "zypk-v1.0.0"); }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult(request.msgId(),
                        request.requestId(), Map.of()));
            }
        });
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        WebSocketFrame frame = new WebSocketFrame("room.leave", "leave-802", 1,
                "802", 1, "zypk-v1.0.0", now.toEpochMilli(), Map.of());
        assertThrows(IllegalStateException.class, () -> router.route(
                new ConnectionSession("7", "802", 0, "zypk-v1.0.0", 0), frame));
    }

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
        GameRegistry registry = GameRegistryFactory.compatibility(); registry.register(provider);
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
        GameRegistry registry = GameRegistryFactory.compatibility(); registry.register(provider);
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
        GameRoomHandle room = new GameRoomHandle(647383L, 298, "nn-v1.0.0", new Object());
        GameProvider provider = new GameProvider() {
            public GameDescriptor descriptor() { return new GameDescriptor(298, "quanguo2998", "quanguo2998",
                    GameCategory.POKER, "poker:betting", RegionScope.NATIONAL, "", "", "nn-v1.0.0"); }
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() { return ignored -> room; }
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler() {
                return Optional.of((ignored, request) -> new GameCommandResult("poker.quanguo2998.dispatch",
                        request.requestId(), Map.of("stateVersion", authorityVersion.get())));
            }
        };
        GameRegistry registry = GameRegistryFactory.compatibility(); registry.register(provider);
        GameWebSocketRouter router = new GameWebSocketRouter(registry, ignored -> room,
                new WebSocketRequestGuard(clock, Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(clock), Duration.ofMinutes(10));
        ConnectionSession firstConnection = new ConnectionSession("617", "647383", 1, "nn-v1.0.0", 0);
        WebSocketFrame initialState = new WebSocketFrame("poker.quanguo2998.dispatch", "nn-page-a-1", 1,
                "647383", 0, "nn-v1.0.0", now.toEpochMilli(), Map.of());
        assertEquals(0, router.route(firstConnection, initialState).result().body().asMap().get("stateVersion"));

        authorityVersion.set(1);
        ConnectionSession reconnect = new ConnectionSession("617", "647383", 1, "nn-v1.0.0", 0);
        assertEquals(0, router.route(reconnect, initialState).result().body().asMap().get("stateVersion"),
                "reusing the prior page request ID correctly replays its old response");
        WebSocketFrame freshReconnectState = new WebSocketFrame("poker.quanguo2998.dispatch", "nn-page-b-1", 1,
                "647383", 0, "nn-v1.0.0", now.toEpochMilli(), Map.of());
        assertEquals(1, router.route(reconnect, freshReconnectState).result().body().asMap().get("stateVersion"),
                "a reconnect-scoped request ID reaches the current authority state");
    }

    @Test void outerDispatchSitRebindsConnectionFromNestedActionAndPayload() {
        Instant now=Instant.parse("2026-09-19T10:40:00Z"); Clock clock=Clock.fixed(now,ZoneOffset.UTC);
        GameRoomHandle room=new GameRoomHandle(322118L,630,"cx-v1.0.0",new Object());
        GameProvider provider=new GameProvider(){
            public GameDescriptor descriptor(){return new GameDescriptor(630,"chengdu2999","chengdu2999",GameCategory.POKER,"poker:cx",RegionScope.CITY,"","","cx-v1.0.0");}
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory(){return ignored->room;}
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->new GameCommandResult("poker.chengdu2999.dispatch",request.requestId(),Map.of("viewerSeat",0,"viewerStatus","SEATED","players",Map.of(0,599L),"stateVersion",1)));}
        };
        GameRegistry registry=GameRegistryFactory.compatibility();registry.register(provider);
        GameWebSocketRouter router=new GameWebSocketRouter(registry,ignored->room,new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofMinutes(10));
        ConnectionSession spectator=new ConnectionSession("599","322118",7,"cx-v1.0.0",0);
        WebSocketFrame frame=new WebSocketFrame("poker.chengdu2999.dispatch","10000000-0000-0000-0000-000000000001",1,"322118",0,"cx-v1.0.0",now.toEpochMilli(),Map.of("action","poker.chengdu2999.sit_req","payload",Map.of("seatId",7)));
        assertEquals(0,router.route(spectator,frame).session().seatId());
    }

    @Test void sitRebindAcceptsSameAuthoritativeSeatAndReplayKeepsBinding() {
        AtomicInteger executions=new AtomicInteger();
        var first=routeSit("700",3,Map.of("viewerSeat",3,"viewerRole","SEATED","players",Map.of("3",700L)),"20000000-0000-0000-0000-000000000001",executions);
        assertEquals(3,first.session().seatId());
        ConnectionSession originalSpectator=new ConnectionSession("700","880001",7,"cx-v1.0.0",0);
        var replay=first.router().route(originalSpectator,first.frame());
        assertTrue(replay.replayed());
        assertEquals(3,replay.session().seatId());
        assertEquals(1,executions.get(),"replay rebinds from the cached authority result without mutating authority again");
        ConnectionSession wrongRoom=new ConnectionSession("700","880002",7,"cx-v1.0.0",0);
        assertThrows(SecurityException.class,()->first.router().route(wrongRoom,first.frame()));
        ConnectionSession wrongVersion=new ConnectionSession("700","880001",7,"cx-v2.0.0",0);
        assertThrows(SecurityException.class,()->first.router().route(wrongVersion,first.frame()));
        WebSocketFrame sameWrongVersion=new WebSocketFrame(first.frame().msgId(),first.frame().requestId(),
                first.frame().seq(),first.frame().roomId(),first.frame().roundNo(),"cx-v2.0.0",
                first.frame().timestamp(),first.frame().body());
        assertThrows(SecurityException.class,()->first.router().route(wrongVersion,sameWrongVersion));
        assertEquals(1,executions.get(),"replay scope rejection must happen without re-executing authority");
    }

    @Test void sitRebindSupportsZjhSeatViewAndRejectsUnownedAuthorityResults() {
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
        GameRoomHandle room=new GameRoomHandle(880001L,630,"cx-v1.0.0",new Object());
        GameProvider provider=new GameProvider(){
            public GameDescriptor descriptor(){return new GameDescriptor(630,"chengdu2999","chengdu2999",GameCategory.POKER,"poker:cx",RegionScope.CITY,"","","cx-v1.0.0");}
            public com.aoo.bcg.gamespi.GameRoomFactory roomFactory(){return ignored->room;}
            public Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->{executions.incrementAndGet();return new GameCommandResult("poker.chengdu2999.dispatch",request.requestId(),authoritativeView);});}
        };
        GameRegistry registry=GameRegistryFactory.compatibility();registry.register(provider);
        GameWebSocketRouter router=new GameWebSocketRouter(registry,ignored->room,
                new WebSocketRequestGuard(Clock.fixed(now,ZoneOffset.UTC),Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(Clock.fixed(now,ZoneOffset.UTC)),Duration.ofMinutes(10));
        ConnectionSession spectator=new ConnectionSession(userId,"880001",requestedSeat,"cx-v1.0.0",0);
        WebSocketFrame frame=new WebSocketFrame("poker.chengdu2999.dispatch",requestId,1,"880001",0,
                "cx-v1.0.0",now.toEpochMilli(),Map.of("action","poker.chengdu2999.sit_req","payload",Map.of("seatId",requestedSeat)));
        return new SitRoute(router,frame,router.route(spectator,frame));
    }

    private static final class ReadySession implements AuthoritativeGameSession {
        private long stateVersion;

        private ReadySession(long stateVersion) {
            this.stateVersion = stateVersion;
        }

        private void advanceTo(long next) {
            stateVersion = next;
        }

        @Override public GameCommandResult execute(com.aoo.bcg.gamespi.GameCommandRequest request) {
            throw new AssertionError("router test session must not execute generic commands");
        }

        @Override public Map<String, Object> viewFor(long viewerPlayerId) {
            return Map.of("readySeats", List.of(0), "viewerPlayerId", viewerPlayerId);
        }

        @Override public Map<String, Object> authoritativeState() {
            return Map.of("state", "WAITING", "players", Map.of(0, 7L),
                    "stateVersion", stateVersion);
        }

        @Override public long stateVersion() { return stateVersion; }
        @Override public com.aoo.bcg.gamespi.time.OperationDeadline operationDeadline() {
            return com.aoo.bcg.gamespi.time.OperationDeadline.none();
        }
        @Override public com.aoo.bcg.gamespi.time.OperationDeadlineArbiter deadlineArbiter() {
            return new com.aoo.bcg.gamespi.time.OperationDeadlineArbiter();
        }
        @Override public List<String> invariantViolations() { return List.of(); }
        @Override public com.aoo.bcg.gamespi.SettlementPayload settlement(int roundNo,
                                                                            String playVersion) {
            throw new IllegalStateException("settlement not part of Ready router test");
        }
    }

    private record SitRoute(GameWebSocketRouter router,WebSocketFrame frame,GameWebSocketRouter.RoutedResult result) {
        ConnectionSession session(){return result.session();}
    }
}
