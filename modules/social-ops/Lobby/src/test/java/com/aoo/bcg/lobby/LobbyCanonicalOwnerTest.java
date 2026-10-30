package com.aoo.bcg.lobby;

import com.aoo.bcg.gamespi.api.ApiOwnershipCatalog;
import com.aoo.bcg.hall.HallApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LobbyCanonicalOwnerTest {
    @Test
    void lobbyIsTheCanonicalLifecycleOwnerAndHallRemainsACompatibilityFacade() {
        HallApplication.start(new String[0]);

        assertEquals(LobbyApplication.State.STARTED, LobbyApplication.state());
        assertEquals(HallApplication.State.STARTED, HallApplication.state());
        assertEquals(LobbyCapabilityCatalog.capabilities(), HallApplication.HallCapabilityCatalog.capabilities());
    }

    @Test
    void capabilityCatalogRetainsTheExistingFourCapabilities() {
        assertEquals(4, LobbyCapabilityCatalog.capabilities().size());
        assertTrue(LobbyCapabilityCatalog.capabilities().contains("room-management"));
        assertTrue(LobbyCapabilityCatalog.capabilities().contains("club-management"));
        assertEquals("Lobby/Club", LobbyCapabilityCatalog.ownerFor("club-management"));
        assertThrows(IllegalStateException.class, () -> LobbyCapabilityCatalog.ownerFor("unknown-capability"));
    }

    @Test
    void lobbyOwnsFrozenContextsAndHallIsOnlyTheCompatibilityAdapter() throws Exception {
        String lobby = Files.readString(Path.of("src/main/java/com/aoo/bcg/lobby/LobbyHttpRoutes.java"));
        String hall = Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/http/HallHttpRoutes.java"));
        String lobbyClub = Files.readString(Path.of("src/main/java/com/aoo/bcg/lobby/LobbyClubTemplateHttpRoutes.java"));
        String hallClub = Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/http/HallClubTemplateHttpRoutes.java"));
        assertTrue(lobby.contains("createContext(\"/health/hall\""));
        assertTrue(lobby.contains("createContext(\"/api/v2/hall\""));
        assertTrue(lobby.contains("createContext(\"/internal/v1/hall/rooms\""));
        assertTrue(lobbyClub.contains("createContext(\"/api/v2/hall/club-template-rooms\""));
        assertFalse(hall.contains("createContext("));
        assertFalse(hallClub.contains("createContext("));
        assertTrue(hall.contains("LobbyHttpRoutes.mount"));
        assertTrue(hallClub.contains("LobbyClubTemplateHttpRoutes.mount"));
    }

    @Test
    void hallProtocolPatternsHaveOneLobbyOwnerWithoutRenamingTheirPatterns() {
        ApiOwnershipCatalog catalog = ApiOwnershipCatalog.standard();

        var http = catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/hall/*");
        var wss = catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "hall.dispatch");
        assertEquals("Lobby", http.ownerModule());
        assertEquals("Lobby", wss.ownerModule());
        assertEquals("/api/v2/hall/*", http.pattern());
        assertEquals("hall.*", wss.pattern());
        assertEquals(1, catalog.entries().stream()
                .filter(entry -> entry.pattern().equals("/api/v2/hall/*"))
                .count());
        assertEquals(1, catalog.entries().stream()
                .filter(entry -> entry.pattern().equals("hall.*"))
                .count());
    }

    @Test
    void clubProtocolPatternsHaveOneLobbyClubOwnerAndFamiliesIsNotThePublicOwner() {
        ApiOwnershipCatalog catalog = ApiOwnershipCatalog.standard();
        var http = catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP, "/api/v2/club/list");
        var wss = catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS, "club.dispatch");
        assertEquals("Lobby/Club", http.ownerModule());
        assertEquals("Lobby/Club", wss.ownerModule());
        assertEquals("/api/v2/club/*", http.pattern());
        assertEquals("club.*", wss.pattern());
        assertTrue(catalog.entries().stream()
                .filter(entry -> entry.pattern().equals("/api/v2/club/*") || entry.pattern().equals("club.*"))
                .noneMatch(entry -> entry.ownerModule().equals("Families")));
    }
}
