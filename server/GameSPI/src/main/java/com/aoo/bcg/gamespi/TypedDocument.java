package com.aoo.bcg.gamespi;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable document boundary that replaces untyped String-key access with declared field keys. */
public abstract class TypedDocument extends AbstractMap<String, Object> {
    private final Map<String, Object> values;

    protected TypedDocument(Map<String, ?> values) {
        @SuppressWarnings("unchecked") Map<String, Object> source =
                (Map<String, Object>) (values == null ? Map.<String, Object>of() : values);
        this.values = ImmutableValue.freezeStringMap(source);
    }

    public final <T> T require(FieldKey<T> key) {
        return optional(key).orElseThrow(() -> new IllegalArgumentException("missing field: " + key.name()));
    }

    public final <T> Optional<T> optional(FieldKey<T> key) {
        Object value = values.get(key.name());
        if (value == null) return Optional.empty();
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("field " + key.name() + " must be "
                    + key.type().getSimpleName() + ", got " + value.getClass().getSimpleName());
        }
        return Optional.of(key.type().cast(value));
    }

    public final Map<String, Object> asMap() { return values; }
    @Override public final Set<Entry<String, Object>> entrySet() { return values.entrySet(); }
}
