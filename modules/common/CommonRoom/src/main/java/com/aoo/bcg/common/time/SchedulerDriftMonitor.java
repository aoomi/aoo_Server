package com.aoo.bcg.common.time;
import java.time.Duration;import java.time.Instant;import java.util.concurrent.atomic.AtomicLong;
/** Bounded-allocation scheduler drift metric suitable for high-frequency timer execution. */
public final class SchedulerDriftMonitor{
 public record Snapshot(long samples,long breaches,long maxLateMillis,long totalLateMillis,long budgetMillis){}
 private final long budgetMillis;private final AtomicLong samples=new AtomicLong(),breaches=new AtomicLong(),maxLate=new AtomicLong(),totalLate=new AtomicLong();
 public SchedulerDriftMonitor(Duration budget){if(budget==null||budget.isNegative())throw new IllegalArgumentException("invalid drift budget");budgetMillis=budget.toMillis();}
 public long record(Instant expected,Instant actual){long late=Math.max(0L,Duration.between(expected,actual).toMillis());samples.incrementAndGet();totalLate.addAndGet(late);maxLate.accumulateAndGet(late,Math::max);if(late>budgetMillis)breaches.incrementAndGet();return late;}
 public Snapshot snapshot(){return new Snapshot(samples.get(),breaches.get(),maxLate.get(),totalLate.get(),budgetMillis);}
}
