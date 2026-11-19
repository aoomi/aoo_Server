package com.aoo.bcg.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Development/test nonce store. Production wiring always uses the JDBC implementation. */
public final class InMemoryAdminNonceStore implements AdminNonceStore {
    private final Map<String, Instant> consumed = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryAdminNonceStore(Clock clock) { this.clock = Objects.requireNonNull(clock); }

    @Override public boolean consume(String namespace, String nonce, Instant expiresAt) {
        Instant now = clock.instant();
        consumed.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        return consumed.putIfAbsent(namespace + ":" + nonce, expiresAt) == null;
    }
}
