package com.aoo.bcg.common.recovery;
import java.time.Instant;
public record RoomLease(long roomId, String ownerNode, long fencingToken, Instant expiresAt) {
    public RoomLease {
        if (roomId <= 0 || ownerNode == null || ownerNode.isBlank() || fencingToken <= 0
                || expiresAt == null) {
            throw new IllegalArgumentException("invalid room lease");
        }
    }
}
