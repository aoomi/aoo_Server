package com.aoo.bcg.mahjong;

import java.util.LinkedHashMap;
import java.util.Map;

/** Validates that one immutable tile definition is bound to each play version. */
public final class MahjongTileSetCatalog {
    private final Map<String, MahjongTileSet> byVersion;

    public MahjongTileSetCatalog(Iterable<MahjongTileSet> tileSets) {
        LinkedHashMap<String, MahjongTileSet> indexed = new LinkedHashMap<>();
        for (MahjongTileSet tileSet : tileSets) {
            if (indexed.putIfAbsent(tileSet.playVersion(), tileSet) != null) {
                throw new IllegalArgumentException("duplicate Mahjong playVersion: " + tileSet.playVersion());
            }
        }
        if (indexed.isEmpty()) throw new IllegalArgumentException("at least one tile set is required");
        byVersion = java.util.Collections.unmodifiableMap(indexed);
    }

    public MahjongTileSet require(String playVersion) {
        MahjongTileSet result = byVersion.get(playVersion);
        if (result == null) throw new IllegalArgumentException("unknown Mahjong playVersion: " + playVersion);
        return result;
    }
}
