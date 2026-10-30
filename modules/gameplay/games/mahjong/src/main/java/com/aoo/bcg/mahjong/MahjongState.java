package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record MahjongState(List<Integer> wall, Map<Integer, List<Integer>> hands, int currentSeat,
        boolean currentSeatHasDrawn, int lastDiscard, int lastDiscardSeat,
        MahjongOperationWindow window, boolean finished, int winnerSeat,
        Map<Integer, List<Meld>> melds, Set<Integer> winners, int lastDrawnTile,
        MahjongRegionalRules.OpeningSelections openingSelections) {
    public record Meld(MahjongOperation operation, List<Integer> tiles, int sourceSeat) {
        public Meld {
            if (operation != MahjongOperation.CHI && operation != MahjongOperation.PENG && operation != MahjongOperation.GANG)
                throw new IllegalArgumentException("invalid meld operation");
            tiles = List.copyOf(tiles);
            int expected = operation == MahjongOperation.GANG ? 4 : 3;
            if (tiles.size() != expected) throw new IllegalArgumentException("invalid meld size");
        }
    }
    public MahjongState {
        wall = List.copyOf(wall);
        LinkedHashMap<Integer, List<Integer>> copied = new LinkedHashMap<>();
        hands.forEach((seat, tiles) -> copied.put(seat, List.copyOf(tiles)));
        hands = Map.copyOf(copied);
        Map<Integer,List<Meld>> sourceMelds=melds;
        LinkedHashMap<Integer, List<Meld>> copiedMelds = new LinkedHashMap<>();
        hands.keySet().forEach(seat -> copiedMelds.put(seat, List.copyOf(sourceMelds.getOrDefault(seat, List.of()))));
        melds = Map.copyOf(copiedMelds);
        winners = Set.copyOf(winners);
        openingSelections = openingSelections == null ? MahjongRegionalRules.OpeningSelections.empty() : openingSelections;
        if (hands.isEmpty() || !hands.containsKey(currentSeat) || window == null)
            throw new IllegalArgumentException("invalid mahjong state");
        if (!hands.keySet().containsAll(winners)) throw new IllegalArgumentException("invalid winners");
    }
    public MahjongState(List<Integer> wall, Map<Integer, List<Integer>> hands, int currentSeat,
            boolean currentSeatHasDrawn, int lastDiscard, int lastDiscardSeat,
            MahjongOperationWindow window, boolean finished, int winnerSeat) {
        this(wall, hands, currentSeat, currentSeatHasDrawn, lastDiscard, lastDiscardSeat, window,
                finished, winnerSeat, Map.of(), winnerSeat >= 0 ? Set.of(winnerSeat) : Set.of(), 0,
                MahjongRegionalRules.OpeningSelections.empty());
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
                nextWindow, gameFinished, winner, melds, winners, lastDrawnTile, openingSelections);
    }
    public MahjongState replaceAll(List<Integer> newWall, Map<Integer, List<Integer>> newHands, int nextSeat,
            boolean drawn, int discard, int discardSeat, MahjongOperationWindow nextWindow,
            boolean gameFinished, int winner, Map<Integer,List<Meld>> newMelds, Set<Integer> newWinners,
            int drawnTile, MahjongRegionalRules.OpeningSelections selections) {
        return new MahjongState(newWall,newHands,nextSeat,drawn,discard,discardSeat,nextWindow,
                gameFinished,winner,newMelds,newWinners,drawnTile,selections);
    }
}
