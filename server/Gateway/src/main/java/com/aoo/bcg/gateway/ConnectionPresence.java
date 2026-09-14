package com.aoo.bcg.gateway;

import java.time.Instant;

public record ConnectionPresence(String userId, String roomId, int seatId, Status status,
                                 String connectionId, long generation, Instant changedAt) {
    public enum Status { ONLINE, OFFLINE }
    public ConnectionPresence {
        if (userId == null || userId.isBlank() || roomId == null || roomId.isBlank() || seatId < 0
                || status == null || connectionId == null || connectionId.isBlank() || generation <= 0 || changedAt == null)
            throw new IllegalArgumentException("invalid connection presence");
    }
}
