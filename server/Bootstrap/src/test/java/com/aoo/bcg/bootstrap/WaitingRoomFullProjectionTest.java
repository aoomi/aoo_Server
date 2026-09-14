package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class WaitingRoomFullProjectionTest {
    @Test
    void currentRoomReturnsAuthoritativeCapacityForEveryWaitingMember() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcHallWebSocketDispatcher.java"));
        assertTrue(source.contains("occupied.status='JOINED'"));
        assertTrue(source.contains("result.put(\"occupiedCount\",occupiedCount)"));
        assertTrue(source.contains("result.put(\"waitingFull\",playerNum>0&&occupiedCount>=playerNum)"));
    }
}
