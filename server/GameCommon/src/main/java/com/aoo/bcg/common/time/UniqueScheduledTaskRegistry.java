package com.aoo.bcg.common.time;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

/** Process-local ownership registry preventing duplicate/self-proliferating scheduler registration. */
public final class UniqueScheduledTaskRegistry implements AutoCloseable {
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public ScheduledFuture<?> register(String ownerKey, Supplier<ScheduledFuture<?>> factory) {
        if (ownerKey == null || ownerKey.isBlank()) throw new IllegalArgumentException("scheduler owner key is required");
        Objects.requireNonNull(factory, "factory");
        ScheduledFuture<?> current = tasks.get(ownerKey);
        if (current != null && !current.isDone() && !current.isCancelled()) return current;
        return tasks.compute(ownerKey, (key, existing) -> {
            if (existing != null && !existing.isDone() && !existing.isCancelled()) return existing;
            ScheduledFuture<?> created = Objects.requireNonNull(factory.get(), "scheduled task");
            return created;
        });
    }

    public boolean cancel(String ownerKey, boolean interrupt) {
        ScheduledFuture<?> future = tasks.remove(ownerKey);
        return future != null && future.cancel(interrupt);
    }

    public int activeCount() {
        tasks.entrySet().removeIf(entry -> entry.getValue().isDone() || entry.getValue().isCancelled());
        return tasks.size();
    }

    @Override public void close() {
        tasks.values().forEach(task -> task.cancel(false)); tasks.clear();
    }
}
