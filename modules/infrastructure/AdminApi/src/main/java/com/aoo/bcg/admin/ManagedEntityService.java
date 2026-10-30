package com.aoo.bcg.admin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Applies validated user/club patches and cannot bypass the unified change journal. */
public final class ManagedEntityService {
    private final Map<String, Map<String, Object>> values = new ConcurrentHashMap<>();
    private final AdminChangeJournal journal;

    public ManagedEntityService(AdminChangeJournal journal) { this.journal = Objects.requireNonNull(journal); }

    public synchronized Map<String, Object> change(String requestId, AdminChangeJournal.EntityType type,
            String entityId, long operatorId, String reason, Map<String, Object> patch) {
        if (patch == null || patch.isEmpty() || patch.keySet().stream().anyMatch(ManagedEntityService::forbiddenField))
            throw new IllegalArgumentException("non-empty safe patch required");
        String key = type + ":" + entityId;
        Map<String, Object> before = values.getOrDefault(key, Map.of());
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.putAll(patch);
        AdminChangeJournal.Change change = journal.append(requestId, type, entityId, operatorId, reason, before, after);
        values.put(key, change.after());
        return change.after();
    }

    public Optional<Map<String, Object>> find(AdminChangeJournal.EntityType type, String entityId) {
        return Optional.ofNullable(values.get(type + ":" + entityId));
    }

    private static boolean forbiddenField(String field) {
        return field == null || field.isBlank() || field.equalsIgnoreCase("balance")
                || field.equalsIgnoreCase("cards") || field.equalsIgnoreCase("passwordHash");
    }
}
