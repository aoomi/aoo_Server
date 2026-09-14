package com.aoo.bcg.common.time;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
class UniqueScheduledTaskRegistryTest {
    @Test void sameOwnerCannotProliferateAndCloseCancels() {
        var executor=Executors.newSingleThreadScheduledExecutor(); var created=new AtomicInteger();
        var registry=new UniqueScheduledTaskRegistry();
        var first=registry.register("outbox",()->{created.incrementAndGet();return executor.scheduleWithFixedDelay(()->{},1,1,TimeUnit.DAYS);});
        var second=registry.register("outbox",()->{created.incrementAndGet();return executor.schedule(()->{},1,TimeUnit.DAYS);});
        assertSame(first,second); assertEquals(1,created.get()); assertEquals(1,registry.activeCount());
        registry.close(); assertTrue(first.isCancelled()); executor.shutdownNow();
    }
}
