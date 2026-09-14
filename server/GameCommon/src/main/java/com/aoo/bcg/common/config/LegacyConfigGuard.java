package com.aoo.bcg.common.config;

import java.util.Map;

/** Shared validation primitives for legacy text configurations. */
public final class LegacyConfigGuard {
    private LegacyConfigGuard() {}

    public static String required(Map<String, String> values, String game, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(game + " config missing key: " + key);
        }
        return value;
    }

    public static int integer(Map<String, String> values, String game, String key) {
        try {
            return Integer.parseInt(required(values, game, key));
        } catch (NumberFormatException error) {
            throw new IllegalStateException(game + " config invalid integer: " + key, error);
        }
    }

    public static int range(Map<String, String> values, String game, String key, int min, int max) {
        int value = integer(values, game, key);
        if (value < min || value > max) {
            throw new IllegalStateException(game + " config out of range: " + key);
        }
        return value;
    }
}
