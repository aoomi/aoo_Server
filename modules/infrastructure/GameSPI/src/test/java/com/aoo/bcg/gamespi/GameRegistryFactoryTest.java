package com.aoo.bcg.gamespi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameRegistryFactoryTest {
    @Test
    void productionFactoryRejectsCompatibilityIdentity() {
        GameProvider legacy = provider(new GameDescriptor(516, "chengdu1001", "成都麻将",
                GameCategory.MAHJONG, "mahjong:xue-zhan", RegionScope.CITY,
                "SC", "CD", "legacy-equivalent-1"));

        assertThrows(IllegalArgumentException.class,
                () -> GameRegistryFactory.production().register(legacy));
    }

    @Test
    void compatibilityFactoryAcceptsHistoricalIdentity() {
        GameProvider legacy = provider(new GameDescriptor(516, "chengdu1001", "成都麻将",
                GameCategory.MAHJONG, "mahjong:xue-zhan", RegionScope.CITY,
                "SC", "CD", "legacy-equivalent-1"));

        assertDoesNotThrow(() -> GameRegistryFactory.compatibility().register(legacy));
    }

    @Test
    void productionProjectionRejectsIncompleteCanonicalSet() {
        GameProvider canonical = provider(new GameDescriptor(1001, "chengdu1001", "成都麻将",
                GameCategory.MAHJONG, "mahjong:xue-zhan", RegionScope.CITY,
                "SC", "CD", "chengdu1001-v1.0.0"));

        GameRegistry partial = GameRegistryFactory.productionFrom(List.of(canonical));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> GameRegistryFactory.requireCompleteProductionProjection(partial));

        assertEquals("PRODUCTION_PROJECTION_INCOMPLETE currentCount=1 expected=32", error.getMessage());
    }

    @Test
    void productionProjectionRejectsConflictingCurrentIdentity() {
        GameProvider conflict = provider(new GameDescriptor(1001, "wrong-code", "成都麻将",
                GameCategory.MAHJONG, "mahjong:xue-zhan", RegionScope.CITY,
                "SC", "CD", "chengdu1001-v1.0.0"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> GameRegistryFactory.productionFrom(List.of(conflict)));

        assertEquals(true, error.getMessage().startsWith("PRODUCTION_IDENTITY_CONFLICT gameId=1001"));
    }

    @Test
    void gameServerCannotTreatRawServiceLoaderAsCompletedProjection() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                GameRegistryFactory::requireCompletedProductionProjection);

        assertEquals("PRODUCTION_COMPLETION_REQUIRED consumer=gameServer source=bootstrap-catalog",
                error.getMessage());
    }

    private static GameProvider provider(GameDescriptor descriptor) {
        return new GameProvider() {
            @Override public GameDescriptor descriptor() { return descriptor; }
            @Override public GameRoomFactory roomFactory() { return context -> null; }
        };
    }
}
