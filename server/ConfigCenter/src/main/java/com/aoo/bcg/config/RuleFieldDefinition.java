package com.aoo.bcg.config;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Canonical rule-field vocabulary. Aliases are accepted only at draft ingestion. */
public record RuleFieldDefinition(
        String name,
        Set<String> aliases,
        ValueType type,
        String unit,
        boolean required,
        Object defaultValue,
        BigDecimal minimum,
        BigDecimal maximum,
        Set<String> enumValues,
        String displayName,
        String explanation) {

    public enum ValueType { BOOLEAN, INTEGER, DECIMAL, TEXT, ENUM }

    public RuleFieldDefinition {
        name = requireIdentifier(name, "field name");
        type = Objects.requireNonNull(type, "type");
        unit = requireText(unit, "unit");
        displayName = requireText(displayName, "displayName");
        explanation = requireText(explanation, "explanation");
        aliases = Set.copyOf(aliases == null ? Set.of() : aliases);
        enumValues = Set.copyOf(enumValues == null ? Set.of() : enumValues);
        if (aliases.contains(name) || aliases.stream().anyMatch(value -> !value.matches("[a-z][a-zA-Z0-9]*"))) {
            throw new IllegalArgumentException("invalid field aliases: " + name);
        }
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("invalid numeric range: " + name);
        }
        if (type == ValueType.ENUM && enumValues.isEmpty()) {
            throw new IllegalArgumentException("enum values are required: " + name);
        }
        if (type != ValueType.ENUM && !enumValues.isEmpty()) {
            throw new IllegalArgumentException("enum values are forbidden for non-enum field: " + name);
        }
        if (defaultValue != null) validateValue(name, type, defaultValue, minimum, maximum, enumValues);
    }

    public Object validate(Object value) {
        return validateValue(name, type, value, minimum, maximum, enumValues);
    }

    private static Object validateValue(String name, ValueType type, Object value, BigDecimal minimum,
                                        BigDecimal maximum, Set<String> enumValues) {
        if (value == null) throw new IllegalArgumentException("rule field cannot be null: " + name);
        return switch (type) {
            case BOOLEAN -> requireType(name, value, Boolean.class);
            case INTEGER -> validateInteger(name, value, minimum, maximum);
            case DECIMAL -> validateDecimal(name, value, minimum, maximum);
            case TEXT -> {
                if (!(value instanceof String text) || text.isBlank()) {
                    throw new IllegalArgumentException("rule field must be non-blank text: " + name);
                }
                yield text;
            }
            case ENUM -> {
                if (!(value instanceof String text) || !enumValues.contains(text)) {
                    throw new IllegalArgumentException("rule field has unsupported enum value: " + name);
                }
                yield text;
            }
        };
    }

    public Set<String> allNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(name);
        names.addAll(aliases);
        return Set.copyOf(names);
    }

    private static Object validateInteger(String name, Object value, BigDecimal minimum, BigDecimal maximum) {
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)) {
            throw new IllegalArgumentException("rule field must be an integer: " + name);
        }
        BigDecimal decimal = new BigDecimal(value.toString());
        validateRange(name, decimal, minimum, maximum);
        return value instanceof Long ? value : ((Number) value).intValue();
    }

    private static Object validateDecimal(String name, Object value, BigDecimal minimum, BigDecimal maximum) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("rule field must be numeric: " + name);
        }
        BigDecimal decimal = new BigDecimal(number.toString());
        validateRange(name, decimal, minimum, maximum);
        return decimal.stripTrailingZeros();
    }

    private static void validateRange(String name, BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        if (minimum != null && value.compareTo(minimum) < 0 || maximum != null && value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("rule field is outside range: " + name);
        }
    }

    private static <T> T requireType(String name, Object value, Class<T> expected) {
        if (!expected.isInstance(value)) throw new IllegalArgumentException("rule field has wrong type: " + name);
        return expected.cast(value);
    }

    private static String requireIdentifier(String value, String label) {
        value = requireText(value, label);
        if (!value.matches("[a-z][a-zA-Z0-9]*")) throw new IllegalArgumentException("invalid " + label);
        return value;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }
}
