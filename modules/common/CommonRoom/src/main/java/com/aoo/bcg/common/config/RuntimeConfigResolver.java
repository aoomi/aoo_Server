package com.aoo.bcg.common.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** Single runtime precedence contract: arguments > environment > file > config center > defaults. */
public final class RuntimeConfigResolver {
    public enum Origin { ARGUMENT, ENVIRONMENT, FILE, CONFIG_CENTER, DEFAULT }
    public record ResolvedValue(String value, Origin origin) {
        public ResolvedValue { if (value == null || value.isBlank()) throw new IllegalArgumentException("blank configuration value"); }
    }

    private final Map<String,String> arguments;
    private final Map<String,String> environment;
    private final Map<String,String> file;
    private final Map<String,String> configCenter;
    private final Map<String,String> defaults;

    public RuntimeConfigResolver(Map<String,String> arguments, Map<String,String> environment,
                                 Map<String,String> file, Map<String,String> configCenter,
                                 Map<String,String> defaults) {
        this.arguments = clean(arguments);
        this.environment = clean(environment);
        this.file = clean(file);
        this.configCenter = clean(configCenter);
        this.defaults = clean(defaults);
    }

    public static RuntimeConfigResolver system(Properties file, Map<String,String> configCenter,
                                               Map<String,String> defaults) {
        Map<String,String> properties = new LinkedHashMap<>();
        System.getProperties().forEach((key,value) -> properties.put(String.valueOf(key), String.valueOf(value)));
        Map<String,String> fileValues = new LinkedHashMap<>();
        if (file != null) file.forEach((key,value) -> fileValues.put(String.valueOf(key), String.valueOf(value)));
        return new RuntimeConfigResolver(properties, System.getenv(), fileValues, configCenter, defaults);
    }

    public Optional<ResolvedValue> find(String key) {
        requireKey(key);
        String envKey = key.replace('.', '_').replace('-', '_').toUpperCase(java.util.Locale.ROOT);
        if (arguments.containsKey(key)) return Optional.of(new ResolvedValue(arguments.get(key), Origin.ARGUMENT));
        if (environment.containsKey(envKey)) return Optional.of(new ResolvedValue(environment.get(envKey), Origin.ENVIRONMENT));
        if (file.containsKey(key)) return Optional.of(new ResolvedValue(file.get(key), Origin.FILE));
        if (configCenter.containsKey(key)) return Optional.of(new ResolvedValue(configCenter.get(key), Origin.CONFIG_CENTER));
        if (defaults.containsKey(key)) return Optional.of(new ResolvedValue(defaults.get(key), Origin.DEFAULT));
        return Optional.empty();
    }

    public Optional<ResolvedValue> find(RuntimeConfigKey key) { return find(key.canonicalName()); }

    public String require(RuntimeConfigKey key) { return require(key.canonicalName()); }

    public int integer(RuntimeConfigKey key, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(require(key));
            if (value < minimum || value > maximum) throw new IllegalArgumentException();
            return value;
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("invalid integer configuration: " + key.canonicalName(), error);
        }
    }

    public String require(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException("required configuration is missing: " + key)).value();
    }

    private static Map<String,String> clean(Map<String,String> values) {
        Map<String,String> result = new LinkedHashMap<>();
        if (values != null) values.forEach((key,value) -> {
            if (key != null && value != null && !value.isBlank()) result.put(key, value.trim());
        });
        return Map.copyOf(result);
    }

    private static void requireKey(String key) {
        Objects.requireNonNull(key, "key");
        if (!key.matches("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*")) throw new IllegalArgumentException("configuration key must be canonical: " + key);
    }
}
