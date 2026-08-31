package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable, play-versioned Mahjong tile inventory. */
public record MahjongTileSet(String playVersion, Map<Integer, Integer> copiesByTile,
        Set<Integer> flowerTiles) {
    public MahjongTileSet {
        if (playVersion == null || playVersion.isBlank() || copiesByTile == null || copiesByTile.isEmpty()) {
            throw new IllegalArgumentException("playVersion and tile inventory are required");
        }
        LinkedHashMap<Integer, Integer> copies = new LinkedHashMap<>();
        copiesByTile.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            int tile = entry.getKey();
            int amount = entry.getValue();
            if (tile <= 0 || amount <= 0 || amount > 4) {
                throw new IllegalArgumentException("invalid tile inventory entry: " + tile + "=" + amount);
            }
            copies.put(tile, amount);
        });
        LinkedHashSet<Integer> flowers = new LinkedHashSet<>(flowerTiles == null ? Set.of() : flowerTiles);
        if (!copies.keySet().containsAll(flowers)) {
            throw new IllegalArgumentException("flower tiles must belong to the inventory");
        }
        copiesByTile = java.util.Collections.unmodifiableMap(copies);
        flowerTiles = java.util.Collections.unmodifiableSet(flowers);
    }

    public static MahjongTileSet sichuan(String playVersion) {
        return suited(playVersion, false, false);
    }

    public static MahjongTileSet standard(String playVersion) {
        return suited(playVersion, true, false);
    }

    public static MahjongTileSet standardWithFlowers(String playVersion) {
        return suited(playVersion, true, true);
    }

    private static MahjongTileSet suited(String playVersion, boolean honors, boolean flowers) {
        LinkedHashMap<Integer, Integer> copies = new LinkedHashMap<>();
        for (int suit = 1; suit <= 3; suit++) {
            for (int rank = 1; rank <= 9; rank++) copies.put(suit * 10 + rank, 4);
        }
        if (honors) for (int tile = 41; tile <= 47; tile++) copies.put(tile, 4);
        LinkedHashSet<Integer> flowerTiles = new LinkedHashSet<>();
        if (flowers) {
            for (int tile = 51; tile <= 58; tile++) {
                copies.put(tile, 1);
                flowerTiles.add(tile);
            }
        }
        return new MahjongTileSet(playVersion, copies, flowerTiles);
    }

    public int size() {
        return copiesByTile.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean contains(int tile) {
        return copiesByTile.containsKey(tile);
    }

    public boolean isFlower(int tile) {
        return flowerTiles.contains(tile);
    }

    public List<Integer> createWall() {
        ArrayList<Integer> wall = new ArrayList<>(size());
        copiesByTile.forEach((tile, amount) -> {
            for (int copy = 0; copy < amount; copy++) wall.add(tile);
        });
        return List.copyOf(wall);
    }

    public List<String> inventoryViolations(Iterable<Integer> tiles) {
        LinkedHashMap<Integer, Integer> seen = new LinkedHashMap<>();
        ArrayList<String> errors = new ArrayList<>();
        for (int tile : tiles) {
            Integer maximum = copiesByTile.get(tile);
            if (maximum == null) {
                errors.add("UNKNOWN_TILE:" + tile);
            } else if (seen.merge(tile, 1, Integer::sum) > maximum) {
                errors.add("TOO_MANY_COPIES:" + tile);
            }
        }
        return List.copyOf(errors);
    }
}
