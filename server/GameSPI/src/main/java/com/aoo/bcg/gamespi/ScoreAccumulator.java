package com.aoo.bcg.gamespi;

public interface ScoreAccumulator {
    void add(long playerId, long delta, String reasonCode);
    long scoreOf(long playerId);
}
