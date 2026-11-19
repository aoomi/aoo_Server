package com.aoo.bcg.common.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Central JSON decode boundary: deserialize, enforce structural schema, then rerun domain invariants. */
public final class DomainJsonDecoder {
    private static final int MAX_DEPTH = 32;
    private static final int MAX_CONTAINER_SIZE = 10_000;
    private final ObjectMapper mapper;

    public DomainJsonDecoder(ObjectMapper mapper) { this.mapper = Objects.requireNonNull(mapper, "mapper"); }

    public <T> T decode(String json, Class<T> type, Consumer<T> invariant) {
        try { return validate(mapper.readValue(requireJson(json), type), invariant); }
        catch (com.fasterxml.jackson.core.JsonProcessingException error) { throw new IllegalArgumentException("invalid JSON payload", error); }
    }

    public <T> T decode(String json, TypeReference<T> type, Consumer<T> invariant) {
        try { return validate(mapper.readValue(requireJson(json), type), invariant); }
        catch (com.fasterxml.jackson.core.JsonProcessingException error) { throw new IllegalArgumentException("invalid JSON payload", error); }
    }

    public static void requireDocument(Map<String, ?> document) {
        requireStructure(Objects.requireNonNull(document, "document"), 0);
    }

    private static <T> T validate(T value, Consumer<T> invariant) {
        if (value == null) throw new IllegalArgumentException("decoded domain value is null");
        Objects.requireNonNull(invariant, "invariant").accept(value);
        return value;
    }

    private static String requireJson(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("JSON payload is required");
        return json;
    }

    private static void requireStructure(Object value, int depth) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("document nesting exceeds schema limit");
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) return;
        if (value instanceof Map<?, ?> map) {
            if (map.size() > MAX_CONTAINER_SIZE) throw new IllegalArgumentException("document object exceeds schema limit");
            map.forEach((key, child) -> {
                if (!(key instanceof String text) || text.isBlank() || text.length() > 128)
                    throw new IllegalArgumentException("invalid document field name");
                requireStructure(child, depth + 1);
            });
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.size() > MAX_CONTAINER_SIZE) throw new IllegalArgumentException("document array exceeds schema limit");
            collection.forEach(child -> requireStructure(child, depth + 1));
            return;
        }
        throw new IllegalArgumentException("unsupported decoded document value: " + value.getClass().getName());
    }
}
