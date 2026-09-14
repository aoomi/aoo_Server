package com.aoo.bcg.gamespi;

/** Immutable transport-to-player binding used by reconnect and replay authorization. */
public record AuthenticatedViewerScope(String authenticatedUserId, long playerId, long roomId,
        int seatId, String connectionId, long connectionGeneration) {
    public AuthenticatedViewerScope {
        if (authenticatedUserId == null || authenticatedUserId.isBlank() || playerId <= 0 || roomId <= 0
                || seatId < 0 || connectionId == null || connectionId.isBlank() || connectionGeneration <= 0) {
            throw new IllegalArgumentException("invalid authenticated viewer scope");
        }
        if (!authenticatedUserId.equals(Long.toString(playerId))) {
            throw new IllegalArgumentException("authenticated user/player mismatch");
        }
    }
}
