package com.aoo.bcg.mahjong;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class StandardMahjongRuleSet implements MahjongRuleSet<MahjongState> {
    private final StandardMahjongConfig config;
    private final StandardMahjongWinDetector wins = new StandardMahjongWinDetector();
    public StandardMahjongRuleSet(StandardMahjongConfig config) { this.config = java.util.Objects.requireNonNull(config); }

    @Override public Set<MahjongOperation> allowedOperations(int seatId, MahjongState state) {
        if (seatId == state.lastDiscardSeat() || state.lastDiscard() == 0) return Set.of();
        List<Integer> hand = state.hands().get(seatId); if (hand == null) return Set.of();
        int copies = (int) hand.stream().filter(tile -> tile == state.lastDiscard()).count();
        EnumSet<MahjongOperation> result = EnumSet.noneOf(MahjongOperation.class);
        if (config.allowPeng() && copies >= 2) result.add(MahjongOperation.PENG);
        if (config.allowExposedGang() && copies >= 3) result.add(MahjongOperation.GANG);
        if (config.allowChi() && isNextSeat(state, seatId) && canChi(hand, state.lastDiscard())) result.add(MahjongOperation.CHI);
        if (canWin(seatId, hand, state.lastDiscard(), state)) result.add(MahjongOperation.HU);
        if (!result.isEmpty()) result.add(MahjongOperation.PASS);
        return Set.copyOf(result);
    }
    @Override public boolean canWin(int seatId, List<Integer> concealedTiles, int winningTile, MahjongState state) { return wins.isWinning(concealedTiles, winningTile); }
    private static boolean isNextSeat(MahjongState state, int seat) {
        List<Integer> seats = state.hands().keySet().stream().sorted().toList(); int from = seats.indexOf(state.lastDiscardSeat());
        return seats.get((from + 1) % seats.size()) == seat;
    }
    private static boolean canChi(List<Integer> hand, int tile) {
        return contains(hand, tile - 2, tile - 1, tile) || contains(hand, tile - 1, tile + 1, tile) || contains(hand, tile + 1, tile + 2, tile);
    }
    private static boolean contains(List<Integer> hand, int first, int second, int offered) {
        return first / 10 == offered / 10 && second / 10 == offered / 10 && hand.contains(first) && hand.contains(second);
    }
}
