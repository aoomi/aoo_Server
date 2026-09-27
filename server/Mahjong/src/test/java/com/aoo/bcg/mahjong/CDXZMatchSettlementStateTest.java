package com.aoo.bcg.mahjong;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CDXZMatchSettlementStateTest {
    @Test void accumulatesSequentialRoundsAndRestoresExactly() {
        CDXZMatchSettlementState state = new CDXZMatchSettlementState(2);
        assertTrue(state.record(9, 1, "op-1", Map.of(10L, 4L, 20L, -4L)));
        assertFalse(state.roomFinished());
        assertTrue(state.record(9, 2, "op-2", Map.of(10L, -1L, 20L, 1L)));
        assertEquals(Map.of(10L, 3L, 20L, -3L), state.totalScore());
        assertTrue(state.roomFinished());

        CDXZMatchSettlementState restored = CDXZMatchSettlementState.restore(state.snapshot());
        assertEquals(state.snapshot(), restored.snapshot());
        assertEquals(Map.of(10L, -1L, 20L, 1L), restored.roundScoreDelta(2));
    }

    @Test void exactRetryIsIdempotentButConflictsAndGapsAreRejected() {
        CDXZMatchSettlementState state = new CDXZMatchSettlementState(3);
        Map<Long, Long> first = Map.of(10L, 2L, 20L, -2L);
        assertTrue(state.record(9, 1, "op-1", first));
        assertFalse(state.record(9, 1, "op-1", first));
        assertEquals(first, state.totalScore());
        assertThrows(IllegalStateException.class,
                () -> state.record(9, 1, "different", first));
        assertThrows(IllegalStateException.class,
                () -> state.record(9, 3, "op-3", Map.of(10L, 1L, 20L, -1L)));
        assertThrows(IllegalArgumentException.class,
                () -> state.record(9, 2, "op-2", Map.of(10L, 1L, 20L, 1L)));
    }

    @Test void restoreRejectsTamperedCumulativeTotals() {
        CDXZMatchSettlementState state = new CDXZMatchSettlementState(1);
        state.record(9, 1, "op-1", Map.of(10L, 2L, 20L, -2L));
        Map<String, Object> tampered = new java.util.LinkedHashMap<>(state.snapshot());
        tampered.put("totalScore", Map.of(10L, 99L, 20L, -2L));
        assertThrows(IllegalArgumentException.class, () -> CDXZMatchSettlementState.restore(tampered));
    }
}
