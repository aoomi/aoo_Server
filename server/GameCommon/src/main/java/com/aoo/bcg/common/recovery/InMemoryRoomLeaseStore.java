package com.aoo.bcg.common.recovery;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
public final class InMemoryRoomLeaseStore implements RoomLeaseStore {
    private final ConcurrentHashMap<Long, RoomLease> leases = new ConcurrentHashMap<>();
    private final AtomicLong tokens = new AtomicLong();
    private final Clock clock;
    public InMemoryRoomLeaseStore(Clock clock) { this.clock = java.util.Objects.requireNonNull(clock, "clock"); }
    @Override public RoomLease acquire(long roomId, String nodeId, Duration ttl) {
        if (roomId <= 0 || nodeId == null || nodeId.isBlank() || ttl == null
                || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("invalid room lease");
        return leases.compute(roomId, (id, current) -> {
            if (current != null && current.expiresAt().isAfter(clock.instant()) && !current.ownerNode().equals(nodeId)) throw new IllegalStateException("room lease held by another node");
            long token = tokens.updateAndGet(previous -> Math.incrementExact(previous));
            return new RoomLease(roomId, nodeId, token, clock.instant().plus(ttl));
        });
    }
    @Override public boolean isCurrent(RoomLease lease) { if (lease == null) return false; RoomLease current = leases.get(lease.roomId()); return lease.equals(current) && current.expiresAt().isAfter(clock.instant()); }
    @Override public void release(RoomLease lease) { if (lease != null) leases.remove(lease.roomId(), lease); }
}
