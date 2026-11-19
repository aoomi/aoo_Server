package com.aoo.bcg.common.resource;
import static org.junit.jupiter.api.Assertions.*;import java.util.concurrent.atomic.AtomicLong;import org.junit.jupiter.api.Test;
class LeakRegressionGateTest{
 @Test void fixedCyclesAcceptBoundedGrowthAcrossAllResourceKinds(){var cycle=new AtomicLong();var report=LeakRegressionGate.run(cycle::incrementAndGet,()->{long n=cycle.get();return new LeakRegressionGate.Sample(100+n,200+n,10,20);},new LeakRegressionGate.Budget(2,5,4,4,0,0));assertTrue(report.passed());assertEquals(7,cycle.get());}
 @Test void persistentListenerGrowthFailsGate(){var cycle=new AtomicLong();var report=LeakRegressionGate.run(cycle::incrementAndGet,()->new LeakRegressionGate.Sample(100,200,10,cycle.get()),new LeakRegressionGate.Budget(0,4,0,0,0,0));assertEquals(java.util.List.of("listener"),report.violations());assertThrows(IllegalStateException.class,report::requirePassed);}
}
