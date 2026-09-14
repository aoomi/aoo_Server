package com.aoo.bcg.common.config;

import java.net.URI;
import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Schema-validates raw configuration sources before values reach business code. */
public final class StrictRuntimeConfig {
    private static final Set<String> OWNED_ENV_PREFIXES = Set.of("AOO_", "ADMIN_", "WS_", "DB_", "FEATURE_");
    private final RuntimeConfigResolver resolver;

    private StrictRuntimeConfig(RuntimeConfigResolver resolver) { this.resolver = resolver; }

    public static StrictRuntimeConfig bind(Map<String,String> arguments, Map<String,String> environment,
                                           Map<String,String> file, Map<String,String> configCenter,
                                           Map<String,String> defaults) {
        RuntimeConfigKey.validateCatalog();
        Map<String,RuntimeConfigKey> canonical = new LinkedHashMap<>(), envNames = new LinkedHashMap<>();
        for (RuntimeConfigKey key : RuntimeConfigKey.values()) {
            canonical.put(key.canonicalName(), key);
            envNames.put(key.environmentName(), key);
        }
        validateCanonical("argument", arguments, canonical);
        validateCanonical("file", file, canonical);
        validateCanonical("config-center", configCenter, canonical);
        validateCanonical("default", defaults, canonical);
        if (environment != null) environment.forEach((name,value) -> {
            RuntimeConfigKey key = envNames.get(name);
            if (key != null) validateValue(key, value);
            else if (OWNED_ENV_PREFIXES.stream().anyMatch(name::startsWith))
                throw new IllegalArgumentException("unknown owned environment configuration: " + name);
        });
        return new StrictRuntimeConfig(new RuntimeConfigResolver(arguments, environment, file, configCenter, defaults));
    }

    public String require(RuntimeConfigKey key) { return resolver.require(key); }
    public RuntimeConfigResolver.ResolvedValue resolved(RuntimeConfigKey key) { return resolver.find(key).orElseThrow(); }
    public boolean has(RuntimeConfigKey key) { return resolver.find(key).isPresent(); }
    public int integer(RuntimeConfigKey key, int min, int max) { return resolver.integer(key, min, max); }

    private static void validateCanonical(String source, Map<String,String> values, Map<String,RuntimeConfigKey> catalog) {
        if (values == null) return;
        values.forEach((name,value) -> {
            RuntimeConfigKey key = catalog.get(name);
            if (key == null) throw new IllegalArgumentException("unknown " + source + " configuration: " + name);
            validateValue(key, value);
        });
    }

    private static void validateValue(RuntimeConfigKey key, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank configuration: " + key.canonicalName());
        try {
            switch (key.kind()) {
                case BOOLEAN -> { if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException(); }
                case DURATION -> { if (Duration.parse(value).isNegative()) throw new IllegalArgumentException(); }
                case INTEGER -> Long.parseLong(value);
                case ADDRESS -> { URI uri = URI.create(value); if (uri.getScheme() == null && !value.matches("[A-Za-z0-9_.-]+:\\d+")) throw new IllegalArgumentException(); }
                case SECRET -> SecretReference.parse(value);
                case TEXT -> { }
            }
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("invalid " + key.kind().name().toLowerCase() + " configuration: " + key.canonicalName(), error);
        }
    }
}
