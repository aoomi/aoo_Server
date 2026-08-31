package com.aoo.bcg.gamespi;

import java.util.Map;

/** Typed immutable authoritative-state and snapshot boundary. */
public final class StatePayload extends TypedDocument {
    private static final StatePayload EMPTY = new StatePayload(Map.of());
    private StatePayload(Map<String, ?> values) { super(values); }
    public static StatePayload empty() { return EMPTY; }
    public static StatePayload copyOf(Map<String, ?> values) {
        return values == null || values.isEmpty() ? EMPTY : new StatePayload(values);
    }
}
