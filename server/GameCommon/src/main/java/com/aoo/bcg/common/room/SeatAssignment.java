package com.aoo.bcg.common.room;

/** Ownership token used to compensate a failed join without releasing another player's seat. */
public record SeatAssignment(int seatId, long playerId) {
    public SeatAssignment { if (seatId < 0 || playerId <= 0) throw new IllegalArgumentException("invalid seat assignment"); }
}
