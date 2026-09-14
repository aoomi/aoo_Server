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
}
