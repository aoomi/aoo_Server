package com.aoo.bcg.gateway;
public record ConnectionSession(String userId, String roomId, int seatId, String playVersion, long lastSequence,
                                String connectionId, long generation) {
    public ConnectionSession(String userId, String roomId, int seatId, String playVersion, long lastSequence) {
        this(userId, roomId, seatId, playVersion, lastSequence, java.util.UUID.randomUUID().toString(), 1);
    }
    public ConnectionSession {
        if (userId == null || userId.isBlank() || roomId == null || roomId.isBlank() || seatId < 0
                || playVersion == null || playVersion.isBlank() || lastSequence < 0
                || connectionId == null || connectionId.isBlank() || generation <= 0) {
            throw new IllegalArgumentException("invalid connection session");
        }
    }
    public ConnectionSession accept(long sequence) {
        if (sequence != lastSequence + 1) throw new IllegalArgumentException("sequence must be continuous");
        return new ConnectionSession(userId, roomId, seatId, playVersion, sequence, connectionId, generation);
    }

    /** Explicit boundary mapping; the transport connection itself is not a player identity. */
    public com.aoo.bcg.gamespi.PlayerSeatIdentity playerSeatIdentity() {
        return new com.aoo.bcg.gamespi.PlayerSeatIdentity(userId, seatId);
    }
    public com.aoo.bcg.gamespi.AuthenticatedViewerScope authenticatedViewerScope() {
        try {
            return new com.aoo.bcg.gamespi.AuthenticatedViewerScope(userId, Long.parseLong(userId),
                    Long.parseLong(roomId), seatId, connectionId, generation);
        } catch (NumberFormatException error) {
            throw new IllegalStateException("viewer scope requires numeric authoritative identities", error);
        }
    }
}
