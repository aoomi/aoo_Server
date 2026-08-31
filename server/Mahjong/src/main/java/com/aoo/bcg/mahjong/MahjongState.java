package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MahjongState(List<Integer> wall, Map<Integer, List<Integer>> hands, int currentSeat,
        boolean currentSeatHasDrawn, int lastDiscard, int lastDiscardSeat,
        MahjongOperationWindow window, boolean finished, int winnerSeat) {
    public MahjongState {
        wall = List.copyOf(wall);
        LinkedHashMap<Integer, List<Integer>> copied = new LinkedHashMap<>();
        hands.forEach((seat, tiles) -> copied.put(seat, List.copyOf(tiles)));
        hands = Map.copyOf(copied);
        if (hands.isEmpty() || !hands.containsKey(currentSeat) || window == null)
            throw new IllegalArgumentException("invalid mahjong state");
    }
    public List<Integer> mutableHand(int seatId) {
        List<Integer> hand = hands.get(seatId);
        if (hand == null) throw new IllegalArgumentException("unknown seat");
        return new ArrayList<>(hand);
    }
    public MahjongState replace(List<Integer> newWall, Map<Integer, List<Integer>> newHands, int nextSeat,
            boolean drawn, int discard, int discardSeat, MahjongOperationWindow nextWindow,
            boolean gameFinished, int winner) {
        return new MahjongState(newWall, newHands, nextSeat, drawn, discard, discardSeat,
                nextWindow, gameFinished, winner);
    }
}
