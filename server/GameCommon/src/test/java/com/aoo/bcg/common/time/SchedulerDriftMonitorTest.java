package com.aoo.bcg.common.time;
import java.time.*;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class SchedulerDriftMonitorTest{
 @Test void recordsBudgetBreachesAndSupportsLongRunWithoutSampleRetention(){var monitor=new SchedulerDriftMonitor(Duration.ofMillis(50));Instant base=Instant.parse("2026-08-23T00:00:00Z");for(int i=0;i<100_000;i++)monitor.record(base,base.plusMillis(i%100));var snapshot=monitor.snapshot();assertEquals(100_000,snapshot.samples());assertEquals(49_000,snapshot.breaches());assertEquals(99,snapshot.maxLateMillis());assertEquals(50,snapshot.budgetMillis());}
}
