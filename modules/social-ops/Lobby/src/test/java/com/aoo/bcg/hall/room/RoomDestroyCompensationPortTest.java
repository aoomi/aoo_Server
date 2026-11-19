package com.aoo.bcg.hall.room;

import com.aoo.bcg.lobby.LobbyHttpRoomAuthorityPort;
import com.aoo.bcg.lobby.LobbyRoomCreateSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Compatibility boundary for the canonical Lobby typed destroy handoff. */
final class RoomDestroyCompensationPortTest {
    @Test
    void sagaPassesThePersistedLeaseIdentityToTypedDestroy() {
        AtomicReference<Map<String, Object>> destroyedLease = new AtomicReference<>();
        LobbyRoomCreateSaga.Store store = new MemoryStore();
        LobbyRoomCreateSaga.BillingPort billing = new NoopBilling();
        LobbyRoomCreateSaga.RoomPort rooms = new LobbyRoomCreateSaga.RoomPort() {
            @Override public void register(LobbyRoomCreateSaga.Command command) {}
            @Override public Map<String, Object> confirm(LobbyRoomCreateSaga.Command command, long stateVersion) {
                throw new IllegalStateException("confirmation failed");
            }
            @Override public void remove(LobbyRoomCreateSaga.Command command) {}
        };
        LobbyRoomCreateSaga.AuthorityPort authority = new LobbyRoomCreateSaga.AuthorityPort() {
            @Override public Map<String, Object> create(LobbyRoomCreateSaga.Command command) {
                return lease(command);
            }
            @Override public Map<String, Object> recover(LobbyRoomCreateSaga.Command command) {
                return lease(command);
            }
            @Override public Map<String, Object> destroy(LobbyRoomCreateSaga.Command command,
                    Map<String, Object> expectedLease) {
                destroyedLease.set(expectedLease);
                return Map.of("status", "REMOVED");
            }
        };
        LobbyRoomCreateSaga saga = new LobbyRoomCreateSaga(store, billing, rooms,
                command -> command, authority);

        assertThrows(IllegalStateException.class, () -> saga.create(7001L,
                "destroy-compensation-request", requestBody()));

        assertEquals(Map.of("roomId", 901001L, "gameId", 1001L,
                "playVersion", "chengdu1001-v1.0.0", "gameCode", "chengdu1001",
                "roomInstanceId", "instance-901001",
                "lifecycleEpoch", 2L, "fencingToken", 9L, "stateVersion", 3L),
                destroyedLease.get());
    }

    @Test
    void httpPortRejectsMissingIdentityAndDoesNotUseVoidFallback() {
        LobbyHttpRoomAuthorityPort port = new HttpRoomAuthorityPort(
                URI.create("http://127.0.0.1:1"), "hall-token", new ObjectMapper());
        LobbyRoomCreateSaga.Command command = command();

        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> port.destroy(command, null));
        assertEquals("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED", missing.getMessage());
        IllegalStateException incomplete = assertThrows(IllegalStateException.class,
                () -> port.destroy(command, Map.of("roomId", command.roomId())));
        assertEquals("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_REQUIRED", incomplete.getMessage());
        IllegalStateException fallback = assertThrows(IllegalStateException.class,
                () -> port.remove(command, 9L));
        assertEquals("ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED", fallback.getMessage());
    }

    private static Map<String, Object> lease(LobbyRoomCreateSaga.Command command) {
        return Map.of("roomId", command.roomId(), "gameId", (long) command.gameId(),
                "playVersion", command.playVersion(), "gameCode", "chengdu1001",
                "roomInstanceId", "instance-" + command.roomId(), "lifecycleEpoch", 2L,
                "fencingToken", 9L, "stateVersion", 3L);
    }

    private static LobbyRoomCreateSaga.Command command() {
        return new LobbyRoomCreateSaga.Command(7001L, "destroy-compensation-request", 901001L,
                101, "v1", "1.0.0", 1L, Map.of("playerCount", 4),
                new LobbyRoomCreateSaga.Scope("PERSONAL", 0, ""), Map.of(), "trace",
                LobbyRoomCreateSaga.CreationOrigin.PUBLIC, "request-hash");
    }

    private static Map<String, Object> requestBody() {
        return Map.of("roomId", 901001L, "gameId", 1001, "playVersion", "chengdu1001-v1.0.0",
                "clientVersion", "1.0.0", "stateVersion", 1L,
                "rules", Map.of("playerCount", 4), "scope", Map.of("type", "PERSONAL"),
                "ruleRevision", Map.of("classificationRegionCode", "CD", "releaseId", 11L,
                        "indexGeneration", 3L, "sourceHash", "a".repeat(64)));
    }

    private static final class NoopBilling implements LobbyRoomCreateSaga.BillingPort {
        @Override public void reserve(LobbyRoomCreateSaga.Command command) {}
        @Override public void confirm(LobbyRoomCreateSaga.Command command) {}
        @Override public void release(LobbyRoomCreateSaga.Command command) {}
    }

    private static final class MemoryStore implements LobbyRoomCreateSaga.Store {
        private LobbyRoomCreateSaga.State state;
        @Override public LobbyRoomCreateSaga.State begin(LobbyRoomCreateSaga.Command command) {
            state = new LobbyRoomCreateSaga.State(command.requestId(), command.requestHash(),
                    LobbyRoomCreateSaga.Step.STARTED, 0L, Map.of());
            return state;
        }
        @Override public LobbyRoomCreateSaga.State find(String requestId) { return state; }
        @Override public LobbyRoomCreateSaga.State advance(String requestId, long expectedVersion,
                LobbyRoomCreateSaga.Step step, Map<String, Object> response) {
            state = new LobbyRoomCreateSaga.State(requestId, state.requestHash(), step,
                    expectedVersion + 1, response);
            return state;
        }
        @Override public void fail(String requestId, long expectedVersion, LobbyRoomCreateSaga.Step step,
                LobbyRoomCreateSaga.Step compensationOrigin, String reason) {
            state = new LobbyRoomCreateSaga.State(requestId, state.requestHash(), step,
                    expectedVersion + 1, state.response(), compensationOrigin);
        }
    }
}
