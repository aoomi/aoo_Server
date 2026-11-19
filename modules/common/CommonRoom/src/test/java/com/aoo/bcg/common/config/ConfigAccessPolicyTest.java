package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigAccessPolicyTest {
    @Test void runtimeCanReadOnlyItsNamespaceAndCannotPublish() {
        var admin = new ConfigAccessPolicy.Principal("admin-api", ConfigAccessPolicy.Role.SERVICE_READER,
            Set.of("/aoo/services/admin-api/"), Set.of());
        assertDoesNotThrow(() -> ConfigAccessPolicy.requireRead(admin, "/aoo/services/admin-api/release-7"));
        assertThrows(SecurityException.class, () -> ConfigAccessPolicy.requireRead(admin, "/aoo/services/game-server/release-7"));
        assertThrows(SecurityException.class, () -> ConfigAccessPolicy.requireWrite(admin, "/aoo/releases/release-8"));
    }

    @Test void publisherCannotReadSecretNamespace() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigAccessPolicy.Principal("ci-publisher",
            ConfigAccessPolicy.Role.RELEASE_PUBLISHER, Set.of("/aoo/secrets/"), Set.of("/aoo/releases/")));
        var publisher = new ConfigAccessPolicy.Principal("ci-publisher", ConfigAccessPolicy.Role.RELEASE_PUBLISHER,
            Set.of("/aoo/releases/"), Set.of("/aoo/releases/"));
        assertDoesNotThrow(() -> ConfigAccessPolicy.requireWrite(publisher, "/aoo/releases/release-8"));
    }
}
