package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract tests for v2.09 create-failure compensation. */
final class RoomRuntimeAuthorityFailureTombstoneTest {
    @Test
    void createFailureClosesTheExactIdentityAndPreservesTheOriginalCause() throws Exception {
        String source = source();
        assertTrue(source.contains("boolean identityCreated = acquisition.created()"));
        assertTrue(source.contains("closeFailedCreateRuntime(route, identity"));
        assertTrue(source.contains("destroy(route.roomId(), identity.fencingToken(), identity"));
        assertTrue(source.contains("new IllegalStateException(code, originalFailure)"));
        assertTrue(source.contains("failure.addSuppressed(cleanupFailure)"));
    }

    @Test
    void createFailureDestroyIsTheOnlyPathAllowedToSealCreatingRoute() throws Exception {
        String source = source();
        assertTrue(source.contains("\"CREATE_RUNTIME_FAILURE\".equals(reason)"));
        assertTrue(source.contains("\"CREATING\".equals(route.state())"));
        assertTrue(source.contains("roomIdentityStore.destroy(roomId, stored.identity())"));
        assertTrue(source.contains("\"ROOM_RUNTIME_CREATE_TOMBSTONE_\" + result.status()"));
        assertTrue(source.contains("if (\"REMOVED\".equals(route(roomId).state())) return;"));
    }

    @Test
    void successfulCreateAndRecoverDoNotCallFailureCompensation() throws Exception {
        String source = source();
        int compensationCalls = occurrences(source, "closeFailedCreateRuntime(route, identity");
        assertTrue(compensationCalls == 1,
                "only the create catch block may compensate a failed create");
        assertTrue(source.contains("RoomRuntimeIdentity identity = advanceIdentityIfRequired("));
        assertTrue(source.contains("stateStore.saveSnapshot(restored, route.fencingToken())"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }
}
