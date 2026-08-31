package com.aoo.bcg.longcard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record LongCardState(Map<Integer, List<Integer>> hands, List<Integer> deck,
        List<Integer> discards, int currentSeat, LongCardPhase phase, Integer exposedCard,
        Set<Integer> calledSeats, Integer winnerSeat) {
    public LongCardState {
        if (hands == null || hands.size() < 2 || deck == null || discards == null || phase == null
                || !hands.containsKey(currentSeat)) throw new IllegalArgumentException("invalid long-card state");
        Map<Integer, List<Integer>> copy = new LinkedHashMap<>();
        hands.forEach((seat, cards) -> copy.put(seat, List.copyOf(cards)));
        hands = Map.copyOf(copy);
        deck = List.copyOf(deck);
        discards = List.copyOf(discards);
        calledSeats = Set.copyOf(calledSeats == null ? Set.of() : calledSeats);
        if (phase == LongCardPhase.FINISHED && winnerSeat == null)
            throw new IllegalArgumentException("finished state requires winner");
    }
}
