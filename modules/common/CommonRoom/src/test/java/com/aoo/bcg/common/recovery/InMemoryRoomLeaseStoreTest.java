package com.aoo.bcg.common.recovery;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryRoomLeaseStoreTest {
    @Test void rejectsConcurrentOwnerAndIssuesIncreasingTokens() {
        InMemoryRoomLeaseStore store = new InMemoryRoomLeaseStore(Clock.systemUTC());
        RoomLease first = store.acquire(1, "node-a", Duration.ofMinutes(1));
        assertTrue(store.isCurrent(first));
        assertThrows(IllegalStateException.class, () -> store.acquire(1, "node-b", Duration.ofMinutes(1)));
        RoomLease renewed = store.acquire(1, "node-a", Duration.ofMinutes(1));
        assertTrue(renewed.fencingToken() > first.fencingToken());
        assertFalse(store.isCurrent(first));
    }

    @Test void expiredLeaseMovesToAnotherNodeWithHigherFence() {
        MutableClock clock = new MutableClock();
        InMemoryRoomLeaseStore store = new InMemoryRoomLeaseStore(clock);
        RoomLease first = store.acquire(7, "node-a", Duration.ofSeconds(5));
        clock.advance(Duration.ofSeconds(6));
        RoomLease moved = store.acquire(7, "node-b", Duration.ofSeconds(5));
        assertFalse(store.isCurrent(first));
        assertTrue(store.isCurrent(moved));
        assertTrue(moved.fencingToken() > first.fencingToken());
    }

    @Test void releaseIsFencedAndInvalidLeasesFailClosed() {
        InMemoryRoomLeaseStore store = new InMemoryRoomLeaseStore(Clock.systemUTC());
        RoomLease first=store.acquire(3,"node-a",Duration.ofMinutes(1));
        RoomLease renewed=store.acquire(3,"node-a",Duration.ofMinutes(1));
        store.release(first);
        assertTrue(store.isCurrent(renewed));
        store.release(renewed);
        assertFalse(store.isCurrent(renewed));
        assertThrows(IllegalArgumentException.class,()->store.acquire(0,"node",Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,()->store.acquire(1,"node",null));
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-08-22T00:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
