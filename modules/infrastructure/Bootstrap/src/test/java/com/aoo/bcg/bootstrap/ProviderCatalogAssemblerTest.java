package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameIdentityPolicy;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.RegionScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProviderCatalogAssemblerTest {
    @Test
    void productionAssemblyContainsThePublishedIdentityProjection() {
        GameRegistry registry = ProviderCatalogAssembler.production();
        long current = registry.descriptors().stream()
                .filter(descriptor -> GameIdentityPolicy.isCurrentPublished(
                        descriptor.gameId(), descriptor.version()))
                .count();
        assertEquals(32, current);
    }

    @Test
    void bootstrapAndGatewayUseTheSameAssemblySemantics() {
        GameRegistry bootstrap = BootstrapAPP.loadRegistry();
        GameRegistry gateway = ProviderCatalogAssembler.production();
        assertEquals(bootstrap.descriptors().size(), gateway.descriptors().size());
        assertEquals(bootstrap.descriptors().stream().map(Object::toString).toList(),
                gateway.descriptors().stream().map(Object::toString).toList());
    }

    @Test
    void explicitCompatibilityAssemblyRetainsHistoricalWordCardOnlyOutsideProduction() {
        GameRegistry production = ProviderCatalogAssembler.production();
        GameRegistry compatibility = ProviderCatalogAssembler.compatibility();
        assertFalse(production.descriptors().stream().anyMatch(d -> d.category() == GameCategory.WORD_CARD));
        assertEquals(10, compatibility.descriptors().stream()
                .filter(d -> d.category() == GameCategory.WORD_CARD).count());
        assertEquals("com.aoo.bcg.wordcard.WordCardFamilyProvider",
                compatibility.require(136, "1.0.0").getClass().getName());
    }

    @Test
    void genericCatalogBridgeRejectsWordCardDescriptor() {
        GameDescriptor historical = new GameDescriptor(136, "byzp", "BYZP", GameCategory.WORD_CARD,
                "word-card:pao-hu-zi", RegionScope.PROVINCE, "guangxi", "", "1.0.0");
        assertThrows(IllegalArgumentException.class,
                () -> new CatalogGameProvider(historical, true, "compatibility"));
    }
}
