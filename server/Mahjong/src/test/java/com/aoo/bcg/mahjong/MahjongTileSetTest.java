package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MahjongTileSetTest {
    @Test void versionsBindValidatedRegionalInventories() {
        MahjongTileSet sichuan = MahjongTileSet.sichuan("cd-xz-1");
        MahjongTileSet standard = MahjongTileSet.standard("jy-1");
        MahjongTileSet flowers = MahjongTileSet.standardWithFlowers("flower-1");
        MahjongTileSetCatalog catalog = new MahjongTileSetCatalog(List.of(sichuan, standard, flowers));

        assertEquals(108, catalog.require("cd-xz-1").size());
        assertEquals(136, catalog.require("jy-1").size());
        assertEquals(144, catalog.require("flower-1").size());
        assertEquals(8, flowers.flowerTiles().size());
        assertThrows(IllegalArgumentException.class, () -> catalog.require("missing"));
    }

    @Test void detectsUnknownTilesAndCopyOverflow() {
        MahjongTileSet tileSet = MahjongTileSet.sichuan("v1");
        ArrayList<Integer> invalid = new ArrayList<>(tileSet.createWall());
        invalid.add(11);
        invalid.add(41);
        assertEquals(List.of("TOO_MANY_COPIES:11", "UNKNOWN_TILE:41"),
                tileSet.inventoryViolations(invalid));
    }

    @Test void wallOrderIsStableAndImmutableForSeededReplay() {
        MahjongTileSet tileSet=MahjongTileSet.standard("stable");
        assertEquals(List.of(11,11,11,11,12,12,12,12,13,13,13,13),tileSet.createWall().subList(0,12));
        assertEquals(tileSet.createWall(),MahjongTileSet.standard("stable").createWall());
        assertThrows(UnsupportedOperationException.class,()->tileSet.copiesByTile().put(99,1));
    }
}
