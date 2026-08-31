package com.aoo.bcg.gamespi.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperationDeadlineTest {
    @Test void deadlineIsAbsolutePersistableAndServerAuthoritative() {
        Instant now = Instant.parse("2026-08-23T08:00:00Z");
        var time = new AuthoritativeTimeSource(Clock.fixed(now, ZoneOffset.UTC));
        var deadline = OperationDeadline.open("play", 2, Duration.ofSeconds(20), time);
        assertEquals(now.plusSeconds(20), deadline.deadline());
        assertFalse(deadline.expired(time));
        var expiredTime = new AuthoritativeTimeSource(Clock.fixed(deadline.deadline(), ZoneOffset.UTC));
        assertTrue(deadline.expired(expiredTime));
        assertFalse(OperationDeadline.none().open());
    }
}
