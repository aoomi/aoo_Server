package com.aoo.bcg.common.config;

import java.util.Map;

/** Bootstrap provider for workload-injected secrets; path is an environment variable name. */
public final class EnvironmentSecretProvider implements SecretProvider {
    private final Map<String,String> environment;
    public EnvironmentSecretProvider(Map<String,String> environment) { this.environment = Map.copyOf(environment); }
    @Override public SecretMaterial resolve(SecretReference reference) {
        if (!reference.path().matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("environment secret path must be uppercase");
        String value = environment.get(reference.path());
        if (value == null || value.isBlank()) throw new IllegalStateException("injected secret is unavailable: " + reference.path());
        return new SecretMaterial(value.toCharArray());
    }
}
