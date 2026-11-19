package com.aoo.bcg.common.recovery;
import java.time.Duration;
public interface RoomLeaseStore {
    RoomLease acquire(long roomId, String nodeId, Duration ttl);
    boolean isCurrent(RoomLease lease);
    /** Releases only the exact fenced lease; a stale owner can never delete its successor. */
    void release(RoomLease lease);
}
