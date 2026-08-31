package com.aoo.bcg.common.recovery;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryRoomSnapshotStore implements RoomSnapshotStore {
    private final ConcurrentHashMap<Long, RoomSnapshot> snapshots = new ConcurrentHashMap<>();
    @Override public void save(RoomSnapshot snapshot) { snapshots.compute(snapshot.roomId(), (id, previous) -> { if (previous != null && snapshot.lastEventSequence() < previous.lastEventSequence()) throw new IllegalStateException("snapshot sequence regression"); return snapshot; }); }
    @Override public Optional<RoomSnapshot> latest(long roomId) { return Optional.ofNullable(snapshots.get(roomId)); }
}
