package com.aoo.bcg.lobby;

import com.aoo.bcg.gamespi.api.ApiOwnershipCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negative architecture contracts for the Hall-to-Lobby migration.
 *
 * <p>These checks intentionally fail if a future change makes Hall register a
 * second context, makes Families own a Hall/Club route, or introduces a
 * second owner for a frozen public pattern.</p>
 */
final class LobbyHallCompatibilityNegativeTest {
    @Test
    void hallFacadeCannotRegisterContextsOrCreateASecondCapabilityCatalog() throws Exception {
        String hallApplication = source("src/main/java/com/aoo/bcg/hall/HallApplication.java");
        String hallRoutes = source("src/main/java/com/aoo/bcg/hall/http/HallHttpRoutes.java");
        String hallClubRoutes = source("src/main/java/com/aoo/bcg/hall/http/HallClubTemplateHttpRoutes.java");

        assertTrue(hallApplication.contains("LobbyApplication.start(args)"));
        assertTrue(hallApplication.contains("LobbyCapabilityCatalog.capabilities()"));
        assertFalse(hallApplication.contains("AtomicReference"));
        assertFalse(hallApplication.contains("createContext("));
        assertFalse(hallRoutes.contains("createContext("));
        assertFalse(hallClubRoutes.contains("createContext("));
        assertTrue(hallRoutes.contains("LobbyHttpRoutes.mount"));
        assertTrue(hallClubRoutes.contains("LobbyClubTemplateHttpRoutes.mount"));
    }

    @Test
    void canonicalCatalogRejectsShadowOwnersAndUnknownPublicPatterns() {
        ApiOwnershipCatalog catalog = ApiOwnershipCatalog.standard();

        assertEquals("Lobby", catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP,
                "/api/v2/hall/catalog").ownerModule());
        assertEquals("Lobby/Club", catalog.requireOwner(ApiOwnershipCatalog.Transport.HTTP,
                "/api/v2/club/list").ownerModule());
        assertEquals("Lobby", catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS,
                "hall.dispatch").ownerModule());
        assertEquals("Lobby/Club", catalog.requireOwner(ApiOwnershipCatalog.Transport.WSS,
                "club.dispatch").ownerModule());
        assertThrows(IllegalStateException.class, () -> catalog.requireOwner(
                ApiOwnershipCatalog.Transport.HTTP, "/api/v2/unknown-lobby-route"));
        assertTrue(catalog.entries().stream()
                .filter(entry -> entry.pattern().equals("/api/v2/hall/*")
                        || entry.pattern().equals("/api/v2/club/*")
                        || entry.pattern().equals("hall.*")
                        || entry.pattern().equals("club.*"))
                .noneMatch(entry -> entry.ownerModule().equals("Families")));
        assertEquals(1, catalog.entries().stream()
                .filter(entry -> entry.pattern().equals("/api/v2/hall/*")).count());
        assertEquals(1, catalog.entries().stream()
                .filter(entry -> entry.pattern().equals("/api/v2/club/*")).count());
    }

    @Test
    void duplicateHallOrClubOwnersFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new ApiOwnershipCatalog(List.of(
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.HTTP,
                        "/api/v2/hall/*", "Lobby", "platform-hall"),
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.HTTP,
                        "/api/v2/hall/catalog", "Families", "game-families"))));
        assertThrows(IllegalArgumentException.class, () -> new ApiOwnershipCatalog(List.of(
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.WSS,
                        "club.*", "Lobby/Club", "platform-hall"),
                new ApiOwnershipCatalog.Ownership(ApiOwnershipCatalog.Transport.WSS,
                        "club.dispatch", "Families", "game-families"))));
    }

    private static String source(String relative) throws Exception {
        return Files.readString(Path.of(relative));
    }
}
