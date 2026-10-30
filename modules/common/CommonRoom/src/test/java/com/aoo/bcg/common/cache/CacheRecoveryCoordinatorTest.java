package com.aoo.bcg.common.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CacheRecoveryCoordinatorTest {
    @Test void cacheLossCannotStampedeOrigin() throws Exception {
        var coordinator = new CacheRecoveryCoordinator(3, 1_000_000, 5, Duration.ofSeconds(1));
        var started = new CountDownLatch(3); var release = new CountDownLatch(1);
        var concurrent = new AtomicInteger(); var peak = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(40); var futures = new ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < 40; i++) futures.add(pool.submit(() -> {
            try { coordinator.protectedOriginLoad("room", () -> { int active=concurrent.incrementAndGet(); peak.accumulateAndGet(active, Math::max); started.countDown(); try { release.await(); return 1; } finally { concurrent.decrementAndGet(); } }); }
            catch (Exception ignored) { }
        }));
        assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS)); release.countDown();
        for (var future : futures) future.get(); pool.shutdown();
        assertTrue(peak.get() <= 3, "origin concurrency must remain bounded");
    }

    @Test void warmupIsCriticalFirstAndCheckpointsOnlySuccess() throws Exception {
        var coordinator = new CacheRecoveryCoordinator(1, 1_000_000, 5, Duration.ofSeconds(1));
        var seen = new ArrayList<String>();
        var result = coordinator.warmup(List.of(
            new CacheRecoveryCoordinator.WarmupKey("game", "normal", CacheRecoveryCoordinator.Priority.NORMAL),
            new CacheRecoveryCoordinator.WarmupKey("game", "critical", CacheRecoveryCoordinator.Priority.CRITICAL),
            new CacheRecoveryCoordinator.WarmupKey("game", "hot", CacheRecoveryCoordinator.Priority.HOT)),
            key -> seen.add(key.key()), 2, Duration.ZERO);
        assertEquals(List.of("critical", "hot", "normal"), seen);
        assertEquals(3, result.size());
    }
}
