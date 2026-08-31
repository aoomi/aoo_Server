package com.aoo.bcg.common.room;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Idempotent, retryable cleanup barrier. Every registered resource is attempted even after a failure. */
public final class PlayerExitCleanupCoordinator {
    public static final Set<String> REQUIRED_STEPS = Set.of("connection", "seat", "candidates", "timers", "chatTargets", "clientMappingPush");
    private final Map<PlayerExitScope, Set<String>> completed = new ConcurrentHashMap<>();
    public void cleanup(PlayerExitScope scope, Map<String, Runnable> steps) {
        if (scope == null || steps == null || !steps.keySet().equals(REQUIRED_STEPS)) throw new IllegalArgumentException("complete exit cleanup steps required");
        Set<String> done = completed.computeIfAbsent(scope, ignored -> ConcurrentHashMap.newKeySet());
        Map<String, Throwable> failures = new LinkedHashMap<>();
        for (String name : new LinkedHashSet<>(java.util.List.of("connection", "seat", "candidates", "timers", "chatTargets", "clientMappingPush"))) {
            if (done.contains(name)) continue;
            try { steps.get(name).run(); done.add(name); } catch (Throwable failure) { failures.put(name, failure); }
        }
        if (!failures.isEmpty()) {
            IllegalStateException aggregate = new IllegalStateException("player exit cleanup incomplete: " + failures.keySet());
            failures.values().forEach(aggregate::addSuppressed); throw aggregate;
        }
        completed.remove(scope);
    }
    public Set<String> completedSteps(PlayerExitScope scope) { return Set.copyOf(completed.getOrDefault(scope, Set.of())); }
}
