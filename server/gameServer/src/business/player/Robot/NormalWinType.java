package business.player.Robot;

import java.util.*;

import static business.player.Robot.MyUtils.combListStream;
import static business.player.Robot.StandardTileUnitType.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.groupingBy;

/**
 * 普通和牌（相对于七对等特殊和牌类型而言）。
 */
public class NormalWinType {

    private static final NormalWinType INSTANCE = new NormalWinType();

    public static NormalWinType get() {
        return INSTANCE;
    }

    private NormalWinType() {
    }

    public List<Tile> getDiscardCandidates(Set<Tile> aliveTiles, Collection<Tile> candidates) {
        // 保证顺子刻子的定义都是3张牌、将牌是2张牌
        if (SHUNZI.size() != 3 || KEZI.size() != 3) {
            throw new RuntimeException();
        }
        if (JIANG.size() != 2) {
            throw new RuntimeException();
        }

        if (aliveTiles.isEmpty()) {
            return emptyList();
        }

        List<Tile> aliveTileList = new ArrayList<>(aliveTiles);

        // 全部的preShunke
        Map<TileSuit, List<Tile>> candidatesBySuit = candidates.stream()
                .collect(groupingBy(tile -> tile.type().suit()));
        List<PreUnit> preShunkes = parsePreShunkes(aliveTileList, candidatesBySuit);

        // System.out.println("==========222===============================================");
        // preShunkes.forEach(System.out::println);
        // 从好到差排序（牌数越多越好，牌数相同的，lackedTypes种类越多越好）
        preShunkes.sort(Comparator.<PreUnit, Integer>comparing(preUnit -> preUnit.tiles.size())
                .thenComparing(preUnit -> preUnit.lackedTypesList.size()).reversed());

        // System.out.println("==========333===============================================");
        // preShunkes.forEach(System.out::println);
        // 找出与最差preShunke情况相同的units中的牌
        Set<Tile> remainTiles = new HashSet<>(aliveTiles);
        List<Tile> tilesFromWorstUnits = new ArrayList<>();
        @SuppressWarnings("unused")
        int crtUnitSize = Integer.MAX_VALUE, crtLackedKinds = Integer.MAX_VALUE;
        for (PreUnit shunke : preShunkes) {
            if (remainTiles.isEmpty()) {
                break;
            }
            if (crtUnitSize > shunke.tiles.size()) {
                // String fs = String.format("==(%d)(%d)", crtUnitSize,
                // shunke.tiles.size());
                // System.out.println(fs);
                tilesFromWorstUnits.clear();
                crtUnitSize = shunke.tiles.size();
                crtLackedKinds = shunke.lackedTypesList.size();
                // System.out.println("==========clear=======================");
            }
            // System.out.printf("=====add=====(%s)",shunke);
            // System.out.println();
            shunke.tiles.stream().filter(remainTiles::contains).forEach(tile -> {
                // String fs = String.format("测试(%s)", tile.toString());
                // System.out.println(fs);
                tilesFromWorstUnits.add(tile);
                remainTiles.remove(tile);
            });
        }
        // System.out.println("==========444===============================================");
        reverse(tilesFromWorstUnits);
        return tilesFromWorstUnits;
    }

    private List<PreUnit> parsePreShunkes(List<Tile> aliveTiles, Map<TileSuit, List<Tile>> candidatesBySuit) {
        List<PreUnit> result = new ArrayList<>();

        // 差两张牌的顺刻
        aliveTiles.forEach(tile -> {
            List<List<TileType>> lackedTypesList = new ArrayList<>();
            lackedTypesList.addAll(SHUNZI.getLackedTypesForTiles(Collections.singletonList(tile)));
            lackedTypesList.addAll(KEZI.getLackedTypesForTiles(Collections.singletonList(tile)));
            if (!lackedTypesList.isEmpty()) {
                result.add(new PreUnit(false, tile, lackedTypesList, 2));
            }
        });

        Map<TileSuit, List<Tile>> aliveTilesBySuit = aliveTiles.stream()
                .collect(groupingBy(tile -> tile.type().suit()));

        // 差一张牌的顺刻
        aliveTilesBySuit.forEach((suit, tiles) -> {
            combListStream(tiles, 2).forEach(testUnit -> {
                List<List<TileType>> lackedTypesList = new ArrayList<>();
                lackedTypesList.addAll(SHUNZI.getLackedTypesForTiles(testUnit));
                lackedTypesList.addAll(KEZI.getLackedTypesForTiles(testUnit));
                if (!lackedTypesList.isEmpty()) {
                    result.add(new PreUnit(false, testUnit, lackedTypesList, 1));
                }
            });
            // 顺刻
            combListStream(tiles, 3).filter(testUnit -> SHUNZI.isLegalTiles(testUnit) || KEZI.isLegalTiles(testUnit))
                    .forEach(unitTiles -> result.add(new PreUnit(false, unitTiles)));
        });

        return result;
    }

    private static class PreUnit {
        boolean isJiang;
        List<Tile> tiles;
        List<List<TileType>> lackedTypesList;
        @SuppressWarnings("unused")
        int lackedCount;

        PreUnit(boolean isJiang, List<Tile> tiles, List<List<TileType>> lackedTypesList, int lackedCount) {
            this.isJiang = isJiang;
            this.tiles = tiles;
            this.lackedTypesList = lackedTypesList;
            this.lackedCount = lackedCount;
        }

        PreUnit(boolean isJiang, List<Tile> tiles) {
            this(isJiang, tiles, Collections.emptyList(), 0);
        }

        PreUnit(boolean isJiang, Tile tile, List<List<TileType>> lackedTypeList, int lackedCount) {
            this(isJiang, Collections.singletonList(tile), lackedTypeList, lackedCount);
        }

        // PreUnit(boolean isJiang, Tile tile, TileType lackedType) {
        // this(isJiang, Collections.singletonList(tile),
        // singletonList(singletonList(lackedType)), 1);
        // }
        //
        // List<List<TileType>> lackedTypeList() {
        // return lackedTypesList;
        // }
        //
        // List<Tile> tiles() {
        // return tiles;
        // }

        @Override
        public String toString() {
            return "[isJiang=" + isJiang + ", tiles=" + tiles + ", lackedTypesList=" + lackedTypesList + "]";
        }

    }

}
