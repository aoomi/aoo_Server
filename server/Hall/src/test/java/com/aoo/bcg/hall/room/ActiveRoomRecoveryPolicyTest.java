package com.aoo.bcg.hall.room;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActiveRoomRecoveryPolicyTest {
    @Test void restoresOnly_authority_owned_waiting_or_playing_rooms() {
        assertTrue(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("phase", "WAITING")));
        assertTrue(ActiveRoomRecoveryPolicy.isRecoverable("PLAYING", "ACTIVE", Map.of("phase", "PLAYING")));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "REMOVING", Map.of("phase", "WAITING")));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("DISSOLVED", "ACTIVE", Map.of("phase", "WAITING")));
    }

    @Test void never_restores_final_settlement_or_terminal_snapshots() {
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("phase", "SETTLED")));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("PLAYING", "ACTIVE", Map.of("phase", "FINISHED")));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("phase", "UNKNOWN")));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("roomTerminal", true)));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("bigSettlement", true)));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("finalSettlement", Map.of("rounds", 8))));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of("dissolved", true)));
    }

    @Test void restores_inter_round_settlement_before_final_round() {
        assertTrue(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of(
                "roundNo", 1, "roundLimit", 8, "state", Map.of("finished", true))));
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of(
                "roundNo", 8, "roundLimit", 8, "state", Map.of("finished", true))));
    }

    @Test void restores_structured_authority_snapshot_without_legacy_phase() {
        assertTrue(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of(
                "roundNo", 8, "roundLimit", 8,
                "players", Map.of("0", 380, "1", 381),
                "state", Map.of("finished", false, "currentSeat", 1))));
    }

    @Test void rejects_scored_final_round_even_before_explicit_final_settlement_payload() {
        assertFalse(ActiveRoomRecoveryPolicy.isRecoverable("OPEN", "ACTIVE", Map.of(
                "roundNo", 8, "roundLimit", 8, "roundScored", true,
                "players", Map.of("0", 380, "1", 381),
                "state", Map.of("finished", true))));
    }
}
