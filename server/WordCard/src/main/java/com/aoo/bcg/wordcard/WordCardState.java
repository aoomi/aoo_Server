package com.aoo.bcg.wordcard;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public record WordCardState(Map<Integer,List<Integer>> hands, List<Integer> deck, List<Integer> discards,
        Map<Integer,Integer> huXi, int currentSeat, WordCardPhase phase, Integer exposedCard, Integer winnerSeat) {
    public WordCardState {
        if (hands == null || hands.size() < 2 || deck == null || discards == null || huXi == null
                || phase == null || !hands.containsKey(currentSeat)) throw new IllegalArgumentException("invalid word-card state");
        Map<Integer,List<Integer>> copy = new LinkedHashMap<>(); hands.forEach((s,c)->copy.put(s,List.copyOf(c))); hands=Map.copyOf(copy);
        deck=List.copyOf(deck); discards=List.copyOf(discards); huXi=Map.copyOf(huXi);
        if (phase == WordCardPhase.FINISHED && winnerSeat == null) throw new IllegalArgumentException("finished state requires winner");
    }
}
