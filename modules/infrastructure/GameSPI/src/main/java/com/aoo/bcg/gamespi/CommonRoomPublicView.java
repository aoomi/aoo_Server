package com.aoo.bcg.gamespi;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Complete public seat projection for one viewer; private gameplay state is excluded. */
public record CommonRoomPublicView(int seatCount, int localSeatId, List<CommonRoomPublicSeat> seats) {
    public CommonRoomPublicView {
        if (seatCount < 2 || seatCount > 10) throw new IllegalArgumentException("seatCount must be 2..10");
        if (localSeatId < -1 || localSeatId >= seatCount) {
            throw new IllegalArgumentException("localSeatId must be -1 or a physical seat");
        }
        seats = List.copyOf(seats);
        if (seats.size() != seatCount) throw new IllegalArgumentException("seats must contain every physical slot");
        var identities = new HashSet<Integer>();
        for (CommonRoomPublicSeat seat : seats) {
            if (seat.seatId() >= seatCount || !identities.add(seat.seatId())) {
                throw new IllegalArgumentException("seats must contain unique in-range seatIds");
            }
        }
        if (identities.size() != seatCount) throw new IllegalArgumentException("seats must be complete");
    }

    public Map<String,Object> asMap() {
        return Map.of("seatCount", seatCount, "localSeatId", localSeatId,
                "seats", seats.stream().map(CommonRoomPublicSeat::asMap).toList());
    }
}
