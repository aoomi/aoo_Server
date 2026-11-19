package BaseTask;

import BaseTask.AsynTask.AsyncTaskQueue;
import BaseTask.SyncTask.SyncTaskQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskQueueSafetyTest {
    @Test void asyncQueueIsBoundedAndDisposeWakesDealer() throws Exception {
        AsyncTaskQueue queue = new AsyncTaskQueue("bounded-async", false, 1, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            queue.regAsynTask(() -> { started.countDown(); await(release); return 1; }, null);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            queue.regAsynTask(() -> 2, null);
            assertThrows(RejectedExecutionException.class, () -> queue.regAsynTask(() -> 3, null));
        } finally {
            release.countDown();
            queue.dispose();
        }
        assertThrows(RejectedExecutionException.class, () -> queue.regAsynTask(() -> 4, null));
    }

    @Test void syncQueueIsBoundedAndDisposeStopsTimerAndDealers() throws Exception {
        SyncTaskQueue queue = new SyncTaskQueue("bounded-sync", 1, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            queue.RegisterTask(() -> { started.countDown(); await(release); });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            queue.RegisterTask(() -> { });
            assertThrows(RejectedExecutionException.class, () -> queue.RegisterTask(() -> { }));
        } finally {
            release.countDown();
            queue.dispose();
        }
        assertThrows(RejectedExecutionException.class, () -> queue.RegisterTask(() -> { }));
        assertThrows(RejectedExecutionException.class, () -> queue.RegisterTask(() -> { }, 1L));
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
