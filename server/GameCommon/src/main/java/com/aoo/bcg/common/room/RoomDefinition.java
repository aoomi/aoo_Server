package com.aoo.bcg.common.room;

/** Strong construction boundary shared by legacy and native authoritative rooms. */
public record RoomDefinition(long roomId, int gameId, String playVersion, int seatCount, long ownerId) {
    public RoomDefinition {
        if (roomId <= 0 || gameId <= 0) throw new IllegalArgumentException("positive room and game identity required");
        if (playVersion == null || playVersion.isBlank()) throw new IllegalArgumentException("playVersion is required");
        if (seatCount < 2 || seatCount > 16) throw new IllegalArgumentException("seatCount must be between 2 and 16");
        if (ownerId < 0) throw new IllegalArgumentException("ownerId cannot be negative");
    }

    public static RoomDefinition unowned(long roomId, int gameId, String playVersion, int seatCount) {
        return new RoomDefinition(roomId, gameId, playVersion, seatCount, 0);
    }
}
