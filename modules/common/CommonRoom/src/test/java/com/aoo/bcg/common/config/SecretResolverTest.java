package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecretResolverTest {
    @Test void configurationAcceptsOnlyVersionedExternalReferences() {
        assertThrows(IllegalArgumentException.class, () -> StrictRuntimeConfig.bind(
            Map.of("admin.api.token", "literal-token-is-forbidden"), Map.of(), Map.of(), Map.of(), Map.of()));
        assertDoesNotThrow(() -> StrictRuntimeConfig.bind(
            Map.of("admin.api.token", "secret://vault/admin/api-token#v3"), Map.of(), Map.of(), Map.of(), Map.of()));
    }

    @Test void dispatchesToNamedProviderAndReturnsWipeableMaterial() {
        var resolver = new SecretResolver(Map.of("vault", reference -> new SecretMaterial("sensitive".toCharArray())));
        try (var material = resolver.resolve("secret://vault/admin/api-token#v3")) {
            assertArrayEquals("sensitive".toCharArray(), material.copy());
        }
        assertThrows(IllegalStateException.class, () -> new SecretResolver(Map.of()).resolve("secret://vault/x#v1"));
    }
}
