package com.aoo.bcg.common.concurrency;
import org.junit.jupiter.api.Test;
import java.time.Duration;import java.util.Map;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class IsolatedWorkloadExecutorsTest {
 @Test void saturatedBackgroundCannotStarveGameplay()throws Exception{
  var one=new IsolatedWorkloadExecutors.PoolBudget(1,1,Duration.ofMillis(20));
  try(var pools=new IsolatedWorkloadExecutors(Map.of(IsolatedWorkloadExecutors.Workload.GAMEPLAY,one,IsolatedWorkloadExecutors.Workload.CHAT,one,IsolatedWorkloadExecutors.Workload.REPLAY,one,IsolatedWorkloadExecutors.Workload.BACKGROUND,one))){
   var hold=new CountDownLatch(1);var started=new CountDownLatch(1);
   pools.submit(IsolatedWorkloadExecutors.Workload.BACKGROUND,()->{started.countDown();hold.await();return 1;});started.await();
   pools.submit(IsolatedWorkloadExecutors.Workload.BACKGROUND,()->2);
   assertThrows(ExecutionException.class,()->pools.submit(IsolatedWorkloadExecutors.Workload.BACKGROUND,()->3).get());
   assertEquals(7,pools.submit(IsolatedWorkloadExecutors.Workload.GAMEPLAY,()->7).get(200,TimeUnit.MILLISECONDS));hold.countDown();
  }
 }
}
