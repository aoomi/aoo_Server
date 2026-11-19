package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract for retry-safe identity allocation and tombstone handling. */
final class RoomRuntimeAuthorityAllocationContractTest {
    @Test
    void createUsesTheDurableIdentityAcquisitionBoundary() throws Exception {
        String source = source();
        assertTrue(source.contains("RoomRuntimeIdentityStore.Acquisition acquisition = roomIdentityStore.createOrReuse("));
        assertTrue(source.contains("RoomRuntimeIdentity identity = acquisition.entry().identity();"));
        assertTrue(source.contains("boolean identityCreated = acquisition.created();"));
        int runtimeStart = source.indexOf("private void requireOrCreateRuntime(Map<String, Object> command, Route route, long ownerId)");
        int runtimeEnd = source.indexOf("private void closeFailedCreateRuntime", runtimeStart);
        String runtime = source.substring(runtimeStart, runtimeEnd);
        assertFalse(runtime.contains("roomIdentityStore.create(route.roomId()"));
        assertTrue(runtime.contains("roomIdentityStore.createOrReuse("));
    }

    @Test
    void identityCollisionAndDestroyedTombstonesRemainFailClosed() throws Exception {
        String source = Files.readString(Path.of(
                "../ConfigCenter/src/main/java/com/aoo/bcg/config/RoomRuntimeIdentityStore.java"));
        assertTrue(source.contains("ROOM_RUNTIME_IDENTITY_DESTROYED"));
        assertTrue(source.contains("ROOM_RUNTIME_IDENTITY_CONFLICT"));
        assertTrue(source.contains("return new Acquisition(current, false)"));
        assertTrue(source.contains("throw new IllegalStateException(\"ROOM_RUNTIME_IDENTITY_CONFLICT\", conflict)"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
    }
}
