package com.aoo.bcg.common.transaction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomAccountingSagaTest {
    private final RoomAccountingSagaId id = new RoomAccountingSagaId(7, 2, "rules-v3", "settle", "settle-7-2");

    @Test void completesEveryStepExactlyOnceAcrossDuplicateDelivery() {
        var repository = new InMemoryRoomAccountingSagaRepository();
        var saga = new RoomAccountingSaga(repository);
        var game = new AtomicInteger(); var accounting = new AtomicInteger(); var finished = new AtomicInteger();
        assertEquals(RoomAccountingSagaState.COMPLETED, saga.execute(id, game::incrementAndGet,
                accounting::incrementAndGet, finished::incrementAndGet, () -> fail("must not compensate")));
        assertEquals(RoomAccountingSagaState.COMPLETED, saga.execute(id, game::incrementAndGet,
                accounting::incrementAndGet, finished::incrementAndGet, () -> fail("must not compensate")));
        assertEquals(1, game.get()); assertEquals(1, accounting.get()); assertEquals(1, finished.get());
    }

    @Test void accountingFailureCompensatesAndFinalizationFailureCanResume() {
        var compensatedRepository = new InMemoryRoomAccountingSagaRepository();
        var compensated = new RoomAccountingSaga(compensatedRepository);
        var compensation = new AtomicInteger();
        RoomAccountingSaga.ExecutionFailure failure = assertThrows(RoomAccountingSaga.ExecutionFailure.class,
                () -> compensated.execute(id, () -> { }, () -> { throw new IllegalStateException("billing down"); },
                        () -> fail("must not finalize"), compensation::incrementAndGet));
        assertEquals(RoomAccountingSagaState.COMPENSATED, failure.state());
        assertEquals(1, compensation.get());

        var resumableRepository = new InMemoryRoomAccountingSagaRepository();
        var resumable = new RoomAccountingSaga(resumableRepository);
        var failOnce = new AtomicBoolean(true); var game = new AtomicInteger(); var accounting = new AtomicInteger();
        assertThrows(RoomAccountingSaga.ExecutionFailure.class,
                () -> resumable.execute(id, game::incrementAndGet, accounting::incrementAndGet,
                        () -> { if (failOnce.getAndSet(false)) throw new IllegalStateException("event store down"); },
                        () -> fail("must not compensate")));
        assertEquals(RoomAccountingSagaState.COMPLETED,
                resumable.execute(id, game::incrementAndGet, accounting::incrementAndGet,
                        () -> { }, () -> fail("must not compensate")));
        assertEquals(1, game.get()); assertEquals(1, accounting.get());
    }
}
