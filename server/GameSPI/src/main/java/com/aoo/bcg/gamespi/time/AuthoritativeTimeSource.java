package com.aoo.bcg.gamespi.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Injectable wall-clock authority for persisted state, protocol deadlines and audit timestamps. */
public final class AuthoritativeTimeSource {
    private final Clock clock;
    private final ClockRollbackMonitor rollbackMonitor;

    public AuthoritativeTimeSource(Clock clock) {
        this(clock,new ClockRollbackMonitor());
    }
    public AuthoritativeTimeSource(Clock clock,ClockRollbackMonitor rollbackMonitor) {
        this.clock=Objects.requireNonNull(clock,"clock");this.rollbackMonitor=Objects.requireNonNull(rollbackMonitor,"rollbackMonitor");
    }

    public static AuthoritativeTimeSource systemUtc() {
        return new AuthoritativeTimeSource(Clock.systemUTC());
    }

    public Instant now() { return rollbackMonitor.observe(clock.instant()); }
    public long clockRollbackCount(){return rollbackMonitor.rollbackCount();}
    public long epochMillis() { return now().toEpochMilli(); }

    public Instant deadlineAfter(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("duration must not be negative");
        return now().plus(duration);
    }

    public Duration remainingUntil(Instant deadline) {
        Objects.requireNonNull(deadline, "deadline");
        Duration remaining = Duration.between(now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
