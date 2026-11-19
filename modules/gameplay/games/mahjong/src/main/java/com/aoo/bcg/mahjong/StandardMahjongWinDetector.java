package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Standard four-meld-and-one-pair detector without wildcards. */
public final class StandardMahjongWinDetector {
    public boolean isWinning(List<Integer> concealedTiles, int offeredTile) {
        ArrayList<Integer> tiles = new ArrayList<>(concealedTiles);
        if (tiles.size() % 3 == 1) tiles.add(offeredTile);
        if (tiles.size() % 3 != 2) return false;
        TreeMap<Integer, Integer> counts = new TreeMap<>();
        for (int tile : tiles) counts.merge(tile, 1, Integer::sum);
        for (int pair : List.copyOf(counts.keySet())) {
            if (counts.get(pair) >= 2) {
                take(counts, pair, 2);
                if (melds(counts)) return true;
                counts.merge(pair, 2, Integer::sum);
            }
        }
        return false;
    }

    private boolean melds(TreeMap<Integer, Integer> counts) {
        Integer tile = counts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(java.util.Map.Entry::getKey).findFirst().orElse(null);
        if (tile == null) return true;
        if (counts.get(tile) >= 3) {
            take(counts, tile, 3); if (melds(counts)) return true; counts.merge(tile, 3, Integer::sum);
        }
        if (sameSuitSequence(tile) && counts.getOrDefault(tile + 1, 0) > 0 && counts.getOrDefault(tile + 2, 0) > 0) {
            take(counts, tile, 1); take(counts, tile + 1, 1); take(counts, tile + 2, 1);
            if (melds(counts)) return true;
            counts.merge(tile, 1, Integer::sum); counts.merge(tile + 1, 1, Integer::sum); counts.merge(tile + 2, 1, Integer::sum);
        }
        return false;
    }
    private static boolean sameSuitSequence(int tile) {
        int rank = Math.floorMod(tile, 10);
        return rank >= 1 && rank <= 7 && tile / 10 == (tile + 2) / 10;
    }
    private static void take(TreeMap<Integer, Integer> counts, int tile, int amount) { counts.put(tile, counts.get(tile) - amount); }
}
