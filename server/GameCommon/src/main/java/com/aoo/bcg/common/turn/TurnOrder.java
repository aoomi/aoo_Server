package com.aoo.bcg.common.turn;
import java.util.List;
public final class TurnOrder {
    private final List<Integer> seats; private int index;
    public TurnOrder(List<Integer> seats, int firstSeatId) { this.seats = List.copyOf(seats); this.index = this.seats.indexOf(firstSeatId); if (this.seats.isEmpty() || index < 0) throw new IllegalArgumentException("invalid turn order"); }
    public synchronized int current() { return seats.get(index); }
    public synchronized int advance() { index = (index + 1) % seats.size(); return current(); }
}
