package com.aoo.bcg.gamespi.time;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class UnlimitedOperationDeadlineTest {
    private static AuthoritativeTimeSource at(String instant) {
        return new AuthoritativeTimeSource(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test void unlimitedWindowPersistsAndAcceptsPlayerWithoutEverTimingOut() {
        OperationDeadline window = OperationDeadline.unlimited("round-1-seat-0", 0);
        assertTrue(window.open());
        assertTrue(window.unlimited());
        assertFalse(window.expired(at("2099-01-01T00:00:00Z")));
        assertEquals(window, OperationDeadline.from(window.toMap()));
        assertEquals(true, window.toMap().get("unlimited"));

        AtomicInteger effects = new AtomicInteger();
        OperationDeadlineArbiter arbiter = new OperationDeadlineArbiter();
        assertThrows(IllegalStateException.class,
                () -> arbiter.resolveTimeout(window, at("2099-01-01T00:00:00Z"), effects::incrementAndGet));
        assertEquals(0, effects.get());
        assertEquals("played", arbiter.resolvePlayer(window, "request-1", at("2099-01-01T00:00:00Z"),
                () -> { effects.incrementAndGet(); return "played"; }).value());
        assertEquals(1, effects.get());
        assertThrows(IllegalStateException.class,
                () -> arbiter.resolvePlayer(window, "request-2", at("2100-01-01T00:00:00Z"),
                        () -> { effects.incrementAndGet(); return "duplicate"; }));
        assertEquals(1, effects.get());
    }

    @Test void finiteAndClosedWindowsRetainTheirPreviousContract() {
        OperationDeadline closed = OperationDeadline.none();
        assertFalse(closed.open());
        assertFalse(closed.unlimited());
        assertEquals(closed, OperationDeadline.from(closed.toMap()));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationDeadlineArbiter().resolvePlayer(closed, "request", at("2026-01-01T00:00:00Z"), () -> "bad"));

        AuthoritativeTimeSource start = at("2026-01-01T00:00:00Z");
        OperationDeadline finite = OperationDeadline.open("play", 1, Duration.ofSeconds(1), start);
        assertFalse(finite.unlimited());
        assertFalse(finite.toMap().containsKey("unlimited"));
        assertEquals(finite, OperationDeadline.from(finite.toMap()));
        assertTrue(finite.expired(at("2026-01-01T00:00:01Z")));
        assertThrows(IllegalStateException.class,
                () -> new OperationDeadlineArbiter().resolvePlayer(finite, "late", at("2026-01-01T00:00:01Z"), () -> "bad"));
        assertThrows(IllegalArgumentException.class,
                () -> OperationDeadline.from(Map.of("operationId", "play", "seatId", 1,
                        "deadlineEpochMillis", 1L, "unlimited", true)));
    }
}
