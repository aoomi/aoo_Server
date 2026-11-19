package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameRegistryProductionPolicyTest {
    @Test
    void productionRegistryRejects516AsCompatibilityOnly() {
        GameProvider legacy = provider(516, "chengdu1001", "legacy-equivalent-1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GameRegistryFactory.production().register(legacy));
        assertEquals(true, error.getMessage().startsWith("IDENTITY_COMPATIBILITY_ONLY"));
    }

    @Test
    void compatibilityRegistryStillAcceptsHistoricalProvider() {
        GameProvider legacy = provider(516, "chengdu1001", "legacy-equivalent-1");
        GameRegistry registry = GameRegistryFactory.compatibility();
        assertDoesNotThrow(() -> registry.register(legacy));
        assertEquals(516, registry.require(516).descriptor().gameId());
    }

    @Test
    void productionRegistryAcceptsCanonicalChengduIdentity() {
        GameProvider canonical = provider(1001, "chengdu1001", "chengdu1001-v1.0.0");
        assertDoesNotThrow(() -> GameRegistryFactory.production().register(canonical));
    }

    @Test
    void productionRegistryRejectsWordCardCompatibilityIdentity() {
        GameDescriptor descriptor = new GameDescriptor(136, "byzp", "BYZP", GameCategory.WORD_CARD,
                "word-card:pao-hu-zi", RegionScope.PROVINCE, "guangxi", "", "1.0.0");
        assertThrows(IllegalArgumentException.class,
                () -> GameRegistryFactory.production().register(provider(descriptor)));
        assertDoesNotThrow(() -> GameRegistryFactory.compatibility().register(provider(descriptor)));
    }

    @Test
    void productionDiscoveryExcludesCompatibilityProvidersWithoutFallback() {
        GameProvider legacy = provider(516, "chengdu1001", "legacy-equivalent-1");
        GameProvider canonical = provider(1001, "chengdu1001", "chengdu1001-v1.0.0");

        GameRegistry registry = GameRegistryFactory.productionFrom(List.of(legacy, canonical));

        assertEquals(List.of(1001), registry.descriptors().stream()
                .map(GameDescriptor::gameId).toList());
        assertThrows(IllegalArgumentException.class, () -> registry.require(516));
        assertEquals(1001, registry.require(1001).descriptor().gameId());
    }

    @Test
    void productionDiscoveryFailsClosedWhenOnlyCompatibilityProvidersAreAvailable() {
        GameProvider legacy = provider(516, "chengdu1001", "legacy-equivalent-1");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> GameRegistryFactory.productionFrom(List.of(legacy)));

        assertEquals("NO_CURRENT_PRODUCTION_PROVIDER", error.getMessage());
    }

    @Test
    void productionDiscoveryExcludesStaleNonCompatibilityProviderImmediately() {
        GameProvider stale = provider(8, "old-room", "1.0.0");
        GameProvider canonical = provider(1001, "chengdu1001", "chengdu1001-v1.0.0");

        GameRegistry registry = GameRegistryFactory.productionFrom(List.of(stale, canonical));

        assertEquals(List.of(1001), registry.descriptors().stream()
                .map(GameDescriptor::gameId).toList());
        assertThrows(IllegalArgumentException.class, () -> registry.require(8, "1.0.0"));
    }

    @Test
    void compatibilityDiscoveryRetainsHistoricalProviders() {
        GameProvider legacy = provider(new GameDescriptor(516, "chengdu1001", "成都血战麻将",
                GameCategory.MAHJONG, "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD",
                "legacy-equivalent-1"));

        GameRegistry registry = GameRegistryFactory.compatibilityFrom(List.of(legacy));

        assertEquals(516, registry.require(516).descriptor().gameId());
    }

    @Test
    void compatibilityDiscoveryRejectsCurrentCanonicalIdentity() {
        GameProvider canonical = provider(1001, "chengdu1001", "chengdu1001-v1.0.0");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> GameRegistryFactory.compatibilityFrom(List.of(canonical)));

        assertEquals("NO_ALLOWLISTED_COMPATIBILITY_PROVIDER", error.getMessage());
    }

    @Test
    void compatibilityDiscoveryRejectsUnknownOrUnregisteredLegacyProvider() {
        GameProvider unknown = provider(516, "chengdu1001", "legacy-equivalent-2");
        GameProvider unregistered = provider(62, "zypk", "zypk-v1.0.0");

        assertThrows(IllegalStateException.class,
                () -> GameRegistryFactory.compatibilityFrom(List.of(unknown)));
        assertThrows(IllegalStateException.class,
                () -> GameRegistryFactory.compatibilityFrom(List.of(unregistered)));
    }

    @Test
    void compatibilityDiscoveryAcceptsOnlyExactRegisteredLegacyProviders() {
        GameProvider xcpdk = provider(new GameDescriptor(618, "xcpdk", "宣城跑得快",
                GameCategory.POKER, "poker:pao-de-kuai", RegionScope.CITY, "AH", "XC",
                "legacy-equivalent-1"));
        GameProvider scjymj = provider(new GameDescriptor(628, "scjymj", "简阳麻将",
                GameCategory.MAHJONG, "mahjong-standard", RegionScope.CITY, "SC", "JY",
                "legacy-equivalent-1"));

        GameRegistry registry = GameRegistryFactory.compatibilityFrom(List.of(xcpdk, scjymj));

        assertEquals(List.of(618, 628), registry.descriptors().stream()
                .map(GameDescriptor::gameId).toList());
    }

    @Test
    void compatibilityDiscoveryExcludesUnapprovedProvidersFromMixedServiceLoaderInput() {
        GameProvider legacy = provider(new GameDescriptor(516, "chengdu1001", "成都血战麻将",
                GameCategory.MAHJONG, "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD",
                "legacy-equivalent-1"));
        GameProvider canonical = provider(1001, "chengdu1001", "chengdu1001-v1.0.0");
        GameProvider unregistered = provider(62, "zypk", "zypk-v1.0.0");

        GameRegistry registry = GameRegistryFactory.compatibilityFrom(List.of(legacy, canonical, unregistered));

        assertEquals(List.of(516), registry.descriptors().stream()
                .map(GameDescriptor::gameId).toList());
        assertThrows(IllegalArgumentException.class, () -> registry.require(1001));
        assertThrows(IllegalArgumentException.class, () -> registry.require(62));
    }

    @Test
    void nullRegistrationModeFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new GameRegistry(null));
    }

    @Test
    void currentRoomAdmissionAcceptsExactlyPublishedIdentityVersions() {
        assertDoesNotThrow(() -> GameIdentityPolicy.requireCurrentPublished(1001, "chengdu1001-v1.0.0"));
        assertDoesNotThrow(() -> GameIdentityPolicy.requireCurrentPublished(2001, "chengdu2001-v1.0.0"));
        assertDoesNotThrow(() -> GameIdentityPolicy.requireCurrentPublished(5, "nn-v1.0.0"));
    }

    @Test
    void currentRoomAdmissionRejectsLegacyAndUnknownIdentityVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> GameIdentityPolicy.requireCurrentPublished(516, "legacy-equivalent-1"));
        assertThrows(IllegalArgumentException.class,
                () -> GameIdentityPolicy.requireCurrentPublished(2001, "legacy-equivalent-1"));
        assertThrows(IllegalArgumentException.class,
                () -> GameIdentityPolicy.requireCurrentPublished(8, "v1"));
        assertThrows(IllegalArgumentException.class,
                () -> GameIdentityPolicy.requireCurrentPublished(5, "quanguo2998-v1.0.0"));
    }

    @Test
    void productionLookupRejectsAnOldCatalogRowEvenIfDiscoveryLoadedIt() {
        GameRegistry registry = GameRegistryFactory.production();
        registry.register(provider(8, "old-room", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> registry.require(8, "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> registry.require("old-room"));
    }

    private static GameProvider provider(int id, String code, String version) {
        GameDescriptor descriptor = new GameDescriptor(id, code, code, GameCategory.MAHJONG,
                "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", version);
        return provider(descriptor);
    }

    private static GameProvider provider(GameDescriptor descriptor) {
        int id = descriptor.gameId();
        String version = descriptor.version();
        return new GameProvider() {
            @Override public GameDescriptor descriptor() { return descriptor; }
            @Override public GameRoomFactory roomFactory() {
                return context -> new GameRoomHandle(context.roomId(), id, version, Map.of());
            }
        };
    }
}
