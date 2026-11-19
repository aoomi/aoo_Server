package business.global.mj.cdxzmj;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameIdentityPolicy;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boundary contract for current CDXZMJ plus its explicit historical bridge.
 *
 * <p>The current provider must be the canonical 1001 identity.  The historical
 * 516 implementation is separately named and admitted only by Compatibility.</p>
 */
class CDXZMJProviderBoundaryTest {
    private static final GameDescriptor CURRENT_CHENGDU = new GameDescriptor(
            1001, "chengdu1001", "成都麻将", GameCategory.MAHJONG,
            "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", "chengdu1001-v1.0.0");

    @Test
    void exactCdxzmjDescriptorIsCurrentPublished() {
        GameDescriptor descriptor = new CDXZMJGameProvider().descriptor();

        assertEquals(1001, descriptor.gameId());
        assertEquals("chengdu1001", descriptor.code());
        assertEquals("chengdu1001-v1.0.0", descriptor.version());
        assertFalse(GameIdentityPolicy.isCompatibilityOnly(descriptor));
    }

    @Test
    void productionProjectionKeepsCurrentAndExcludesHistorical516() {
        GameProvider current = new CDXZMJGameProvider();
        GameProvider legacy = new CDXZMJCompatibilityProvider();

        GameRegistry production = GameRegistryFactory.productionFrom(List.of(legacy, current));

        assertEquals(List.of(1001), production.descriptors().stream()
                .map(GameDescriptor::gameId).toList());
        assertThrows(IllegalArgumentException.class, () -> production.require(516));
        assertEquals("chengdu1001", production.require(1001, "chengdu1001-v1.0.0")
                .descriptor().code());
    }

    @Test
    void productionServiceLoaderNeverAdmits516AndFailsClosedWithoutCurrentProvider() {
        List<GameProvider> discovered = ServiceLoader.load(GameProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        assertTrue(discovered.stream().anyMatch(provider -> provider instanceof CDXZMJGameProvider),
                "the current CDXZMJ ServiceLoader entry must remain visible");
        assertTrue(discovered.stream().anyMatch(provider -> provider instanceof CDXZMJCompatibilityProvider),
                "the historical compatibility ServiceLoader entry must remain visible");

        try {
            GameRegistry production = GameRegistryFactory.productionFromServiceLoader();
            assertFalse(production.descriptors().stream().anyMatch(d -> d.gameId() == 516));
        } catch (IllegalStateException error) {
            assertTrue(error.getMessage().equals("NO_CURRENT_PRODUCTION_PROVIDER")
                    || error.getMessage().startsWith("PRODUCTION_PROJECTION_INCOMPLETE"));
        }
    }

    @Test
    void explicitCompatibilityServiceLoaderRetainsExact516Provider() {
        GameRegistry compatibility = GameRegistryFactory.compatibilityFromServiceLoader();

        GameProvider provider = compatibility.require(516, "legacy-equivalent-1");
        assertNotNull(provider);
        assertTrue(provider instanceof CDXZMJCompatibilityProvider);
        assertTrue(GameIdentityPolicy.isCompatibilityOnly(provider.descriptor()));
    }

    @Test
    void historical516CannotBeAdmittedByCurrentProvider() {
        GameProvider current = new CDXZMJGameProvider();
        assertThrows(IllegalArgumentException.class,
                () -> GameRegistryFactory.productionFrom(List.of(current)).require(516));
        assertFalse(GameIdentityPolicy.isCompatibilityOnly(current.descriptor()));
    }

    @Test
    void currentChengduCatalogResolvesThroughMahjongFamilyProvider() {
        GameProvider provider = MahjongCatalogRuntimeRegistry.providerFor(CURRENT_CHENGDU)
                .orElseThrow(() -> new AssertionError("current chengdu1001 catalog provider is missing"));

        assertEquals(1001, provider.descriptor().gameId());
        assertEquals("chengdu1001", provider.descriptor().code());
        assertEquals("chengdu1001-v1.0.0", provider.descriptor().version());
        assertFalse(GameIdentityPolicy.isCompatibilityOnly(provider.descriptor()));
    }

}
