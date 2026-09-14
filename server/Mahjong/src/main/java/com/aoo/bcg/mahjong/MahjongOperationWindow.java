package com.aoo.bcg.mahjong;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record MahjongOperationWindow(Map<Integer, Set<MahjongOperation>> candidates) {
    public MahjongOperationWindow {
        TreeMap<Integer, Set<MahjongOperation>> copy = new TreeMap<>();
        if (candidates != null) candidates.forEach((seat, operations) -> copy.put(seat, Set.copyOf(operations)));
        candidates = Map.copyOf(copy);
    }
    public static MahjongOperationWindow empty() { return new MahjongOperationWindow(Map.of()); }
    public boolean allows(int seatId, MahjongOperation operation) {
        return candidates.getOrDefault(seatId, Set.of()).contains(operation);
    }
    public boolean isEmpty() { return candidates.isEmpty(); }
    public MahjongOperationWindow pass(int seatId) {
        TreeMap<Integer, Set<MahjongOperation>> copy = new TreeMap<>(candidates); copy.remove(seatId);
        return new MahjongOperationWindow(copy);
    }
}
