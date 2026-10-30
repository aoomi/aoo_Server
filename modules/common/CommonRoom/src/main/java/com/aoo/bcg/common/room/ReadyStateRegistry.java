package com.aoo.bcg.common.room;

import java.util.LinkedHashMap;
import java.util.Map;

/** Authoritative readiness with one cleanup contract for every room lifecycle boundary. */
public final class ReadyStateRegistry {
    private final Map<Integer, Boolean> ready = new LinkedHashMap<>();
    public ReadyStateRegistry(Iterable<Integer> seatIds) { seatIds.forEach(seat -> { if (seat == null || seat < 0 || ready.put(seat, false) != null) throw new IllegalArgumentException("invalid seats"); }); if (ready.isEmpty()) throw new IllegalArgumentException("seats required"); }
    public synchronized void setReady(int seatId, boolean value) { requireSeat(seatId); ready.put(seatId, value); }
    public synchronized boolean isReady(int seatId) { requireSeat(seatId); return ready.get(seatId); }
    public synchronized boolean allOccupiedReady(Iterable<Integer> occupiedSeats) {
        boolean any = false; for (Integer seat : occupiedSeats) { requireSeat(seat); any = true; if (!ready.get(seat)) return false; } return any;
    }
    public synchronized void apply(ReadyLifecycleEvent event, Integer seatId) {
        if (event == null) throw new IllegalArgumentException("event required");
        switch (event) {
            case JOINED, LEFT, TABLE_SWITCHED -> { requireSeat(seatId); ready.put(seatId, false); }
            case ROUND_STARTED, ROUND_SETTLED, ROOM_DISSOLVED -> ready.replaceAll((ignored, value) -> false);
            case RECONNECTED -> requireSeat(seatId); // Preserve server authority; reconnect only refreshes the view.
        }
    }
    public synchronized Map<Integer, Boolean> snapshot() { return Map.copyOf(ready); }
    private void requireSeat(Integer seatId) { if (seatId == null || !ready.containsKey(seatId)) throw new IllegalArgumentException("unknown seat"); }
}
