package com.aoo.bcg.gamespi;

/** Immutable boundary identity. A user owns a seat; neither value identifies a connection. */
public record PlayerSeatIdentity(String authenticatedUserId, int seatId) {
    public PlayerSeatIdentity {
        if (authenticatedUserId == null || authenticatedUserId.isBlank() || seatId < 0) {
            throw new IllegalArgumentException("invalid player-seat identity");
        }
    }
}
