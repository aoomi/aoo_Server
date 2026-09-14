package com.aoo.bcg.gamespi;
import java.time.Duration;import java.util.concurrent.atomic.AtomicLong;
/** Shared fail-closed runtime budget for algorithms, queues and transient memory. */
public final class RuntimeWorkBudget{
 private final long maxSteps,maxQueueItems,maxBytes;private final int maxDepth;private final long deadlineNanos;private final AtomicLong steps=new AtomicLong(),queueItems=new AtomicLong(),bytes=new AtomicLong();
 public RuntimeWorkBudget(long maxSteps,int maxDepth,long maxQueueItems,long maxBytes,Duration elapsed){if(maxSteps<1||maxDepth<1||maxDepth>1024||maxQueueItems<1||maxBytes<1||elapsed==null||elapsed.isNegative()||elapsed.isZero())throw new IllegalArgumentException("invalid runtime work budget");this.maxSteps=maxSteps;this.maxDepth=maxDepth;this.maxQueueItems=maxQueueItems;this.maxBytes=maxBytes;this.deadlineNanos=Math.addExact(System.nanoTime(),elapsed.toNanos());}
 public void step(){if(steps.incrementAndGet()>maxSteps)throw exceeded("steps");checkTime();}
 public void depth(int depth){if(depth<0||depth>maxDepth)throw exceeded("depth");checkTime();}
 public void enqueue(long count,long estimatedBytes){if(count<0||estimatedBytes<0||queueItems.addAndGet(count)>maxQueueItems||bytes.addAndGet(estimatedBytes)>maxBytes)throw exceeded("queue or memory");checkTime();}
 public void dequeue(long count,long estimatedBytes){if(count<0||estimatedBytes<0||queueItems.addAndGet(-count)<0||bytes.addAndGet(-estimatedBytes)<0)throw new IllegalStateException("runtime budget accounting underflow");}
 public void checkTime(){if(System.nanoTime()>deadlineNanos)throw exceeded("elapsed time");}
 public Snapshot snapshot(){return new Snapshot(steps.get(),queueItems.get(),bytes.get());}
 private static BudgetExceededException exceeded(String dimension){return new BudgetExceededException(dimension+" budget exceeded");}
 public record Snapshot(long steps,long queueItems,long bytes){}
 public static final class BudgetExceededException extends RuntimeException{public BudgetExceededException(String message){super(message);}}
}
