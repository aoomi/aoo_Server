package com.aoo.bcg.common.repair;
import java.time.Duration;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;import java.util.function.Consumer;
/** Batch repair runner isolated from live-game pools, with QPS control and resumable checkpoints. */
public final class ThrottledRepairRunner<T>{
 public record Checkpoint(long offset,long succeeded,long failed,boolean paused){}
 private final int batchSize;private final long intervalNanos;private final String resourcePool;private final AtomicBoolean pause=new AtomicBoolean();
 public ThrottledRepairRunner(int batchSize,int maxQps,String resourcePool){if(batchSize<1||maxQps<1)throw new IllegalArgumentException("positive limits required");if(resourcePool==null||resourcePool.isBlank()||resourcePool.equals("live-game"))throw new IllegalArgumentException("dedicated repair resource pool required");this.batchSize=batchSize;intervalNanos=1_000_000_000L/maxQps;this.resourcePool=resourcePool;}
 public void pause(){pause.set(true);}public void resume(){pause.set(false);}public String resourcePool(){return resourcePool;}
 public Checkpoint run(List<T> items,Checkpoint from,Consumer<T> mutation,Duration sleepCap)throws InterruptedException{Objects.requireNonNull(items);Objects.requireNonNull(from);Objects.requireNonNull(mutation);long offset=from.offset(),ok=from.succeeded(),failed=from.failed();while(offset<items.size()&&!pause.get()){long end=Math.min(offset+batchSize,items.size());for(;offset<end&&!pause.get();offset++){long started=System.nanoTime();try{mutation.accept(items.get((int)offset));ok++;}catch(RuntimeException failure){failed++;}long wait=Math.min(Math.max(0,intervalNanos-(System.nanoTime()-started)),sleepCap.toNanos());if(wait>0)Thread.sleep(Duration.ofNanos(wait));}}return new Checkpoint(offset,ok,failed,pause.get());}
}
