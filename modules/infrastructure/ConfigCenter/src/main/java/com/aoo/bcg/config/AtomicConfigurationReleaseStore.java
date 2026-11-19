package com.aoo.bcg.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Linearizable in-memory model; persistent adapters must provide the same compare-and-set contract. */
public final class AtomicConfigurationReleaseStore {
    private final Map<String,ConfigurationRelease> history = new LinkedHashMap<>();
    private ConfigurationRelease active;

    public synchronized ConfigurationRelease publish(ConfigurationRelease candidate, String expectedActiveReleaseId) {
        String current = active == null ? "" : active.releaseId();
        if (!current.equals(expectedActiveReleaseId)) throw new IllegalStateException("configuration release changed concurrently");
        if (history.putIfAbsent(candidate.releaseId(), candidate) != null) throw new IllegalStateException("configuration release already exists");
        active = candidate;
        return active;
    }

    public synchronized ConfigurationRelease rollback(String targetReleaseId, String expectedActiveReleaseId) {
        if (active == null || !active.releaseId().equals(expectedActiveReleaseId))
            throw new IllegalStateException("configuration release changed concurrently");
        ConfigurationRelease target = history.get(targetReleaseId);
        if (target == null) throw new IllegalArgumentException("unknown configuration release");
        active = target;
        return active;
    }

    public synchronized Optional<ConfigurationRelease> active() { return Optional.ofNullable(active); }
    public synchronized Optional<ConfigurationRelease> find(String releaseId) {
        return Optional.ofNullable(history.get(releaseId));
    }
    public synchronized Map<String, ConfigurationRelease> history() { return Map.copyOf(history); }
}
