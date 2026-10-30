package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.InMemoryIdempotencyStore;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.StatePayload;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomOrchestratorReconnectRouterTest {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final RoomSnapshotMetadataSource METADATA = new RoomSnapshotMetadataSource(
            19L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "card-v1", "events-v1", "protocol-v2", "snapshot-v1", 4L, "LS",
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");

    @Test
    void lifecycleEpochUsesCanonicalDecimalStringOutsideJavascriptSafeRange() {
        assertEquals(9_007_199_254_740_992L,
                ProtocolValuePolicy.parseCanonicalLong("9007199254740992", "lifecycleEpoch", true));
        assertThrows(IllegalArgumentException.class,
                () -> ProtocolValuePolicy.parseCanonicalLong(9_007_199_254_740_992L,
                        "lifecycleEpoch", true));
        assertThrows(IllegalArgumentException.class,
                () -> ProtocolValuePolicy.parseCanonicalLong("09007199254740992",
                        "lifecycleEpoch", true));
    }

    @Test
    void roomReconnectRoutesOnlyToConfiguredOrchestratorAuthority() {
        AtomicInteger legacyHandlerCalls = new AtomicInteger();
        FakeSession authoritative = new FakeSession(Map.of(
                "players", Map.of("1", 17L), "stateVersion", 4L));
        GameRoomHandle room = new GameRoomHandle(42L, 2003, "liangshan2003-v1", authoritative);
        GameWebSocketRouter router = new GameWebSocketRouter(
                GameRegistryFactory.compatibility(), ignored -> room,
                new WebSocketRequestGuard(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(Clock.fixed(NOW, ZoneOffset.UTC)), Duration.ofMinutes(10));
        router.configureReconnectAuthority(command -> {
            assertEquals(17L, command.accountId());
            assertEquals(1, command.seatNo());
            assertEquals("connection-42", command.sessionId());
            assertEquals(METADATA, command.metadata());
            RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(
                    42L, "instance-42", 2003, "liangshan2003", "liangshan2003-v1",
                    7L, 11L, 4L);
            return new RoomOrchestrator.ReconnectResult(identity,
                    StatePayload.copyOf(Map.of("players", Map.of("1", 17L), "stateVersion", 4L)));
        });
        ConnectionSession session = new ConnectionSession("17", "42", 1, "liangshan2003-v1", 0,
                "connection-42", 1);
        WebSocketFrame frame = reconnectFrame(1L, "reconnect-42", 3L, 4L);

        GameWebSocketRouter.RoutedResult result = router.route(session, frame);
        assertEquals("room.reconnect_resp", result.result().msgId());
        assertEquals(4L, result.result().body().asMap().get("serverSeq"));
        assertEquals(0, legacyHandlerCalls.get(), "reconnect must not enter a gameplay handler");
    }

    @Test
    void reconnectFailsClosedWhenTheNewAuthorityWasNotConfigured() {
        FakeSession authoritative = new FakeSession(Map.of(
                "players", Map.of("1", 17L), "stateVersion", 4L));
        GameRoomHandle room = new GameRoomHandle(42L, 2003, "liangshan2003-v1", authoritative);
        GameWebSocketRouter router = new GameWebSocketRouter(
                GameRegistryFactory.compatibility(), ignored -> room,
                new WebSocketRequestGuard(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30)),
                new InMemoryIdempotencyStore<>(Clock.fixed(NOW, ZoneOffset.UTC)), Duration.ofMinutes(10));
        assertThrows(IllegalStateException.class,
                () -> router.route(new ConnectionSession("17", "42", 1, "liangshan2003-v1", 0,
                                "connection-42", 1), reconnectFrame(1L, "reconnect-43", 3L, 4L)));
    }

    private static WebSocketFrame reconnectFrame(long sequence, String requestId,
                                                   long lastServerSeq, long stateVersion) {
        return new WebSocketFrame("room.reconnect", requestId, sequence, "42", 0,
                "liangshan2003-v1", NOW.toEpochMilli(), Map.of(
                        "action", "room.reconnect", "roomId", "42",
                        "lastServerSeq", lastServerSeq, "reconnectToken", "token-42",
                "payload", Map.ofEntries(
                                Map.entry("sessionId", "connection-42"), Map.entry("gameId", 2003),
                                Map.entry("gameCode", "liangshan2003"), Map.entry("roomInstanceId", "instance-42"),
                                Map.entry("lifecycleEpoch", "7"), Map.entry("fencingToken", "11"),
                                Map.entry("stateVersion", Long.toString(stateVersion)), Map.entry("eventSequence", "3"),
                                Map.entry("eventId", "reconnect-42"), Map.entry("releaseId", 19L),
                                Map.entry("indexGeneration", 4L), Map.entry("regionCode", "LS"),
                                Map.entry("ruleContentHash", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                                Map.entry("componentContentHash", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
                                Map.entry("cardCodecVersion", "card-v1"), Map.entry("eventInterpreterVersion", "events-v1"),
                                Map.entry("protocolVersion", "protocol-v2"), Map.entry("snapshotSchemaVersion", "snapshot-v1"),
                                Map.entry("sourceHash", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"))));
    }

    private static final class FakeSession implements AuthoritativeGameSession {
        private final Map<String, Object> state;

        private FakeSession(Map<String, Object> state) {
            this.state = Map.copyOf(state);
        }

        @Override public GameCommandResult execute(com.aoo.bcg.gamespi.GameCommandRequest request) {
            throw new AssertionError("reconnect must not execute gameplay commands");
        }

        @Override public Map<String, Object> viewFor(long viewerPlayerId) { return state; }
        @Override public Map<String, Object> authoritativeState() { return state; }
        @Override public long stateVersion() { return ((Number) state.get("stateVersion")).longValue(); }
        @Override public OperationDeadline operationDeadline() { return OperationDeadline.none(); }
        @Override public OperationDeadlineArbiter deadlineArbiter() { return new OperationDeadlineArbiter(); }
        @Override public List<String> invariantViolations() { return List.of(); }
        @Override public SettlementPayload settlement(int roundNo, String playVersion) { return null; }
    }
}
