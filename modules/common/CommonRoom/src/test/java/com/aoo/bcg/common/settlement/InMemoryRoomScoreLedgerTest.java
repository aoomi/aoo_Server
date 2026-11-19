package com.aoo.bcg.common.settlement;

import com.aoo.bcg.gamespi.RoomScoreLedger;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryRoomScoreLedgerTest {
    @Test void commitIsAtomicZeroSumAndRetryIsIdempotent() {
        var ledger = new InMemoryRoomScoreLedger(); ledger.seed(7, Map.of(10L, 30L, 20L, 10L));
        var snapshot = ledger.snapshot(7, Map.of(10L, 0L, 20L, 0L).keySet());
        var command = new RoomScoreLedger.Command("round:7:1", 7, 1, snapshot.revision(),
                snapshot.balances(), Map.of(10L, -10L, 20L, 10L));
        var first = ledger.commit(command); var retry = ledger.commit(command);
        assertEquals(Map.of(10L, 20L, 20L, 20L), first.balances());
        assertFalse(first.replayed()); assertTrue(retry.replayed());
        assertEquals(first.balances(), retry.balances());
    }

    @Test void staleSnapshotAndInsufficientBalanceHaveNoPartialWrite() {
        var ledger = new InMemoryRoomScoreLedger(); ledger.seed(8, Map.of(10L, 5L, 20L, 5L));
        var snapshot = ledger.snapshot(8, java.util.List.of(10L, 20L));
        assertThrows(IllegalStateException.class, () -> ledger.commit(new RoomScoreLedger.Command(
                "round:8:1", 8, 1, snapshot.revision(), snapshot.balances(), Map.of(10L, -6L, 20L, 6L))));
        assertEquals(snapshot, ledger.snapshot(8, java.util.List.of(10L, 20L)));
    }

    @Test void concurrentDifferentOperationsAllowOnlyOneExpectedRevision() throws Exception {
        var ledger = new InMemoryRoomScoreLedger(); ledger.seed(9, Map.of(10L, 10L, 20L, 10L));
        var snapshot = ledger.snapshot(9, java.util.List.of(10L, 20L));
        var ready = new CountDownLatch(2); var go = new CountDownLatch(1); var success = new AtomicInteger();
        Runnable task = () -> { ready.countDown(); try { go.await(); ledger.commit(new RoomScoreLedger.Command(
                "round:9:" + Thread.currentThread().getName(), 9, 1, snapshot.revision(), snapshot.balances(),
                Map.of(10L, -1L, 20L, 1L))); success.incrementAndGet(); } catch (Exception ignored) {} };
        Thread a = new Thread(task, "a"), b = new Thread(task, "b"); a.start(); b.start(); ready.await(); go.countDown(); a.join(); b.join();
        assertEquals(1, success.get()); assertEquals(1, ledger.snapshot(9, java.util.List.of(10L, 20L)).revision());
    }
}
