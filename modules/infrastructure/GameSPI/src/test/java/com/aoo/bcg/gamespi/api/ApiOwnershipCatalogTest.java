package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiOwnershipCatalogTest {
    @Test void resolvesAllPublicTransportNamespacesToNamedMaintainers() {
        var catalog = ApiOwnershipCatalog.standard();
        assertEquals("AdminApi", catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/admin/game-profiles").ownerModule());
        assertEquals("game-mahjong", catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "mahjong.xuezhan.play_card_req").maintainerTeam());
        assertEquals("realtime-platform", catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "common.room.join_req").maintainerTeam());
        assertThrows(IllegalStateException.class, () -> catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "shadow.action_req"));
    }

    @Test void clubPatternsResolveToOneCanonicalLobbyClubOwnerWithoutRenamingWireNames() {
        var catalog = ApiOwnershipCatalog.standard();
        var http = catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/club/list");
        var wss = catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "club.dispatch");
        assertEquals(ApiOwnershipCatalog.LOBBY_CLUB_OWNER, http.ownerModule());
        assertEquals(ApiOwnershipCatalog.LOBBY_CLUB_OWNER, wss.ownerModule());
        assertEquals("/api/v2/club/*", http.pattern());
        assertEquals("club.*", wss.pattern());
        assertEquals("platform-hall", http.maintainerTeam());
        assertEquals(1, catalog.entries().stream().filter(entry -> entry.pattern().equals("/api/v2/club/*")).count());
        assertEquals(1, catalog.entries().stream().filter(entry -> entry.pattern().equals("club.*")).count());
        assertTrue(catalog.entries().stream().filter(entry -> entry.pattern().equals("club.*")
                || entry.pattern().equals("/api/v2/club/*")).noneMatch(entry -> entry.ownerModule().equals("Families")));
    }

    @Test void duplicateAndOverlappingOwnersFailClosedBeforeBinding() {
        assertThrows(IllegalArgumentException.class, () -> new ApiOwnershipCatalog(null));
        assertThrows(IllegalArgumentException.class, () -> new ApiOwnershipCatalog(List.of(
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.WSS, "club.*", "Lobby/Club", "platform-hall"),
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.WSS, "club.dispatch", "Lobby/Club", "platform-hall"))));
        assertThrows(IllegalArgumentException.class, () -> new ApiOwnershipCatalog(List.of(
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/club/*", "Lobby/Club", "platform-hall"),
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/club/*", "Families", "game-families"))));
    }
}
