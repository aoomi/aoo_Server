package com.aoo.bcg.gateway;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/** Byte-level v2 JSON contract. IDs and decimal values are strings to remain lossless in JavaScript. */
public final class ProtocolValuePolicy {
    public static final int MAX_STRING_BYTES = 16 * 1024;
    public static final int MAX_COLLECTION_ITEMS = 4096;
    public static final long MAX_SAFE_JS_INTEGER = 9_007_199_254_740_991L;
    private static final Pattern ID = Pattern.compile("[1-9][0-9]{0,19}");
    private ProtocolValuePolicy() {}
    public static String id(long value) { if (value <= 0) throw new IllegalArgumentException("positive id required"); return Long.toUnsignedString(value); }
    public static long parseId(String value) { if (value == null || !ID.matcher(value).matches()) throw new IllegalArgumentException("canonical decimal id required"); return Long.parseLong(value); }
    public static String decimal(BigDecimal value) { Objects.requireNonNull(value); return value.stripTrailingZeros().toPlainString(); }
    public static long epochMillis(long value) { if (value <= 0) throw new IllegalArgumentException("positive UTC epoch millis required"); return value; }
    public static long durationMillis(Duration value) { Objects.requireNonNull(value); if (value.isNegative()) throw new IllegalArgumentException("negative duration"); return value.toMillis(); }
    public static <E extends Enum<E>> Optional<E> enumValue(Class<E> type, String wire) {
        if (wire == null) return Optional.empty();
        try { return Optional.of(Enum.valueOf(type, wire)); } catch (IllegalArgumentException unknown) { return Optional.empty(); }
    }
    public static void validate(Object value) {
        validate(value, 0);
    }
    private static void validate(Object value, int depth) {
        if (depth > 32) throw new IllegalArgumentException("payload nesting exceeds 32");
        if (value == null) throw new IllegalArgumentException("null JSON values forbidden");
        if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) throw new IllegalArgumentException("floating-point JSON values forbidden");
        if (value instanceof String s && s.getBytes(StandardCharsets.UTF_8).length > MAX_STRING_BYTES) throw new IllegalArgumentException("string too large");
        if (value instanceof Map<?,?> m) {
            if (m.size() > MAX_COLLECTION_ITEMS) throw new IllegalArgumentException("object too large");
            for (var e : m.entrySet()) { if (!(e.getKey() instanceof String)) throw new IllegalArgumentException("object keys must be strings"); validate(e.getValue(), depth + 1); }
        } else if (value instanceof List<?> c) {
            if (c.size() > MAX_COLLECTION_ITEMS) throw new IllegalArgumentException("collection too large");
            c.forEach(v -> validate(v, depth + 1));
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            BigInteger integer = value instanceof BigInteger b ? b : BigInteger.valueOf(((Number)value).longValue());
            if (integer.abs().compareTo(BigInteger.valueOf(MAX_SAFE_JS_INTEGER)) > 0) throw new IllegalArgumentException("integer exceeds lossless JSON range");
        } else if (!(value instanceof String) && !(value instanceof Boolean)) throw new IllegalArgumentException("unsupported JSON value: " + value.getClass().getName());
    }
    public static <T> List<T> ordered(Collection<T> source, Comparator<? super T> comparator) {
        var copy = new ArrayList<>(source); copy.sort(comparator); return List.copyOf(copy);
    }
}
