package com.aoo.bcg.gamespi.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperationDeadlineArbiterTest {
    @Test void playerBeforeDeadlineWinsExactlyOnce() {
        Instant now=Instant.parse("2026-08-23T00:00:00Z"); var time=time(now); var window=OperationDeadline.open("round-1-seat-2",2,Duration.ofSeconds(1),time);
        var effects=new AtomicInteger();var arbiter=new OperationDeadlineArbiter();
        assertEquals("ok",arbiter.resolvePlayer(window,"r1",time,()->{effects.incrementAndGet();return "ok";}).value());
        assertEquals(OperationDeadlineArbiter.Outcome.PLAYER,arbiter.resolveTimeout(window,time(now.plusSeconds(2)),effects::incrementAndGet).outcome());
        assertEquals(1,effects.get());
    }
    @Test void exactDeadlineBelongsToTimeoutAndNeverRunsPlayerMutation() {
        Instant now=Instant.parse("2026-08-23T00:00:00Z");var window=OperationDeadline.open("round-2-seat-1",1,Duration.ofSeconds(1),time(now));var effects=new AtomicInteger();var arbiter=new OperationDeadlineArbiter();
        assertThrows(IllegalStateException.class,()->arbiter.resolvePlayer(window,"r2",time(now.plusSeconds(1)),()->{effects.incrementAndGet();return "bad";}));
        assertEquals(OperationDeadlineArbiter.Outcome.TIMEOUT,arbiter.resolveTimeout(window,time(now.plusSeconds(1)),effects::incrementAndGet).outcome());
        assertEquals(0,effects.get());
    }
    private static AuthoritativeTimeSource time(Instant instant){return new AuthoritativeTimeSource(Clock.fixed(instant,ZoneOffset.UTC));}
}
