package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.AlgorithmBudget;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/** Four-meld-and-one-pair detector where the configured tile may replace any tile. */
public final class LaiZiMahjongWinDetector {
    public boolean isWinning(List<Integer> concealed, int offered, int wildcardTile) {
        ArrayList<Integer> tiles = new ArrayList<>(concealed);
        if (tiles.size() % 3 == 1) tiles.add(offered);
        if (tiles.size() % 3 != 2) return false;
        TreeMap<Integer,Integer> counts = new TreeMap<>(); int wild = 0;
        for (int tile : tiles) if (tile == wildcardTile) wild++; else counts.merge(tile, 1, Integer::sum);
        HashSet<String> visited = new HashSet<>();
        AlgorithmBudget budget = AlgorithmBudget.start(tiles.size(), 14, 100_000, 16, Duration.ofMillis(50));
        for (int pair : new ArrayList<>(counts.keySet())) {
            budget.step();
            int used = Math.min(2, counts.get(pair)); int need = 2 - used;
            if (need <= wild) { take(counts, pair, used); if (melds(counts, wild - need, visited, budget)) return true; counts.merge(pair, used, Integer::sum); }
        }
        return wild >= 2 && melds(counts, wild - 2, visited, budget);
    }
    private boolean melds(TreeMap<Integer,Integer> counts, int wild, HashSet<String> visited, AlgorithmBudget budget) {
        try (var depth = budget.enter()) {
        Integer first = counts.entrySet().stream().filter(e -> e.getValue() > 0).map(java.util.Map.Entry::getKey).findFirst().orElse(null);
        if (first == null) return wild % 3 == 0;
        String key = counts.toString() + '/' + wild; if (!visited.add(key)) return false;
        int actual = Math.min(3, counts.get(first)); int need = 3 - actual;
        if (need <= wild) { take(counts, first, actual); if (melds(counts, wild - need, visited, budget)) return true; counts.merge(first, actual, Integer::sum); }
        for (int start = first - 2; start <= first; start++) if (validSequence(start) && first >= start && first <= start + 2) {
            int missing = 0; ArrayList<Integer> used = new ArrayList<>();
            for (int tile = start; tile <= start + 2; tile++) if (counts.getOrDefault(tile, 0) > 0) { take(counts, tile, 1); used.add(tile); } else missing++;
            if (missing <= wild && melds(counts, wild - missing, visited, budget)) return true;
            used.forEach(tile -> counts.merge(tile, 1, Integer::sum));
        }
        return false;
        }
    }
    private static boolean validSequence(int start) { int rank = Math.floorMod(start, 10); return rank >= 1 && rank <= 7 && start / 10 == (start + 2) / 10; }
    private static void take(TreeMap<Integer,Integer> counts, int tile, int amount) { counts.put(tile, counts.get(tile) - amount); }
}
