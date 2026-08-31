package com.aoo.bcg.common.event;
import java.util.List;
public interface RoomEventJournal {
    void append(RoomEventIdentity identity, Object payload);
    List<Object> after(long roomId, long sequenceExclusive);
}
