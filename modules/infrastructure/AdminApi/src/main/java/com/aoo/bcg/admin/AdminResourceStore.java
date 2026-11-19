package com.aoo.bcg.admin;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Replaceable control-plane storage port with request-level idempotency. */
public final class AdminResourceStore implements AdminResourceRepository {
    private final Map<String, Map<String, Map<String, Object>>> resources = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> commandResults = new ConcurrentHashMap<>();
    private final Map<String, String> commandTargets = new ConcurrentHashMap<>();
    private final Clock clock;

    public AdminResourceStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<Map<String, Object>> list(String resourceType) {
        List<Map<String, Object>> result = bucket(resourceType).values().stream()
                .map(AdminResourceStore::withEtag).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        result.sort(Comparator.comparing(value -> String.valueOf(value.get("id"))));
        return List.copyOf(result);
    }

    @Override
    public synchronized Map<String, Object> execute(String resourceType, String id,
            long operatorId, Map<String, Object> command, String forcedStatus, String ifMatch) {
        String requestId = required(command, "requestId");
        String reason = required(command, "reason");
        if (operatorId <= 0) throw new IllegalArgumentException("operator required");
        Map<String, Object> previous = commandResults.get(requestId);
        String commandTarget = resourceType + ":" + id;
        if (previous != null) {
            if (!commandTarget.equals(commandTargets.get(requestId))) {
                throw new IllegalArgumentException("requestId already used by another command");
            }
            return withEtag(previous);
        }

        Map<String, Object> current = bucket(resourceType).get(id);
        requireMatch(ifMatch, current);

        Map<String, Object> value = new LinkedHashMap<>(command);
        value.remove("requestId");
        value.put("id", id);
        value.put("reason", reason);
        value.put("updatedBy", operatorId);
        value.put("updatedAt", clock.instant().toString());
        if (forcedStatus != null) value.put("status", forcedStatus);
        Map<String, Object> immutable = Map.copyOf(value);
        bucket(resourceType).put(id, immutable);
        commandTargets.put(requestId, commandTarget);
        commandResults.put(requestId, immutable);
        return withEtag(immutable);
    }

    private Map<String, Map<String, Object>> bucket(String resourceType) {
        return resources.computeIfAbsent(resourceType, ignored -> new ConcurrentHashMap<>());
    }

    private String required(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return text;
    }

    static Map<String, Object> withEtag(Map<String, Object> value) {
        Map<String, Object> result = new LinkedHashMap<>(value);
        result.put("_etag", etag(value));
        return Map.copyOf(result);
    }

    static String etag(Map<String, Object> value) {
        if (value == null) return null;
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            new java.util.TreeMap<>(value).forEach((key, item) -> {
                digest.update(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                digest.update((byte) 0); digest.update(String.valueOf(item).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            });
            return "\"" + java.util.HexFormat.of().formatHex(digest.digest()) + "\"";
        } catch (Exception error) { throw new IllegalStateException("cannot calculate resource etag", error); }
    }

    static void requireMatch(String ifMatch, Map<String, Object> current) {
        if (ifMatch == null || ifMatch.isBlank()) throw new AdminPreconditionException("If-Match required", etag(current));
        if (current == null ? !"*".equals(ifMatch) : !etag(current).equals(ifMatch))
            throw new AdminPreconditionException("resource version conflict", etag(current));
    }
}
