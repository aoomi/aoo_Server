package com.aoo.bcg.common.recovery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface RoomSnapshotStore {
    void save(RoomSnapshot snapshot);
    Optional<RoomSnapshot> latest(long roomId);
    default List<RoomSnapshot> recoverable(Instant now, int limit) { return List.of(); }
}
