package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class GatewayRecoveryIsolationContractTest {
    @Test void oneBrokenSnapshotIsQuarantinedWithoutBlockingRemainingRooms() throws Exception {
        String source=Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        int loop=source.indexOf("for (StartupRoom candidate : candidates)");
        int terminal=source.indexOf("if (!failures.isEmpty())",loop);
        String recovery=source.substring(loop,terminal);
        assertTrue(recovery.contains("UNRECOVERABLE_AUTHORITY_SNAPSHOT"));
        assertTrue(recovery.contains("hallLifecycle.close(candidate.roomId()"));
        assertTrue(recovery.contains("transition(candidate.roomId(),claimed.fencingToken(),\"REMOVED\")"));
        assertTrue(recovery.contains("gameCode="));
        assertTrue(recovery.contains("stateVersion="));
        assertTrue(recovery.indexOf("failures.add")>recovery.indexOf("catch (RuntimeException quarantineFailure)"));
    }
}
