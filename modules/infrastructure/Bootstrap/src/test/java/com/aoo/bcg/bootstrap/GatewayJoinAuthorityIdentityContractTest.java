package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contract for the bounded join identity gate. */
final class GatewayJoinAuthorityIdentityContractTest {
    @Test
    void joinRequiresTheDurableRuntimeIdentityAndActiveFence() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("requireJoinRuntimeIdentity(room, route)"));
        assertTrue(source.contains("ROOM_JOIN_RUNTIME_IDENTITY_REQUIRED"));
        assertTrue(source.contains("ROOM_JOIN_RUNTIME_IDENTITY_CONFLICT"));
        assertTrue(source.contains("roomIdentityStore.require(room.roomId())"));
        assertTrue(source.contains("stored.identity().fencingToken() != route.fencingToken()"));
    }

    @Test
    void joinDoesNotInventIdentityFromRoomOrGameIdentifiers() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        int start = source.indexOf("private RoomRuntimeIdentityStore.Entry requireJoinRuntimeIdentity");
        int end = source.indexOf("\n    }", start);
        assertTrue(start >= 0 && end > start, "join identity gate must remain bounded");
        String gate = source.substring(start, end);
        assertFalse(gate.contains("String.valueOf(room.roomId())"));
        assertFalse(gate.contains("getOrDefault"));
        assertFalse(gate.contains("UUID.randomUUID"));
    }
}
