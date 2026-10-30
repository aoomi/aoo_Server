package com.aoo.bcg.common.random;

import java.util.List;

/** Deterministic random stream whose seed is retained for round replay and audit. */
public interface GameRandomSource {
    int nextInt(int bound);
    boolean nextBoolean();
    void shuffle(List<?> values);
    long seed();
}
