package com.aoo.bcg.gamespi.time;
import java.time.Instant;import java.util.concurrent.atomic.AtomicLong;import java.util.concurrent.atomic.AtomicReference;
/** Detects wall-clock rollback and clamps authority time to the last observed instant. */
public final class ClockRollbackMonitor{
 private final AtomicReference<Instant> highWatermark=new AtomicReference<>(Instant.MIN);private final AtomicLong rollbackCount=new AtomicLong();
 public Instant observe(Instant value){for(int attempt=0;attempt<1_024;attempt++){Instant high=highWatermark.get();if(value.isBefore(high)){rollbackCount.incrementAndGet();return high;}if(highWatermark.compareAndSet(high,value))return value;Thread.onSpinWait();}throw new IllegalStateException("clock high-watermark contention budget exceeded");}
 public long rollbackCount(){return rollbackCount.get();}
}
