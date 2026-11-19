package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrictRuntimeConfigTest {
    @Test void rejectsUnknownKeysAndOwnedEnvironmentTypos() {
        assertThrows(IllegalArgumentException.class, () -> StrictRuntimeConfig.bind(
            Map.of("admin.api.prt", "8088"), Map.of(), Map.of(), Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> StrictRuntimeConfig.bind(
            Map.of(), Map.of("ADMIN_API_PRT", "8088"), Map.of(), Map.of(), Map.of()));
        assertDoesNotThrow(() -> StrictRuntimeConfig.bind(
            Map.of(), Map.of("PATH", "/bin", "ADMIN_API_PORT", "8088"), Map.of(), Map.of(), Map.of()));
    }

    @Test void validatesTypesBeforeResolution() {
        assertThrows(IllegalArgumentException.class, () -> StrictRuntimeConfig.bind(
            Map.of("admin.api.port", "eight"), Map.of(), Map.of(), Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> StrictRuntimeConfig.bind(
            Map.of("feature.legacy-protocol", "yes"), Map.of(), Map.of(), Map.of(), Map.of()));
        var config = StrictRuntimeConfig.bind(Map.of(), Map.of("ADMIN_API_PORT", "8088"),
            Map.of(), Map.of(), Map.of());
        assertEquals(8088, config.integer(RuntimeConfigKey.ADMIN_API_PORT, 1, 65535));
    }
}
