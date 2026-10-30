package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contract for the provider's typed adapter and Hall destroy bridge. */
final class RoomDestroyPortAdapterTest {
    @Test
    void providerBuildsExplicitFullIdentityAndCallsTheAcceptedAdapter() throws Exception {
        String source = source();
        assertTrue(source.contains("destroyThroughOrchestrator(authority, command)"));
        assertTrue(source.contains("authority.destroyThroughRoomOrchestrator(typed)"));
        assertFalse(source.contains("authority.destroy(typed)"));
        assertTrue(source.contains("text(command, \"roomInstanceId\")"));
        assertTrue(source.contains("positiveDestroy(command, \"lifecycleEpoch\")"));
        assertTrue(source.contains("positiveDestroy(command, \"fencingToken\")"));
        assertTrue(source.contains("nonNegativeDestroy(command, \"stateVersion\")"));
        assertTrue(source.contains("text(command, \"sessionId\")"));
        assertTrue(source.contains("text(command, \"requestId\")"));
    }

    @Test
    void providerMapsEveryTypedDestroyStatusAndDisconnectsHallVoidFallback() throws Exception {
        String source = source();
        assertTrue(source.contains("return Map.of(\"status\", status, \"destroyed\", false"));
        assertTrue(source.contains("return \"CAS_CONFLICT\""));
        assertTrue(source.contains("return \"NOT_FOUND\""));
        assertTrue(source.contains("return \"ALREADY_DESTROYED\""));
        assertTrue(source.contains("throw new IllegalStateException(\"ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED\")"));
        assertTrue(source.contains("requireDestroySuccess(managedAuthority.destroy(value))"));
        assertFalse(source.contains("managedAuthority.remove(command.roomId(),fence)"));
        assertFalse(source.contains("authority.remove(roomId,fencingToken)"));
    }

    @Test
    void directHallDestroyCarriesStoredIdentityInsteadOfDerivingIt() throws Exception {
        String source = source();
        assertTrue(source.contains("copyExpectedIdentity(value, expectedLease, command)"));
        assertTrue(source.contains("expectedLease, command"));
        assertTrue(source.contains("value.put(\"reason\", \"HALL_CREATE_COMPENSATION\")"));
        assertTrue(source.contains("return requireDestroySuccess(managedAuthority.destroy(value))"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
    }
}
