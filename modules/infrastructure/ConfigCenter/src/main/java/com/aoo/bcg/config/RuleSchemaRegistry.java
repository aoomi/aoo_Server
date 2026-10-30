package com.aoo.bcg.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Immutable-version registry; the same play/schema version can never be redefined. */
public final class RuleSchemaRegistry {
    private final Map<Key, RuleSchema> schemas = new ConcurrentHashMap<>();

    public void register(RuleSchema schema) {
        if (schema == null) throw new IllegalArgumentException("schema is required");
        if (schemas.putIfAbsent(new Key(schema.playType(), schema.version()), schema) != null) {
            throw new IllegalStateException("rule schema version is immutable");
        }
    }

    public Optional<RuleSchema> find(String playType, String version) {
        return Optional.ofNullable(schemas.get(new Key(playType, version)));
    }

    public RuleSchema require(String playType, String version) {
        return find(playType, version).orElseThrow(() -> new IllegalArgumentException("rule schema not found"));
    }

    private record Key(String playType, String version) { }
}
