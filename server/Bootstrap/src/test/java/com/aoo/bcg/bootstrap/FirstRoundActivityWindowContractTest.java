package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FirstRoundActivityWindowContractTest {
    @Test
    void firstRoundUsesLatestWaitingRoomActivityInsteadOfOriginalCreationTime() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayGameCommandCommitter.java"));
        String method = source.substring(source.indexOf("private void markFirstRoundStarted"),
                source.indexOf("private static boolean firstRoundStarted"));
        assertTrue(method.contains("last_business_activity_at>DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 300 SECOND)"));
        assertFalse(method.contains("created_at>DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 300 SECOND)"));
    }
}
