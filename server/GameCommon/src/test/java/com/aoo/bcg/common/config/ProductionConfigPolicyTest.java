package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProductionConfigPolicyTest {
    @Test void productionRejectsDevelopmentFallbacksAndLegacyProtocol() {
        var devFallback = StrictRuntimeConfig.bind(Map.of("aoo.environment", "production",
            "admin.dev.operator-id", "1"), Map.of(), Map.of(), Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> ProductionConfigPolicy.verify(devFallback, Set.of()));
        var legacy = StrictRuntimeConfig.bind(Map.of("aoo.environment", "production",
            "feature.legacy-protocol", "true"), Map.of(), Map.of(), Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> ProductionConfigPolicy.verify(legacy, Set.of()));
    }

    @Test void environmentAndRequiredValuesHaveNoFallback() {
        var absent = StrictRuntimeConfig.bind(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> ProductionConfigPolicy.verify(absent, Set.of()));
        var production = StrictRuntimeConfig.bind(Map.of("aoo.environment", "production"),
            Map.of(), Map.of(), Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> ProductionConfigPolicy.verify(production, Set.of(RuntimeConfigKey.ADMIN_API_TOKEN)));
    }
}
