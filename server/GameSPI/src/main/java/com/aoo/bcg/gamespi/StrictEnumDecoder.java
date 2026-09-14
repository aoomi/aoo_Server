package com.aoo.bcg.gamespi;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Protocol enum decoder that never maps an unknown wire value to a default constant. */
public final class StrictEnumDecoder {
    private StrictEnumDecoder() {}

    public static <E extends Enum<E>> E byName(Class<E> type, Object wireValue) {
        Objects.requireNonNull(type, "type");
        if (!(wireValue instanceof String name) || name.isBlank()) throw unknown(type, wireValue);
        return Arrays.stream(type.getEnumConstants()).filter(value -> value.name().equals(name)).findFirst()
                .orElseThrow(() -> unknown(type, wireValue));
    }

    public static <E extends Enum<E>> E byCode(Class<E> type, Object wireValue, ToIntFunction<E> code) {
        Objects.requireNonNull(type, "type"); Objects.requireNonNull(code, "code");
        if (!(wireValue instanceof Number number)) throw unknown(type, wireValue);
        long raw = number.longValue();
        if (raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE) throw unknown(type, wireValue);
        return Arrays.stream(type.getEnumConstants()).filter(value -> code.applyAsInt(value) == raw).findFirst()
                .orElseThrow(() -> unknown(type, wireValue));
    }

    private static IllegalArgumentException unknown(Class<?> type, Object value) {
        return new IllegalArgumentException("unknown " + type.getSimpleName() + " wire value: " + value);
    }
}
