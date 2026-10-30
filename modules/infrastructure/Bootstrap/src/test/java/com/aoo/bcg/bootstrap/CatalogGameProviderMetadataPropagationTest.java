package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;

class CatalogGameProviderMetadataPropagationTest {
    @Test
    void roomFactoryCarriesExplicitSnapshotMetadataSource() {
        GameDescriptor descriptor = new GameDescriptor(2007, "wannian2007", "万年跑得快",
                GameCategory.POKER, "poker:pao-de-kuai", RegionScope.CITY, "JX", "WN",
                "wannian2007-v1.0.0");
        CatalogGameProvider provider = new CatalogGameProvider(descriptor, true, "catalog-test");
        RoomSnapshotMetadataSource source = new RoomSnapshotMetadataSource(
                7L, "a".repeat(64), "b".repeat(64), "card-v1", "events-v1", "protocol-v1",
                "snapshot-v1", 11L, "CN-JX-WN", "c".repeat(64));

        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(
                200701L, 10L, Map.of(), null, source));

        assertSame(source, room.requireRoomSnapshotMetadataSource());
    }
}
