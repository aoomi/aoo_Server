package com.aoo.bcg.gamespi.time;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Injectable monotonic source for elapsed metrics only; never persist its values as business time. */
public final class MonotonicTicker {
    private final LongSupplier nanos;

    public MonotonicTicker(LongSupplier nanos) { this.nanos = Objects.requireNonNull(nanos, "nanos"); }
    public static MonotonicTicker system() { return new MonotonicTicker(System::nanoTime); }
    public long mark() { return nanos.getAsLong(); }
    public Duration elapsedSince(long mark) { return Duration.ofNanos(Math.max(0L, nanos.getAsLong() - mark)); }
}
