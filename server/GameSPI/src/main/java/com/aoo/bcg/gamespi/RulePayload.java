package com.aoo.bcg.gamespi;

import java.util.Map;

/** Typed immutable room-rule boundary. */
public final class RulePayload extends TypedDocument {
    private static final RulePayload EMPTY = new RulePayload(Map.of());
    private RulePayload(Map<String, ?> values) { super(values); }
    public static RulePayload empty() { return EMPTY; }
    public static RulePayload copyOf(Map<String, ?> values) {
        return values == null || values.isEmpty() ? EMPTY : new RulePayload(values);
    }
}
