package com.aoo.bcg.gamespi.api;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Closed-loop deprecation metadata and objective removal decision. */
public final class ApiDeprecationRegistry {
    public record Deprecation(ApiOwnershipCatalog.Transport transport, String endpoint,
                              SemanticVersion deprecatedSince, String reason, String replacement,
                              String usageMetricName, Instant sunsetAt) {
        public Deprecation {
            if (transport == null || endpoint == null || endpoint.isBlank() || deprecatedSince == null
                    || reason == null || reason.isBlank() || replacement == null || replacement.isBlank()
                    || usageMetricName == null || !usageMetricName.matches("[a-z][a-z0-9_.-]+") || sunsetAt == null)
                throw new IllegalArgumentException("incomplete API deprecation metadata");
        }
    }
    public record Status(Deprecation deprecation, long observedCalls, boolean sunsetReached, boolean removable) {}
    private final ApiLifecycleCatalog lifecycle;
    private final Clock clock;
    private final Map<String,Entry> entries = new LinkedHashMap<>();
    private record Entry(Deprecation deprecation, LongSupplier usage) {}

    public ApiDeprecationRegistry(ApiLifecycleCatalog lifecycle, Clock clock) { this.lifecycle=lifecycle; this.clock=clock; }
    public synchronized void register(Deprecation value, LongSupplier usage) {
        lifecycle.require(value.transport(), value.endpoint());
        lifecycle.require(value.transport(), value.replacement());
        String key = value.transport()+":"+value.endpoint();
        if (entries.putIfAbsent(key, new Entry(value, usage)) != null) throw new IllegalArgumentException("duplicate deprecation: "+key);
    }
    public synchronized Optional<Status> status(ApiOwnershipCatalog.Transport transport, String endpoint) {
        Entry entry=entries.get(transport+":"+endpoint);
        if(entry==null)return Optional.empty();
        long calls=Math.max(0,entry.usage().getAsLong());
        boolean sunset=!clock.instant().isBefore(entry.deprecation().sunsetAt());
        return Optional.of(new Status(entry.deprecation(),calls,sunset,sunset&&calls==0));
    }
}
