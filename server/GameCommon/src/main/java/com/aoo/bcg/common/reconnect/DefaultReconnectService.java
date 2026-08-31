package com.aoo.bcg.common.reconnect;

import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.common.recovery.RoomSnapshotStore;

import java.util.List;
import java.util.Objects;
import com.aoo.bcg.common.perspective.ViewerContext;

public final class DefaultReconnectService<V>
        implements ReconnectService<V, PerspectiveRoomEvent> {
    private static final int MAX_INCREMENTAL_EVENTS = 500;
    private final ReconnectAuthorizer authorizer;
    private final RoomSnapshotStore snapshots;
    private final PerspectiveViewBuilder<RoomSnapshot, V> projector;
    private final PerspectiveRoomEventJournal events;

    public DefaultReconnectService(ReconnectAuthorizer authorizer, RoomSnapshotStore snapshots,
            PerspectiveViewBuilder<RoomSnapshot, V> projector, PerspectiveRoomEventJournal events) {
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public ReconnectResult<V, PerspectiveRoomEvent> reconnect(long authenticatedPlayerId, long roomId,
            long lastSequence, String reconnectToken) {
        if (authenticatedPlayerId <= 0 || roomId <= 0 || lastSequence < 0)
            throw new IllegalArgumentException("invalid reconnect request");
        authorizer.requireAccess(authenticatedPlayerId, roomId, reconnectToken);
        RoomSnapshot snapshot = snapshots.latest(roomId)
                .orElseThrow(() -> new IllegalStateException("room snapshot not found"));
        V playerView = projector.build(snapshot, authenticatedPlayerId);
        // A client-controlled sequence must never be allowed to skip authoritative events.
        long cursor = snapshot.lastEventSequence();
        List<PerspectiveRoomEvent> missing = events.after(ViewerContext.player(roomId, authenticatedPlayerId),
                cursor, MAX_INCREMENTAL_EVENTS);
        long latest = missing.isEmpty() ? snapshot.lastEventSequence()
                : missing.get(missing.size() - 1).sequence();
        return new ReconnectResult<>(playerView, missing, latest);
    }
}
