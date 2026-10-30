package com.aoo.bcg.common.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Bounds origin load while a cache is unavailable and provides ordered, jittered recovery warmup. */
public final class CacheRecoveryCoordinator {
    public enum Priority { CRITICAL, HOT, NORMAL }
    public record WarmupKey(String domain, String key, Priority priority) {
        public WarmupKey { Objects.requireNonNull(domain); Objects.requireNonNull(key); Objects.requireNonNull(priority); }
    }

    private static final class DomainGuard {
        final Semaphore concurrent;
        final AtomicLong nextPermitNanos = new AtomicLong();
        final AtomicInteger failures = new AtomicInteger();
        final AtomicLong openUntilNanos = new AtomicLong();
        DomainGuard(int concurrency) { concurrent = new Semaphore(concurrency, true); }
    }

    private final ConcurrentHashMap<String, DomainGuard> domains = new ConcurrentHashMap<>();
    private final int maxConcurrent;
    private final long permitIntervalNanos;
    private final int failureThreshold;
    private final long openNanos;

    public CacheRecoveryCoordinator(int maxConcurrentPerDomain, int maxQpsPerDomain,
                                    int failureThreshold, Duration openDuration) {
        if (maxConcurrentPerDomain < 1 || maxQpsPerDomain < 1 || failureThreshold < 1) throw new IllegalArgumentException("limits must be positive");
        this.maxConcurrent = maxConcurrentPerDomain;
        this.permitIntervalNanos = 1_000_000_000L / maxQpsPerDomain;
        this.failureThreshold = failureThreshold;
        this.openNanos = Objects.requireNonNull(openDuration).toNanos();
        if (openNanos < 1) throw new IllegalArgumentException("openDuration must be positive");
    }

    public <T> T protectedOriginLoad(String domain, Callable<T> load) throws Exception {
        Objects.requireNonNull(domain); Objects.requireNonNull(load);
        DomainGuard guard = domains.computeIfAbsent(domain, ignored -> new DomainGuard(maxConcurrent));
        long now = System.nanoTime();
        if (now < guard.openUntilNanos.get()) throw new OriginProtectedException("origin circuit open for " + domain);
        if (!guard.concurrent.tryAcquire()) throw new OriginProtectedException("origin concurrency exhausted for " + domain);
        try {
            reserveRatePermit(guard);
            T value = load.call();
            guard.failures.set(0);
            return value;
        } catch (Exception failure) {
            if (guard.failures.incrementAndGet() >= failureThreshold) guard.openUntilNanos.set(System.nanoTime() + openNanos);
            throw failure;
        } finally {
            guard.concurrent.release();
        }
    }

    public List<WarmupKey> warmup(Collection<WarmupKey> keys, WarmupLoader loader,
                                  int batchSize, Duration maxJitter) throws InterruptedException {
        Objects.requireNonNull(keys); Objects.requireNonNull(loader); Objects.requireNonNull(maxJitter);
        if (batchSize < 1 || maxJitter.isNegative()) throw new IllegalArgumentException("invalid warmup limits");
        List<WarmupKey> ordered = new ArrayList<>(keys);
        ordered.sort(Comparator.comparing(WarmupKey::priority).thenComparing(WarmupKey::domain).thenComparing(WarmupKey::key));
        List<WarmupKey> completed = new ArrayList<>();
        for (int offset = 0; offset < ordered.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, ordered.size());
            for (WarmupKey key : ordered.subList(offset, end)) {
                try {
                    protectedOriginLoad(key.domain(), () -> { loader.load(key); return Boolean.TRUE; });
                    completed.add(key);
                } catch (OriginProtectedException ignored) {
                    // Protection wins over warmup completeness; a later pass resumes safely.
                } catch (Exception failure) {
                    // A failed key is intentionally absent from the completion checkpoint.
                }
            }
            if (end < ordered.size() && !maxJitter.isZero()) {
                long bound = maxJitter.toMillis();
                if (bound > 0) Thread.sleep(ThreadLocalRandom.current().nextLong(bound + 1));
            }
        }
        return List.copyOf(completed);
    }

    private void reserveRatePermit(DomainGuard guard) {
        for (int attempt = 0; attempt < 1_024; attempt++) {
            long now = System.nanoTime();
            long previous = guard.nextPermitNanos.get();
            long reserved = Math.max(now, previous);
            if (!guard.nextPermitNanos.compareAndSet(previous, reserved + permitIntervalNanos)) { Thread.onSpinWait(); continue; }
            if (reserved > now) throw new OriginProtectedException("origin rate exhausted");
            return;
        }
        throw new OriginProtectedException("origin permit contention budget exhausted");
    }

    @FunctionalInterface public interface WarmupLoader { void load(WarmupKey key) throws Exception; }
    public static final class OriginProtectedException extends RuntimeException {
        public OriginProtectedException(String message) { super(message); }
    }
}
