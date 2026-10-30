package com.aoo.bcg.common.resource;

import java.lang.ref.ReferenceQueue;import java.lang.ref.WeakReference;import java.time.Duration;import java.util.ArrayList;import java.util.List;import java.util.function.Supplier;

/** Fixed-cycle leak probe used beside production heap-dump comparison. */
public final class HeapReclaimVerifier{
 public interface CycleResource extends AutoCloseable{}
 public record Report(int cycles,int reclaimed,int retained){public boolean passed(){return retained==0;}}
 private HeapReclaimVerifier(){}
 public static Report verify(Supplier<? extends CycleResource> completeRoomCycle,int cycles,Duration timeout){if(cycles<1||timeout.isNegative()||timeout.isZero())throw new IllegalArgumentException("invalid heap reclaim probe");var queue=new ReferenceQueue<CycleResource>();List<WeakReference<CycleResource>> references=new ArrayList<>();for(int index=0;index<cycles;index++)createCloseAndForget(completeRoomCycle,queue,references);long deadline=System.nanoTime()+timeout.toNanos();int reclaimed=0;while(reclaimed<cycles&&System.nanoTime()<deadline){System.gc();System.runFinalization();while(queue.poll()!=null)reclaimed++;if(reclaimed<cycles)try{Thread.sleep(10);}catch(InterruptedException error){Thread.currentThread().interrupt();break;}}return new Report(cycles,reclaimed,cycles-reclaimed);}
 private static void createCloseAndForget(Supplier<? extends CycleResource> supplier,ReferenceQueue<CycleResource> queue,List<WeakReference<CycleResource>> references){CycleResource value=supplier.get();references.add(new WeakReference<>(value,queue));try{value.close();}catch(Exception error){throw new IllegalStateException("room cycle cleanup failed",error);}}
}
