package com.aoo.bcg.gamespi;

public interface SettlementExtension<C> {
    void settle(C context, ScoreAccumulator scores);
}
