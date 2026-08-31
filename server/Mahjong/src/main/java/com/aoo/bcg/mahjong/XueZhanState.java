package com.aoo.bcg.mahjong;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record XueZhanState(XueZhanPhase phase, Map<Integer,List<Integer>> hands,
        Map<Integer,List<Integer>> exchanges, Map<Integer,MahjongSuit> missingSuits, Set<Integer> winners) {
    public XueZhanState {
        if (phase == null || hands == null || hands.size() < 2) throw new IllegalArgumentException("invalid xue-zhan state");
        hands = hands.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        exchanges = exchanges == null ? Map.of() : exchanges.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        missingSuits = Map.copyOf(missingSuits == null ? Map.of() : missingSuits);
        winners = Set.copyOf(winners == null ? Set.of() : winners);
    }
}
