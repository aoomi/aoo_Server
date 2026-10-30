package com.aoo.bcg.common.time;
import com.aoo.bcg.gamespi.time.*;import java.time.*;import java.util.concurrent.atomic.AtomicInteger;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class ClockRollbackSafetyTest{
 @Test void rollbackIsCountedClampedAndCannotRepeatEffect(){Instant[]values={Instant.ofEpochSecond(100),Instant.ofEpochSecond(90),Instant.ofEpochSecond(101)};var index=new AtomicInteger();Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return values[Math.min(index.getAndIncrement(),values.length-1)];}};var time=new AuthoritativeTimeSource(clock);var gate=new TemporalEffectGate(time);assertTrue(gate.acquire("room:1:settle",Duration.ofMinutes(1)));assertFalse(gate.acquire("room:1:settle",Duration.ofMinutes(1)));assertEquals(1,time.clockRollbackCount());assertFalse(gate.acquire("room:1:settle",Duration.ofMinutes(1)));}
}
