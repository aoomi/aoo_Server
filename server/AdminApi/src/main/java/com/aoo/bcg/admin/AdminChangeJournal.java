package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Append-only hash-chained audit for every user and club administration mutation. */
public final class AdminChangeJournal {
    public enum EntityType { USER, CLUB }
    public record Change(String changeId, EntityType entityType, String entityId,
            long operatorId, String reason, Map<String, Object> before, Map<String, Object> after,
            Instant occurredAt, String previousHash, String hash) {
        public Change {
            before = freezeMap(before);
            after = freezeMap(after);
        }
    }
    public interface Store { void append(Change change); List<Change> all(); }

    private final Store store;
    private final Clock clock;
    private final ObjectMapper json;

    public AdminChangeJournal(Store store, Clock clock, ObjectMapper json) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        this.json = Objects.requireNonNull(json);
    }

    public synchronized Change append(String changeId, EntityType type, String entityId,
            long operatorId, String reason, Map<String, Object> before, Map<String, Object> after) {
        if (blank(changeId) || type == null || blank(entityId) || operatorId <= 0 || blank(reason)
                || before == null || after == null || before.equals(after))
            throw new IllegalArgumentException("complete before/after change required");
        List<Change> history = store.all();
        if (history.stream().anyMatch(item -> item.changeId().equals(changeId)))
            return history.stream().filter(item -> item.changeId().equals(changeId)).findFirst().orElseThrow();
        String previousHash = history.isEmpty() ? "GENESIS" : history.getLast().hash();
        Instant occurredAt = clock.instant();
        String hash = hash(previousHash, changeId, type.name(), entityId, Long.toString(operatorId), reason,
                occurredAt.toString(), canonical(before), canonical(after));
        Change change = new Change(changeId, type, entityId, operatorId, reason,
                before, after, occurredAt, previousHash, hash);
        store.append(change);
        return change;
    }

    public boolean verify() {
        String previous = "GENESIS";
        for (Change change : store.all()) {
            String expected = hash(previous, change.changeId(), change.entityType().name(), change.entityId(),
                    Long.toString(change.operatorId()), change.reason(), change.occurredAt().toString(),
                    canonical(change.before()), canonical(change.after()));
            if (!previous.equals(change.previousHash()) || !expected.equals(change.hash())) return false;
            previous = change.hash();
        }
        return true;
    }

    private String canonical(Map<String, Object> value) {
        try { return json.writer().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(value); }
        catch (Exception error) { throw new IllegalArgumentException("change value cannot be serialized", error); }
    }
    private String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static Map<String, Object> freezeMap(Map<String, Object> source) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, freeze(value)));
        return java.util.Collections.unmodifiableMap(result);
    }
    private static Object freeze(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), freeze(item)));
            return java.util.Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list)
            return java.util.Collections.unmodifiableList(list.stream().map(AdminChangeJournal::freeze).toList());
        if (value instanceof java.util.Set<?> set)
            return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(set.stream().map(AdminChangeJournal::freeze).toList()));
        return value;
    }

    public static final class InMemoryStore implements Store {
        private final List<Change> changes = new ArrayList<>();
        @Override public synchronized void append(Change change) { changes.add(change); }
        @Override public synchronized List<Change> all() { return List.copyOf(changes); }
    }
}
