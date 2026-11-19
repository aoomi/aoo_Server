package com.aoo.bcg.common.recovery;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RoomRecoveryServiceTest {
    @Test void acquiresHigherFenceAndReplaysOnlyMissingEvents() {
        InMemoryRoomSnapshotStore snapshots = new InMemoryRoomSnapshotStore();
        snapshots.save(new RoomSnapshot(9, 516, "v1", "c1", 0, 4, Instant.now(), Map.of("score", 10)));
        InMemoryRoomLeaseStore leases = new InMemoryRoomLeaseStore(Clock.systemUTC());
        IncrementalEventSource<Integer> events = (roomId, sequence) -> List.of(2, 3);
        RoomStateRestorer<Integer, Integer> restorer = new RoomStateRestorer<>() {
            public Integer fromSnapshot(RoomSnapshot snapshot) { return (Integer) snapshot.authoritativeState().get("score"); }
            public Integer replay(Integer state, List<Integer> values) { return state + values.stream().mapToInt(Integer::intValue).sum(); }
            public String digest(Integer state) { return Integer.toHexString(state); }
        };
        RecoveredRoom<Integer> recovered = new RoomRecoveryService<>(snapshots, leases, events, restorer).recover(9, "node-b", Duration.ofMinutes(1));
        assertEquals(15, recovered.state());
        assertTrue(recovered.lease().fencingToken() > recovered.sourceSnapshot().fencingToken());
    }

    @Test void releasesFencedLeaseWhenRecoveryFailsClosed() {
        InMemoryRoomLeaseStore leases = new InMemoryRoomLeaseStore(Clock.systemUTC());
        RoomSnapshotStore missing = new RoomSnapshotStore() {
            public void save(RoomSnapshot snapshot) { throw new AssertionError("unexpected save"); }
            public java.util.Optional<RoomSnapshot> latest(long roomId) { return java.util.Optional.empty(); }
        };
        RoomStateRestorer<Integer, Integer> restorer = new RoomStateRestorer<>() {
            public Integer fromSnapshot(RoomSnapshot snapshot) { return 0; }
            public Integer replay(Integer state, List<Integer> values) { return state; }
            public String digest(Integer state) { return "0"; }
        };
        RoomRecoveryService<Integer,Integer> service = new RoomRecoveryService<>(missing, leases,
                (roomId, sequence) -> List.of(), restorer);
        assertThrows(IllegalStateException.class, () -> service.recover(17,"node-a",Duration.ofMinutes(1)));
        RoomLease replacement=leases.acquire(17,"node-b",Duration.ofMinutes(1));
        assertTrue(leases.isCurrent(replacement));
    }
}
