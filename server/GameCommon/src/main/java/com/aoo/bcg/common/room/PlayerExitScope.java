package com.aoo.bcg.common.room;
public record PlayerExitScope(long roomId, long playerId, int seatId, long connectionGeneration) {
    public PlayerExitScope { if (roomId <= 0 || playerId <= 0 || seatId < 0 || connectionGeneration <= 0) throw new IllegalArgumentException("invalid exit scope"); }
}
