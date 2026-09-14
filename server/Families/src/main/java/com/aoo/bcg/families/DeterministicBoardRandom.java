package com.aoo.bcg.families;

import java.util.ArrayList;
import java.util.List;

/** Seed plus cursor is a complete replay identity for dice and board random events. */
public final class DeterministicBoardRandom {
    public record Snapshot(long seed, long cursor) { public Snapshot { if (cursor < 0) throw new IllegalArgumentException("negative cursor"); } }
    public record DiceRoll(int sides, List<Integer> faces, Snapshot before, Snapshot after) {
        public DiceRoll { faces = List.copyOf(faces); }
    }
    private final long seed;
    private long cursor;
    public DeterministicBoardRandom(long seed) { this(seed, 0); }
    public DeterministicBoardRandom(long seed, long cursor) { if (cursor < 0) throw new IllegalArgumentException("negative cursor"); this.seed=seed; this.cursor=cursor; }
    public Snapshot snapshot() { return new Snapshot(seed, cursor); }
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        long z = seed + (++cursor * 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return (int) Long.remainderUnsigned(z ^ (z >>> 31), bound);
    }
    public DiceRoll roll(int count, int sides) {
        if (count <= 0 || count > 32 || sides < 2) throw new IllegalArgumentException("invalid dice");
        Snapshot before=snapshot(); List<Integer> faces=new ArrayList<>(count);
        for (int i=0;i<count;i++) faces.add(nextInt(sides)+1);
        return new DiceRoll(sides, faces, before, snapshot());
    }
}
