package com.aoo.bcg.common.room;

import com.aoo.bcg.common.random.GameRandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Selects one currently vacant seat without retaining room state or random state. */
public final class AuthoritativeRandomSeatAllocator {
    public int allocate(int seatLimit, Set<Integer> occupiedSeats, GameRandomSource random) {
        if (seatLimit <= 0) throw new IllegalArgumentException("seatLimit must be positive");
        if (occupiedSeats == null) throw new IllegalArgumentException("occupiedSeats is required");
        if (random == null) throw new IllegalArgumentException("random is required");

        for (Integer seat : occupiedSeats) {
            if (seat == null || seat < 0 || seat >= seatLimit) {
                throw new IllegalArgumentException("occupied seat is outside seatLimit");
            }
        }

        List<Integer> vacantSeats = new ArrayList<>(seatLimit - occupiedSeats.size());
        for (int seat = 0; seat < seatLimit; seat++) {
            if (!occupiedSeats.contains(seat)) vacantSeats.add(seat);
        }
        if (vacantSeats.isEmpty()) throw new IllegalStateException("room has no vacant seat");

        return vacantSeats.get(random.nextInt(vacantSeats.size()));
    }
}
