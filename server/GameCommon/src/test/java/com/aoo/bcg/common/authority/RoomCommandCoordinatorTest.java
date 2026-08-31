package com.aoo.bcg.common.authority;

import com.aoo.bcg.common.idempotency.InMemoryIdempotencyStore;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomCommandCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @Test void fencesIdentitySeatConnectionRoundVersionLeaseSequenceAndRequestReuse() {
        TestSession session = new TestSession();
        RoomCommandCoordinator coordinator = coordinator();
        coordinator.register(new GameRoomHandle(8, 62, "rules-v2", session), 3, 10);
        coordinator.bind(8, new RoomCommandCoordinator.PlayerBinding("user-7", 1, "old", 1));
        coordinator.bind(8, new RoomCommandCoordinator.PlayerBinding("user-7", 1, "new", 2));

        GameCommandRequest accepted = request("request-1", 1, 8, 3, "rules-v2", "user-7", 1, 4);
        assertThrows(SecurityException.class, () -> execute(coordinator, 9, "new", 2, accepted, session));
        assertThrows(SecurityException.class, () -> execute(coordinator, 10, "old", 1, accepted, session));
        assertThrows(SecurityException.class, () -> execute(coordinator, 10, "new", 2,
                request("request-x", 1, 8, 3, "rules-v2", "user-7", 0, 4), session));
        assertThrows(SecurityException.class, () -> execute(coordinator, 10, "new", 2,
                request("request-x", 1, 8, 4, "rules-v2", "user-7", 1, 4), session));
        assertThrows(SecurityException.class, () -> execute(coordinator, 10, "new", 2,
                request("request-x", 1, 8, 3, "rules-v3", "user-7", 1, 4), session));

        GameCommandResult result = execute(coordinator, 10, "new", 2, accepted, session);
        assertEquals(NOW.toEpochMilli(), result.serverTimeEpochMillis());
        assertEquals(1, session.executions.get());
        assertEquals(result, execute(coordinator, 10, "new", 2, accepted, session));
        assertEquals(1, session.executions.get());
        assertThrows(SecurityException.class, () -> execute(coordinator, 10, "new", 2,
                request("request-1", 1, 8, 3, "rules-v2", "user-7", 1, 5), session));
        assertThrows(SecurityException.class, () -> execute(coordinator, 10, "new", 2,
                request("request-2", 1, 8, 3, "rules-v2", "user-7", 1, 4), session));
    }

    @Test void serializesOneRoomWhileDifferentRoomsCanProgressIndependently() throws Exception {
        RoomCommandCoordinator coordinator = coordinator();
        BlockingSession first = new BlockingSession();
        TestSession second = new TestSession();
        coordinator.register(new GameRoomHandle(1, 1, "v1", first), 0, 1);
        coordinator.register(new GameRoomHandle(2, 1, "v1", second), 0, 1);
        coordinator.bind(1, new RoomCommandCoordinator.PlayerBinding("u1", 0, "c1", 1));
        coordinator.bind(2, new RoomCommandCoordinator.PlayerBinding("u2", 0, "c2", 1));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var blocked = executor.submit(() -> execute(coordinator, 1, "c1",
                    request("r1", 1, 1, 0, "v1", "u1", 0, 1), first));
            try {
                assertTrue(first.started.await(1, TimeUnit.SECONDS));
                GameCommandResult other = executor.submit(() -> execute(coordinator, 1, "c2",
                        request("r2", 1, 2, 0, "v1", "u2", 0, 1), second)).get(1, TimeUnit.SECONDS);
                assertEquals("r2", other.requestId());
            } finally {
                first.release.countDown();
            }
            assertEquals("r1", blocked.get(1, TimeUnit.SECONDS).requestId());
        }
    }

    @Test void requestFingerprintIsCanonicalAcrossMapInsertionOrder() {
        TestSession session = new TestSession();
        RoomCommandCoordinator coordinator = coordinator();
        coordinator.register(new GameRoomHandle(5, 1, "v1", session), 0, 1);
        coordinator.bind(5, new RoomCommandCoordinator.PlayerBinding("u5", 0, "c5", 1));
        Map<String, Object> firstBody = new LinkedHashMap<>();
        firstBody.put("card", 4);
        firstBody.put("options", new LinkedHashMap<>(Map.of("b", 2, "a", 1)));
        Map<String, Object> secondBody = new LinkedHashMap<>();
        secondBody.put("options", new LinkedHashMap<>(Map.of("a", 1, "b", 2)));
        secondBody.put("card", 4);
        GameCommandRequest first = new GameCommandRequest("game.play_req", "ordered", 1,
                5, 0, "v1", "u5", 0, firstBody);
        GameCommandRequest reordered = new GameCommandRequest("game.play_req", "ordered", 1,
                5, 0, "v1", "u5", 0, secondBody);
        assertEquals(execute(coordinator, 1, "c5", first, session),
                execute(coordinator, 1, "c5", reordered, session));
        assertEquals(1, session.executions.get());
    }

    @Test void rejectsSequenceGapsAndQuarantinesUnknownMutationUntilFencedRecovery() {
        TestSession session = new TestSession();
        RoomCommandCoordinator coordinator = coordinator();
        GameRoomHandle original = new GameRoomHandle(12, 1, "v1", session);
        coordinator.register(original, 0, 4);
        coordinator.bind(12, new RoomCommandCoordinator.PlayerBinding("u12", 0, "c12", 1));

        assertThrows(SecurityException.class, () -> execute(coordinator, 4, "c12",
                request("gap", 2, 12, 0, "v1", "u12", 0, 1), session));

        GameCommandRequest uncertain = request("uncertain", 1, 12, 0, "v1", "u12", 0, 1);
        assertThrows(IllegalStateException.class, () -> coordinator.execute(12, 4, "c12", 1,
                uncertain, (room, value) -> session.execute(value),
                (room, value, result) -> { throw new IllegalStateException("lost commit outcome"); }));
        assertThrows(SecurityException.class, () -> execute(coordinator, 4, "c12",
                request("blocked", 1, 12, 0, "v1", "u12", 0, 1), session));

        TestSession restored = new TestSession();
        coordinator.takeover(12, 4, 5, new GameRoomHandle(12, 1, "v1", restored));
        assertEquals("recovered", execute(coordinator, 5, "c12",
                request("recovered", 1, 12, 0, "v1", "u12", 0, 1), restored).requestId());
    }

    private static RoomCommandCoordinator coordinator() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new RoomCommandCoordinator(new InMemoryIdempotencyStore<>(clock),
                new AuthoritativeTimeSource(clock), Duration.ofMinutes(10), 16);
    }

    private static GameCommandResult execute(RoomCommandCoordinator coordinator, long roomToken,
                                             String connection, GameCommandRequest request,
                                             TestSession session) {
        return execute(coordinator, roomToken, connection, 1, request, session);
    }

    private static GameCommandResult execute(RoomCommandCoordinator coordinator, long roomToken,
                                             String connection, long connectionVersion,
                                             GameCommandRequest request, TestSession session) {
        return coordinator.execute(request.roomId(), roomToken, connection, connectionVersion,
                request, (room, value) -> session.execute(value), (room, value, result) -> session.commits.incrementAndGet());
    }

    private static GameCommandRequest request(String requestId, long sequence, long roomId,
                                              int roundNo, String version, String user, int seat, int card) {
        return new GameCommandRequest("game.play_req", requestId, sequence, roomId, roundNo,
                version, user, seat, Map.of("card", card));
    }

    private static class TestSession implements AuthoritativeGameSession {
        final AtomicInteger executions = new AtomicInteger();
        final AtomicInteger commits = new AtomicInteger();
        @Override public GameCommandResult execute(GameCommandRequest request) {
            executions.incrementAndGet();
            return new GameCommandResult("game.play_resp", request.requestId(), Map.of("accepted", true));
        }
        @Override public Map<String, Object> viewFor(long viewerPlayerId) { return Map.of(); }
        @Override public Map<String, Object> authoritativeState() { return Map.of(); }
        @Override public long stateVersion() { return 0; }
        @Override public OperationDeadline operationDeadline() {
            return new OperationDeadline("operation-1", 0, NOW.plusSeconds(10));
        }
        @Override public OperationDeadlineArbiter deadlineArbiter() { return new OperationDeadlineArbiter(); }
        @Override public List<String> invariantViolations() { return List.of(); }
        @Override public SettlementPayload settlement(int roundNo, String playVersion) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class BlockingSession extends TestSession {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        @Override public GameCommandResult execute(GameCommandRequest request) {
            started.countDown();
            try { release.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            return super.execute(request);
        }
    }
}
