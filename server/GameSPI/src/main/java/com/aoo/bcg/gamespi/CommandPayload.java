package com.aoo.bcg.gamespi;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable command boundary value. The Map view exists only for transitional handlers; new
 * handlers use the typed accessors so missing fields and type mismatches cannot be confused.
 */
public final class CommandPayload extends TypedDocument {
    private static final CommandPayload EMPTY = new CommandPayload(Map.of());

    private CommandPayload(Map<String, Object> values) {
        super(values);
    }

    public static CommandPayload empty() { return EMPTY; }
    public static CommandPayload copyOf(Map<String, ?> values) {
        if (values == null || values.isEmpty()) return EMPTY;
        @SuppressWarnings("unchecked") Map<String, Object> typed = (Map<String, Object>) values;
        return new CommandPayload(typed);
    }

    public int requireInt(String name) {
        Object value = require(name);
        if (!(value instanceof Number number)) throw mismatch(name, "integer", value);
        long exact = number.longValue();
        if (exact < Integer.MIN_VALUE || exact > Integer.MAX_VALUE) throw mismatch(name, "integer", value);
        return Math.toIntExact(exact);
    }

    public long requireLong(String name) {
        Object value = require(name);
        if (!(value instanceof Number number)) throw mismatch(name, "long", value);
        return number.longValue();
    }

    public String requireString(String name) {
        Object value = require(name);
        if (!(value instanceof String text) || text.isBlank()) throw mismatch(name, "non-blank string", value);
        return text;
    }

    public List<Integer> requireIntList(String name) {
        Object value = require(name);
        if (!(value instanceof List<?> list) || list.isEmpty()) throw mismatch(name, "non-empty integer list", value);
        return List.copyOf(list.stream().map(item -> {
            if (!(item instanceof Number number)) throw mismatch(name, "integer list", item);
            return Math.toIntExact(number.longValue());
        }).toList());
    }

    private Object require(String name) {
        Objects.requireNonNull(name, "name");
        if (!containsKey(name)) throw new IllegalArgumentException("missing command field: " + name);
        return get(name);
    }

    private static IllegalArgumentException mismatch(String name, String expected, Object actual) {
        return new IllegalArgumentException("command field " + name + " must be " + expected
                + ", got " + (actual == null ? "null" : actual.getClass().getSimpleName()));
    }
}
