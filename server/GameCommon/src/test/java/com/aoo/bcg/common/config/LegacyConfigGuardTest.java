package com.aoo.bcg.common.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LegacyConfigGuardTest {
    @Test void validatesRequiredValuesAndRanges() {
        Map<String, String> values = Map.of("rate", "50");
        assertEquals(50, LegacyConfigGuard.range(values, "PDK", "rate", 0, 100));
        assertThrows(IllegalStateException.class, () -> LegacyConfigGuard.required(values, "PDK", "missing"));
        assertThrows(IllegalStateException.class, () -> LegacyConfigGuard.range(Map.of("rate", "101"), "PDK", "rate", 0, 100));
    }
}
