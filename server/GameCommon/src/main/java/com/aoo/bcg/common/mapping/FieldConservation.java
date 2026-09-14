package com.aoo.bcg.common.mapping;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Fail-closed contract for hand-written projections when source or target fields evolve. */
public final class FieldConservation {
    private FieldConservation() {}

    public static void requireExactRecordFields(Class<?> target, Set<String> mappedFields) {
        if (target == null || !target.isRecord()) throw new IllegalArgumentException("target must be a record");
        Set<String> actual = Arrays.stream(target.getRecordComponents())
                .map(RecordComponent::getName).collect(Collectors.toUnmodifiableSet());
        Set<String> declared = Set.copyOf(mappedFields);
        if (!actual.equals(declared)) {
            throw new IllegalStateException("projection field drift for " + target.getSimpleName()
                    + ": missing=" + difference(actual, declared)
                    + ", unknown=" + difference(declared, actual));
        }
    }

    public static void requireNoSensitiveFields(Set<String> mappedFields, Set<String> sensitiveFields) {
        Set<String> exposed = difference(mappedFields, difference(mappedFields, sensitiveFields));
        if (!exposed.isEmpty()) throw new SecurityException("sensitive projection fields: " + exposed);
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        return left.stream().filter(value -> !right.contains(value)).collect(Collectors.toUnmodifiableSet());
    }
}
