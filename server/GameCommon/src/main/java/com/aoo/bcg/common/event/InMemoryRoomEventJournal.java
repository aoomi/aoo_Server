package com.aoo.bcg.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.aoo.bcg.gamespi.ImmutableValue;

public final class InMemoryRoomEventJournal implements RoomEventJournal {
    private final ConcurrentHashMap<Long, List<Event>> events = new ConcurrentHashMap<>();
    @Override public void append(RoomEventIdentity identity, Object payload) {
        if (identity == null) throw new IllegalArgumentException("event identity is required");
        List<Event> roomEvents = events.computeIfAbsent(identity.roomId(), ignored -> new ArrayList<>());
        synchronized (roomEvents) {
            if (!roomEvents.isEmpty() && identity.sequence() <= roomEvents.getLast().identity().sequence()) throw new IllegalStateException("event sequence must increase");
            if (roomEvents.stream().anyMatch(event -> event.identity().roundNo() == identity.roundNo()
                    && event.identity().businessEventId().equals(identity.businessEventId())))
                throw new IllegalStateException("duplicate business event identity");
            roomEvents.add(new Event(identity, ImmutableValue.freeze(payload)));
        }
    }
    @Override public List<Object> after(long roomId, long sequenceExclusive) {
        List<Event> roomEvents = events.get(roomId);
        if (roomEvents == null) return List.of();
        synchronized (roomEvents) { return roomEvents.stream().filter(event -> event.identity().sequence() > sequenceExclusive).map(Event::payload).toList(); }
    }
    private record Event(RoomEventIdentity identity, Object payload) {}
}
