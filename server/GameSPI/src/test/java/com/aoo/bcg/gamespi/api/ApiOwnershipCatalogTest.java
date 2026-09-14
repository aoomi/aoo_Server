package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ApiOwnershipCatalogTest {
    @Test void resolvesAllPublicTransportNamespacesToNamedMaintainers() {
        var catalog = ApiOwnershipCatalog.standard();
        assertEquals("AdminApi", catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/admin/game-profiles").ownerModule());
        assertEquals("game-mahjong", catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "mahjong.xuezhan.play_card_req").maintainerTeam());
        assertEquals("realtime-platform", catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "common.room.join_req").maintainerTeam());
        assertThrows(IllegalStateException.class, () -> catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "shadow.action_req"));
    }
}
