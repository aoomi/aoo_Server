package com.aoo.bcg.club;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class UnionTotalScoreTest {
    @Test void acceptsBothBoundaries() {
        assertEquals(0, UnionTotalScore.require(0));
        assertEquals(999_999_999L, UnionTotalScore.require(999_999_999L));
    }

    @Test void rejectsOutOfRangeAndNonIntegers() {
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require(-1));
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require(1_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require(1.5));
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require("1.0"));
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> UnionTotalScore.require("invalid"));
    }
}
