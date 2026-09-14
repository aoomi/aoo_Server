package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigReloadPolicyTest {
    @Test void infrastructureAndProtocolSettingsCannotBePartiallyHotChanged() {
        var active = Map.of(RuntimeConfigKey.MQ_PRODUCER_GROUP, "group-a", RuntimeConfigKey.ADMIN_API_TOKEN, "secret://vault/admin#v1");
        var poolChange = Map.of(RuntimeConfigKey.MQ_PRODUCER_GROUP, "group-b", RuntimeConfigKey.ADMIN_API_TOKEN, "secret://vault/admin#v1");
        var secretChange = Map.of(RuntimeConfigKey.MQ_PRODUCER_GROUP, "group-a", RuntimeConfigKey.ADMIN_API_TOKEN, "secret://vault/admin#v2");
        assertThrows(IllegalStateException.class, () -> ConfigReloadPolicy.requireAtomicHotReload(active, poolChange));
        assertThrows(IllegalStateException.class, () -> ConfigReloadPolicy.requireAtomicHotReload(active, secretChange));
        assertDoesNotThrow(() -> ConfigReloadPolicy.requireAtomicHotReload(active, active));
    }
}
