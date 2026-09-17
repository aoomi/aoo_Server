package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RegionScope;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokerFamilyProviderFactoriesTest {
    @Test
    void keepsPublishedPdkBindingCompatible() {
        GameDescriptor descriptor = poker(8, PdkBusinessCodes.CHENGDU, PaoDeKuaiFamily.CODE);

        GameProvider provider = PokerFamilyProviderFactories
                .providerFor(descriptor, List.of())
                .orElseThrow();

        assertEquals(PdkGameProvider.class, provider.getClass());
        assertSame(descriptor, PokerCatalogRuntimeRegistry.providerFor(descriptor)
                .orElseThrow().descriptor());
    }

    @Test
    void discoversAnExternalFamilyWithoutChangingTheBuiltInSwitch() {
        GameDescriptor descriptor = poker(999, "CD299", "poker:che-xuan");
        GameProvider expected = new TestProvider(descriptor);
        PokerFamilyProviderFactory factory = candidate ->
                candidate.code().equals("CD299") ? Optional.of(expected) : Optional.empty();

        assertSame(expected, PokerFamilyProviderFactories
                .providerFor(descriptor, List.of(factory))
                .orElseThrow());
    }

    @Test
    void rejectsAmbiguousPluginOwnership() {
        GameDescriptor descriptor = poker(998, "CN298", "poker:niu-niu");
        PokerFamilyProviderFactory first = candidate -> Optional.of(new TestProvider(candidate));
        PokerFamilyProviderFactory second = candidate -> Optional.of(new TestProvider(candidate));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> PokerFamilyProviderFactories.providerFor(descriptor, List.of(first, second)));
        assertTrue(error.getMessage().contains("CN298/poker:niu-niu"));
    }

    @Test
    void ignoresNonPokerDescriptors() {
        GameDescriptor descriptor = new GameDescriptor(1, "x", "x", GameCategory.MAHJONG,
                "mahjong:standard", RegionScope.NATIONAL, "", "", "1.0.0");
        PokerFamilyProviderFactory invalid = candidate -> Optional.of(new TestProvider(candidate));

        assertTrue(PokerFamilyProviderFactories
                .providerFor(descriptor, List.of(invalid))
                .isEmpty());
    }

    private static GameDescriptor poker(int id, String code, String family) {
        return new GameDescriptor(id, code, code, GameCategory.POKER, family,
                RegionScope.NATIONAL, "", "", "1.0.0");
    }

    private record TestProvider(GameDescriptor descriptor) implements GameProvider {
        @Override
        public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() {
            return context -> { throw new UnsupportedOperationException("test provider"); };
        }
    }
}
