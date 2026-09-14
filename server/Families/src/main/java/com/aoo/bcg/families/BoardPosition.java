package com.aoo.bcg.families;

import java.util.Map;
import java.util.Set;

/** Immutable board position with explicit bounds, blocked cells and occupancy legality. */
public record BoardPosition(int cellCount, Set<Integer> blocked, Map<Integer,Integer> pieces) {
    public BoardPosition {
        if (cellCount <= 0 || blocked == null || pieces == null) throw new IllegalArgumentException("invalid board");
        blocked=Set.copyOf(blocked); pieces=Map.copyOf(pieces);
        for (int cell:blocked) requireCell(cell, cellCount);
        java.util.HashSet<Integer> occupied=new java.util.HashSet<>();
        for (var entry:pieces.entrySet()) {
            if (entry.getKey()<0) throw new IllegalArgumentException("negative piece id");
            requireCell(entry.getValue(), cellCount);
            if (blocked.contains(entry.getValue()) || !occupied.add(entry.getValue())) throw new IllegalArgumentException("illegal occupancy");
        }
    }
    public BoardPosition move(int pieceId, int steps) {
        Integer from=pieces.get(pieceId); if(from==null || steps<0) throw new IllegalArgumentException("invalid move");
        int target=Math.addExact(from,steps); requireCell(target,cellCount);
        if(blocked.contains(target) || pieces.entrySet().stream().anyMatch(e->e.getKey()!=pieceId&&e.getValue()==target)) throw new IllegalStateException("target unavailable");
        var next=new java.util.LinkedHashMap<>(pieces); next.put(pieceId,target); return new BoardPosition(cellCount,blocked,next);
    }
    private static void requireCell(int cell,int count){if(cell<0||cell>=count)throw new IllegalArgumentException("cell outside board");}
}
