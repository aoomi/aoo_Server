package com.aoo.bcg.common.event;

/** Immutable business identity for an authoritative room event. */
public record RoomEventIdentity(long roomId, int roundNo, long sequence,
                                String businessEventId, String eventType, int schemaVersion) {
    public RoomEventIdentity {
        if (roomId <= 0 || roundNo < 0 || sequence <= 0 || schemaVersion <= 0
                || businessEventId == null || businessEventId.isBlank() || businessEventId.length() > 128
                || eventType == null || eventType.isBlank() || eventType.length() > 128) {
            throw new IllegalArgumentException("invalid room event identity");
        }
    }

    public static RoomEventIdentity v1(long roomId, int roundNo, long sequence,
                                       String businessEventId, String eventType) {
        return new RoomEventIdentity(roomId, roundNo, sequence, businessEventId, eventType, 1);
    }
}
