package com.aoo.bcg.common.reconnect;

import java.util.List;
import com.aoo.bcg.common.perspective.ViewerContext;

public interface PerspectiveRoomEventJournal {
    void appendPublic(long roomId, long sequence, String eventType, Object payload);

    void appendPrivate(long roomId, long sequence, long ownerPlayerId, String eventType, Object payload);

    List<PerspectiveRoomEvent> after(long roomId, long authenticatedPlayerId, long sequenceExclusive, int limit);

    default List<PerspectiveRoomEvent> after(ViewerContext viewer, long sequenceExclusive, int limit) {
        return after(viewer.roomId(), viewer.authenticatedViewerId(), sequenceExclusive, limit);
    }
}
