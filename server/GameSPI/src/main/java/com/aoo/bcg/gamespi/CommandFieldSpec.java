package com.aoo.bcg.gamespi;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/** Typed and bounded declaration for one command payload field. */
public record CommandFieldSpec(String name, Class<?> type, boolean required,
                               CommandFieldTrust trust, Long minimum, Long maximum,
                               Set<String> allowedValues, int maximumItems) {
    public CommandFieldSpec {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("field name is required");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(trust, "trust");
        allowedValues = Set.copyOf(allowedValues == null ? Set.of() : allowedValues);
        if (minimum != null && maximum != null && minimum > maximum)
            throw new IllegalArgumentException("invalid numeric range for " + name);
        if (maximumItems < 0) throw new IllegalArgumentException("maximumItems must not be negative");
        if (trust != CommandFieldTrust.CLIENT_INTENT && required)
            throw new IllegalArgumentException("server-derived payload fields cannot be client-required");
    }

    public static CommandFieldSpec integer(String name, boolean required, long minimum, long maximum) {
        return new CommandFieldSpec(name, Number.class, required, CommandFieldTrust.CLIENT_INTENT,
                minimum, maximum, Set.of(), 0);
    }

    public static CommandFieldSpec text(String name, boolean required, Set<String> allowedValues) {
        return new CommandFieldSpec(name, String.class, required, CommandFieldTrust.CLIENT_INTENT,
                null, null, allowedValues, 0);
    }

    public static CommandFieldSpec list(String name, boolean required, int maximumItems) {
        return new CommandFieldSpec(name, Collection.class, required, CommandFieldTrust.CLIENT_INTENT,
                null, null, Set.of(), maximumItems);
    }

    public static CommandFieldSpec serverDerived(String name, CommandFieldTrust trust) {
        if (trust == CommandFieldTrust.CLIENT_INTENT)
            throw new IllegalArgumentException("server-derived field must use a server trust class");
        return new CommandFieldSpec(name, Object.class, false, trust, null, null, Set.of(), 0);
    }

    void validate(Object value) {
        if (!type.isInstance(value)) throw new IllegalArgumentException(
                "command field " + name + " must be " + type.getSimpleName());
        if (value instanceof Number number && (minimum != null || maximum != null)) {
            long exact = exactLong(number);
            if ((minimum != null && exact < minimum) || (maximum != null && exact > maximum))
                throw new IllegalArgumentException("command field " + name + " is outside the allowed range");
        }
        if (value instanceof String text) {
            if (text.isBlank()) throw new IllegalArgumentException("command field " + name + " must not be blank");
            if (!allowedValues.isEmpty() && !allowedValues.contains(text))
                throw new IllegalArgumentException("command field " + name + " is not an allowed enum value");
        }
        if (value instanceof Collection<?> values && maximumItems > 0 && values.size() > maximumItems)
            throw new IllegalArgumentException("command field " + name + " exceeds its item budget");
    }

    private static long exactLong(Number number) {
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException invalidInteger) {
            throw new IllegalArgumentException("integer command field must be an exact signed 64-bit value",
                    invalidInteger);
        }
    }
}
