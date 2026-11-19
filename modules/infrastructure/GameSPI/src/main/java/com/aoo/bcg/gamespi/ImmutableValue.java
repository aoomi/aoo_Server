package com.aoo.bcg.gamespi;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Deep immutable snapshot for JSON-like protocol, replay, event and recovery values. */
public final class ImmutableValue {
    private ImmutableValue() {}

    public static Object freeze(Object value) { return freeze(value, new IdentityHashMap<>()); }

    public static Map<String, Object> freezeStringMap(Map<String, ?> value) {
        if (value == null || value.isEmpty()) return Map.of();
        @SuppressWarnings("unchecked") Map<String, Object> frozen = (Map<String, Object>) freeze(value);
        return frozen;
    }

    private static Object freeze(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Character || value instanceof Enum<?> || value instanceof UUID
                || value instanceof TemporalAccessor || value instanceof TemporalAmount
                || value.getClass().isRecord()) return value;
        if (visiting.put(value, Boolean.TRUE) != null)
            throw new IllegalArgumentException("cyclic mutable value is not snapshot-safe");
        try {
            if (value instanceof Map<?, ?> map) {
                Map<Object, Object> copy = new LinkedHashMap<>();
                map.forEach((key, item) -> copy.put(freeze(key, visiting), freeze(item, visiting)));
                return Collections.unmodifiableMap(copy);
            }
            if (value instanceof Set<?> set) {
                Set<Object> copy = new LinkedHashSet<>();
                set.forEach(item -> copy.add(freeze(item, visiting)));
                return Collections.unmodifiableSet(copy);
            }
            if (value instanceof Collection<?> collection) {
                List<Object> copy = new ArrayList<>(collection.size());
                collection.forEach(item -> copy.add(freeze(item, visiting)));
                return Collections.unmodifiableList(copy);
            }
            if (value.getClass().isArray()) {
                List<Object> copy = new ArrayList<>(Array.getLength(value));
                for (int index = 0; index < Array.getLength(value); index++)
                    copy.add(freeze(Array.get(value, index), visiting));
                return Collections.unmodifiableList(copy);
            }
            throw new IllegalArgumentException("mutable payload type requires a DTO record: " + value.getClass().getName());
        } finally {
            visiting.remove(value);
        }
    }
}
