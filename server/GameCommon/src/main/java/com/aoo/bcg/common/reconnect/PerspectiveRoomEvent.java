package com.aoo.bcg.common.reconnect;

public record PerspectiveRoomEvent(long sequence, String eventType, Object payload) {
    public PerspectiveRoomEvent {
        if (sequence < 0 || eventType == null || eventType.isBlank())
            throw new IllegalArgumentException("invalid perspective room event");
    }
}
