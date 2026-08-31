package com.aoo.bcg.common.loop;
import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.common.event.CausalDispatchGuard;import com.aoo.bcg.common.retry.ControlledRetry;import com.aoo.bcg.gamespi.*;
import java.time.Duration;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;import org.junit.jupiter.api.Test;
class CpuRecoveryFaultInjectionTest{
 @Test void malformedWorkCyclesDuplicatesAndRetriesFailWithoutPoisoningWorker()throws Exception{
  assertTimeoutPreemptively(Duration.ofSeconds(2),()->{try(var worker=Executors.newSingleThreadExecutor()){
   assertThrows(ExecutionException.class,()->worker.submit(()->{var budget=new RuntimeWorkBudget(10,4,4,64,Duration.ofMillis(100));for(int i=0;i<11;i++)budget.step();}).get());
   assertThrows(ExecutionException.class,()->worker.submit(()->new AcyclicDependencyGraph<>(List.of(new AcyclicDependencyGraph.Node<>("a",Set.of("b")),new AcyclicDependencyGraph.Node<>("b",Set.of("a"))))).get());
   assertThrows(ExecutionException.class,()->worker.submit(()->{var guard=new CausalDispatchGuard(4);guard.dispatch("network","duplicate",()->guard.dispatch("network","duplicate",()->{}));}).get());
   var calls=new AtomicInteger();var retry=new ControlledRetry(new ControlledRetry.Policy(3,Duration.ofNanos(1),Duration.ofNanos(2),Duration.ofMillis(10),0.2),duration->{},(base,jitter)->base);
   assertThrows(ExecutionException.class,()->worker.submit(()->retry.execute("fault",()->{calls.incrementAndGet();throw new java.io.IOException("fault");},error->true)).get());assertEquals(3,calls.get());
   assertEquals("worker-recovered",worker.submit(()->"worker-recovered").get());
  }});
 }
}
