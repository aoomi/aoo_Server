package com.aoo.bcg.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubTemplateEntryOwnershipTest {
    @Test void clickingMemberOwnsTheNewEntityRoomWithoutPreSeatingTheClubOwner() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcHallWebSocketDispatcher.java"));
        assertTrue(source.contains("long owner=actor;"));
        assertFalse(source.contains("long owner=((Number)template.get(\"ownerId\")).longValue();"));
    }
}
