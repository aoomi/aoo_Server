package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomInactivityExpirationContractTest {
    @Test
    void twelveHourExpirationCoversStartedWaitingAndLegacyActiveRooms() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        String durableScan = source.substring(source.indexOf("private void closeDurableInactiveRooms"),
                source.indexOf("private boolean expireWaitingRoom"));
        String atomicTransition = source.substring(source.indexOf("private void beginInactiveRoomRemoval"),
                source.indexOf("private void finishAutomaticRemoval"));

        assertTrue(durableScan.contains("lifecycle_state='ACTIVE'"));
        assertTrue(durableScan.contains("last_business_activity_at<=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 12 HOUR)"));
        assertFalse(durableScan.contains("first_round_started_at IS NOT NULL"));
        assertTrue(atomicTransition.contains("last_business_activity_at<=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 12 HOUR)"));
        assertFalse(atomicTransition.contains("first_round_started_at IS NOT NULL"));
    }
}
