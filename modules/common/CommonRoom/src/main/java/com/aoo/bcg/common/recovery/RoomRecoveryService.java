package com.aoo.bcg.common.recovery;

import java.time.Duration;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class RoomRecoveryService<S, E> {
    private final RoomSnapshotStore snapshots;
    private final RoomLeaseStore leases;
    private final IncrementalEventSource<E> events;
    private final RoomStateRestorer<S, E> restorer;
    private final Clock clock;
    public RoomRecoveryService(RoomSnapshotStore snapshots, RoomLeaseStore leases,
                               IncrementalEventSource<E> events, RoomStateRestorer<S, E> restorer) {
        this(snapshots,leases,events,restorer,Clock.systemUTC());
    }
    public RoomRecoveryService(RoomSnapshotStore snapshots, RoomLeaseStore leases,
                               IncrementalEventSource<E> events, RoomStateRestorer<S,E> restorer, Clock clock) {
        this.snapshots=Objects.requireNonNull(snapshots,"snapshots");this.leases=Objects.requireNonNull(leases,"leases");this.events=Objects.requireNonNull(events,"events");this.restorer=Objects.requireNonNull(restorer,"restorer");this.clock=Objects.requireNonNull(clock,"clock");
    }
    public RecoveredRoom<S> recover(long roomId, String nodeId, Duration leaseTtl) {
        RoomLease lease = leases.acquire(roomId, nodeId, leaseTtl);
        try {
            RoomSnapshot snapshot = snapshots.latest(roomId).orElseThrow(() -> new IllegalStateException("room snapshot not found"));
            if(snapshot.stateVersion()!=snapshot.lastEventSequence())throw new IllegalStateException("snapshot state/event version mismatch");
            if (snapshot.fencingToken() >= lease.fencingToken()) throw new IllegalStateException("new lease must have a higher fencing token");
            S state = restorer.fromSnapshot(snapshot);
            List<E> missing = List.copyOf(Objects.requireNonNull(events.after(roomId, snapshot.lastEventSequence()),"recovery events"));
            state = Objects.requireNonNull(restorer.replay(state, missing),"restored state");
            if (!leases.isCurrent(lease)) throw new IllegalStateException("room lease lost during recovery");
            String digest=restorer.digest(state);
            if(digest==null||digest.isBlank())throw new IllegalStateException("restored state digest is required");
            List<String> expired=RoomDeadlineSnapshot.fromState(snapshot.authoritativeState()).expiredAt(clock.instant());
            return new RecoveredRoom<>(lease, snapshot, state, digest,expired);
        } catch (RuntimeException | Error failure) {
            try { leases.release(lease); }
            catch (RuntimeException releaseFailure) { failure.addSuppressed(releaseFailure); }
            throw failure;
        }
    }
}
