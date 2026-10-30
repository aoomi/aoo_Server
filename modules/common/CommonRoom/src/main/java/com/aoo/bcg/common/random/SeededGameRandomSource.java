package com.aoo.bcg.common.random;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Java-compatible seeded stream used by authoritative game decisions. */
public final class SeededGameRandomSource implements GameRandomSource {
    private static final SecureRandom SEED_SOURCE = new SecureRandom();
    private final long seed;
    private final Random random;

    public SeededGameRandomSource(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public static SeededGameRandomSource create() {
        return new SeededGameRandomSource(SEED_SOURCE.nextLong());
    }

    @Override public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        return random.nextInt(bound);
    }
    @Override public boolean nextBoolean() { return random.nextBoolean(); }
    @Override public void shuffle(List<?> values) { Collections.shuffle(values, random); }
    @Override public long seed() { return seed; }
}
