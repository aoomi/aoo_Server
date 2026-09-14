package com.aoo.bcg.mahjong;

import java.util.List;
import java.util.Set;

public final class LaiZiMahjongRuleSet implements MahjongRuleSet<MahjongState> {
    private final LaiZiMahjongConfig config;
    private final StandardMahjongRuleSet operations;
    private final LaiZiMahjongWinDetector wins = new LaiZiMahjongWinDetector();
    public LaiZiMahjongRuleSet(LaiZiMahjongConfig config) { this.config = config; this.operations = new StandardMahjongRuleSet(config.operations()); }
    @Override public Set<MahjongOperation> allowedOperations(int seat, MahjongState state) {
        java.util.EnumSet<MahjongOperation> result = java.util.EnumSet.noneOf(MahjongOperation.class);
        result.addAll(operations.allowedOperations(seat, state));
        if (canWin(seat, state.hands().getOrDefault(seat, List.of()), state.lastDiscard(), state)) { result.add(MahjongOperation.HU); result.add(MahjongOperation.PASS); }
        return Set.copyOf(result);
    }
    @Override public boolean canWin(int seat, List<Integer> hand, int tile, MahjongState state) { return wins.isWinning(hand, tile, config.wildcardTile()); }
}
