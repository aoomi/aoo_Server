package com.aoo.bcg.common.transaction;

import java.util.Optional;

public interface RoomAccountingSagaRepository {
    boolean create(RoomAccountingSagaId id);
    Optional<RoomAccountingSagaState> find(RoomAccountingSagaId id);
    boolean compareAndSet(RoomAccountingSagaId id, RoomAccountingSagaState expected,
                          RoomAccountingSagaState next);
}
