package com.aoo.bcg.common.config;

import java.util.Map;

public final class SecretResolver {
    private final Map<String,SecretProvider> providers;
    public SecretResolver(Map<String,SecretProvider> providers) { this.providers = Map.copyOf(providers); }
    public SecretMaterial resolve(String reference) {
        SecretReference parsed = SecretReference.parse(reference);
        SecretProvider provider = providers.get(parsed.provider());
        if (provider == null) throw new IllegalStateException("secret provider is not configured: " + parsed.provider());
        return provider.resolve(parsed);
    }
}
