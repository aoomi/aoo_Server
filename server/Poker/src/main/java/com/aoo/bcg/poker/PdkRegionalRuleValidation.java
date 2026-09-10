package com.aoo.bcg.poker;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Fail-closed validation for region-owned room fields before common PDK expansion. */
final class PdkRegionalRuleValidation {
    private PdkRegionalRuleValidation() { }

    static int integerChoice(Map<String,Object> rules, String key, int fallback,
            Set<Integer> allowed) {
        Object raw = rules.get(key);
        int value = raw == null ? fallback : number(raw, key);
        if (!allowed.contains(value)) throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    static int integerRange(Map<String,Object> rules, String key, int fallback,
            int minimum, int maximum) {
        Object raw = rules.get(key);
        int value = raw == null ? fallback : number(raw, key);
        if (value < minimum || value > maximum)
            throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    static String stringChoice(Map<String,Object> rules, String key, String fallback,
            Set<String> allowed) {
        Object raw = rules.get(key);
        String value = raw == null ? fallback : String.valueOf(raw);
        if (!allowed.contains(value)) throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    static Set<String> stringChoices(Map<String,Object> rules, String key,
            Set<String> fallback, Set<String> allowed) {
        Object raw = rules.get(key);
        if (raw == null) return fallback;
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection)
            collection.forEach(value -> values.add(String.valueOf(value)));
        else values.add(String.valueOf(raw));
        if (!allowed.containsAll(values)) throw new IllegalArgumentException("invalid " + key);
        return Set.copyOf(values);
    }

    private static int number(Object raw, String key) {
        if (!(raw instanceof Number number))
            throw new IllegalArgumentException(key + " must be numeric");
        return number.intValue();
    }
}
