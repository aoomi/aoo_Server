package com.aoo.bcg.common.transaction;

public record RoomAccountingSagaId(long roomId, int roundNo, String playVersion, String operation,
                                   String businessId) {
    public RoomAccountingSagaId {
        if (roomId <= 0 || roundNo < 0 || playVersion == null || playVersion.isBlank()
                || operation == null || operation.isBlank() || businessId == null || businessId.isBlank())
            throw new IllegalArgumentException("invalid room accounting saga identity");
    }
}
