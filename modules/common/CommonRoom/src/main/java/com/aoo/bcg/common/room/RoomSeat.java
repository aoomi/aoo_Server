package com.aoo.bcg.common.room;
public record RoomSeat(int seatId, long playerId, SeatStatus status, long acceptedSequence) {
    public RoomSeat { if (seatId < 0 || playerId < 0 || acceptedSequence < 0) throw new IllegalArgumentException("invalid seat"); }
}
