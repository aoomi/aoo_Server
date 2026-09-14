package com.aoo.bcg.gamespi;

import java.util.Objects;

/** Stable, typed key for protocol, rule and authoritative-state documents. */
public record FieldKey<T>(String name, Class<T> type) {
    public FieldKey {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("field name is required");
        Objects.requireNonNull(type, "type");
    }
}
