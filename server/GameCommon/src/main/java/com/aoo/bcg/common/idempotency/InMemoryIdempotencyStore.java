package com.aoo.bcg.common.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdempotencyStore<R> implements IdempotencyStore<R> {
    private final ConcurrentHashMap<String, Entry<R>> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryIdempotencyStore(Clock clock) { this.clock = clock; }
    @Override public Optional<IdempotencyResult<R>> findResult(IdempotencyKey key) {
        String requestId=key.storageKey();Entry<R> entry = entries.get(requestId);
        if (entry == null) return Optional.empty();
        if (!entry.expiresAt().isAfter(clock.instant())) { entries.remove(requestId, entry); return Optional.empty(); }
        return entry.value();
    }
    @Override public boolean acquire(IdempotencyKey key, Duration retention) {
        validate(key, retention);String requestId=key.storageKey();
        Instant now = clock.instant();
        entries.computeIfPresent(requestId, (ignored, entry) -> entry.expiresAt().isAfter(now) ? entry : null);
        return entries.putIfAbsent(requestId, new Entry<>(Optional.empty(), now.plus(retention))) == null;
    }
    @Override public void saveResult(IdempotencyKey key,IdempotencyResult<R> result) {
        String requestId=key.storageKey();
        if (result == null) throw new IllegalArgumentException("invalid idempotency result");
        entries.compute(requestId, (ignored, entry) -> entry != null && entry.value().isPresent()
                ? entry : new Entry<>(Optional.of(result), result.expiresAt()));
    }
    @Override public void save(IdempotencyKey key,R result,Duration retention){validate(key,retention);saveResult(key,IdempotencyResult.success(result,clock.instant().plus(retention)));}
    @Override public void release(IdempotencyKey key) { entries.computeIfPresent(key.storageKey(),
            (ignored, entry) -> entry.value().isEmpty() ? null : entry); }
    private static void validate(IdempotencyKey key, Duration retention) {
        if (key == null || retention == null || retention.isNegative() || retention.isZero()) throw new IllegalArgumentException("invalid idempotency entry");
    }
    private record Entry<R>(Optional<IdempotencyResult<R>> value, Instant expiresAt) {
        private Entry {
            if (value == null || expiresAt == null) throw new IllegalArgumentException("invalid idempotency state");
        }
    }
}
