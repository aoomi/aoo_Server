package com.aoo.bcg.gamespi.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthoritativeTimeSourceTest {
    @Test void fixedClockMakesDeadlinesDeterministic() {
        Instant now = Instant.parse("2026-08-23T00:00:00Z");
        var time = new AuthoritativeTimeSource(Clock.fixed(now, ZoneOffset.UTC));
        assertEquals(now, time.now());
        assertEquals(now.plusSeconds(20), time.deadlineAfter(Duration.ofSeconds(20)));
        assertEquals(Duration.ofSeconds(20), time.remainingUntil(now.plusSeconds(20)));
        assertThrows(IllegalArgumentException.class, () -> time.deadlineAfter(Duration.ofMillis(-1)));
    }

    @Test void elapsedMetricsUseInjectableMonotonicTicker() {
        long[] values = { 10L, 25L };
        int[] index = { 0 };
        var ticker = new MonotonicTicker(() -> values[index[0]++]);
        long mark = ticker.mark();
        assertEquals(Duration.ofNanos(15), ticker.elapsedSince(mark));
    }
}
