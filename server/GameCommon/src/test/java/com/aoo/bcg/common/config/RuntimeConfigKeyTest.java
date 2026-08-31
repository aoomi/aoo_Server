package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeConfigKeyTest {
    @Test void catalogHasUniqueCanonicalAndEnvironmentNames() {
        assertDoesNotThrow(RuntimeConfigKey::validateCatalog);
        assertEquals("ADMIN_DB_URL", RuntimeConfigKey.ADMIN_DB_URL.environmentName());
        assertEquals(RuntimeConfigKey.Kind.SECRET, RuntimeConfigKey.ADMIN_API_TOKEN.kind());
    }

    @Test void typedIntegerRejectsInvalidOrOutOfRangeValues() {
        var valid = new RuntimeConfigResolver(Map.of("admin.api.port", "8088"), Map.of(), Map.of(), Map.of(), Map.of());
        assertEquals(8088, valid.integer(RuntimeConfigKey.ADMIN_API_PORT, 1, 65535));
        var invalid = new RuntimeConfigResolver(Map.of("admin.api.port", "99999"), Map.of(), Map.of(), Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> invalid.integer(RuntimeConfigKey.ADMIN_API_PORT, 1, 65535));
    }
}
