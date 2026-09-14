package com.aoo.bcg.common.transaction;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRoomAccountingSagaRepository implements RoomAccountingSagaRepository {
    private final ConcurrentHashMap<RoomAccountingSagaId, RoomAccountingSagaState> states = new ConcurrentHashMap<>();
    @Override public boolean create(RoomAccountingSagaId id) {
        return states.putIfAbsent(id, RoomAccountingSagaState.NEW) == null;
    }
    @Override public Optional<RoomAccountingSagaState> find(RoomAccountingSagaId id) {
        return Optional.ofNullable(states.get(id));
    }
    @Override public boolean compareAndSet(RoomAccountingSagaId id, RoomAccountingSagaState expected,
                                           RoomAccountingSagaState next) {
        return states.replace(id, expected, next);
    }
}
