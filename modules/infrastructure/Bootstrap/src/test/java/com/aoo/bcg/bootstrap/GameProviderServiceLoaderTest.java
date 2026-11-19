package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class GameProviderServiceLoaderTest {
    @Test
    void productionClasspathDiscoversEveryNativeGameProviderExactlyOnce() {
        Set<String> expected = Set.of(
                "business.global.mj.scjymj.SCJYMJGameProvider",
                "business.global.pk.xcpdk.XCPDKGameProvider",
                "business.global.pk.zypk.ZYPKGameProvider");
        var providers = ServiceLoader.load(GameProvider.class).stream()
                .filter(entry -> !com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry.isInternalRuntimeProvider(entry.get()))
                .toList();
        Set<String> actual = providers.stream()
                .map(provider -> provider.type().getName())
                .collect(Collectors.toSet());
        assertEquals(expected.size(), providers.size(), "only standalone lifecycle providers belong in ServiceLoader");
        assertEquals(expected, actual, "all native providers must survive production classpath assembly");
    }

    @Test void catalogExpandsLongAndWordRowsThroughFamilyProviders() {
        var registry = com.aoo.bcg.gamespi.GameRegistryFactory.compatibility();
        assertEquals(catalogDataRows(), GameCatalogLoader.registerMissing(registry));
        for (int id : List.of(80,138,210,211))
            assertEquals("com.aoo.bcg.longcard.LongCardFamilyProvider", registry.require(id,"1.0.0").getClass().getName());
        for (int id : List.of(136,153,176,302,342,404,407,462,490,596))
            assertEquals("com.aoo.bcg.wordcard.WordCardFamilyProvider", registry.require(id,"1.0.0").getClass().getName());
    }

    @Test void productionCatalogProjectionExcludesWordCardButCompatibilityRetainsIt() {
        var production = com.aoo.bcg.gamespi.GameRegistryFactory.production();
        int registered = GameCatalogLoader.registerMissing(production);
        assertEquals(catalogDataRows() - 10, registered);
        assertTrue(production.descriptors().stream().noneMatch(d -> d.category() == GameCategory.WORD_CARD));
        var compatibility = com.aoo.bcg.gamespi.GameRegistryFactory.compatibility();
        GameCatalogLoader.registerMissing(compatibility);
        assertEquals(10, compatibility.descriptors().stream()
                .filter(d -> d.category() == GameCategory.WORD_CARD).count());
        assertEquals("com.aoo.bcg.wordcard.WordCardFamilyProvider",
                compatibility.require(136, "1.0.0").getClass().getName());
    }

    @Test void catalogIdentityGuardsRejectMissingAndConflictingWordCardIdentity() {
        String[] missingFamily = {"136", "byzp", "BYZP", "WORD_CARD", "", "PROVINCE", "guangxi", "", "1.0.0", "1", "BYZP"};
        assertThrows(IllegalStateException.class,
                () -> GameCatalogLoader.requireCatalogIdentity(missingFamily, 136));
        GameDescriptor conflicting = new GameDescriptor(136, "byzp", "BYZP", GameCategory.WORD_CARD,
                "long-card:sichuan", RegionScope.PROVINCE, "guangxi", "", "1.0.0");
        assertThrows(IllegalStateException.class,
                () -> GameCatalogLoader.requireCategoryFamilyIdentity(conflicting));
        GameDescriptor valid = new GameDescriptor(136, "byzp", "BYZP", GameCategory.WORD_CARD,
                "word-card:pao-hu-zi", RegionScope.PROVINCE, "guangxi", "", "1.0.0");
        assertDoesNotThrow(() -> GameCatalogLoader.requireCategoryFamilyIdentity(valid));
    }

    @Test void productionGatewayRegistryIncludesCatalogBackedPdkProvider() {
        var registry = ProductionGatewayRuntimeProvider.loadGames();
        var provider = registry.require(2001, "chengdu2001-v1.0.0");
        assertEquals("chengdu2001", provider.descriptor().code());
        assertEquals("poker:pao-de-kuai", provider.descriptor().family());
    }

    @Test void pokerFamilyFactoriesResolveCanonicalGamesExactlyOnce() {
        List<PokerFamilyProviderFactory> factories = ServiceLoader.load(PokerFamilyProviderFactory.class)
                .stream().map(ServiceLoader.Provider::get).toList();
        assertEquals(3, factories.size(), "each external Poker family factory must be packaged once");
        assertUniqueFactory(factories, new GameDescriptor(2999, "chengdu2999", "扯旋", GameCategory.POKER,
                "poker:cx", RegionScope.CITY, "sichuan", "chengdu", "cx-v1.0.0"));
        assertUniqueFactory(factories, new GameDescriptor(5, "quanguo2998", "牛牛", GameCategory.POKER,
                "poker:betting", RegionScope.NATIONAL, "", "", "nn-v1.0.0"));
        assertUniqueFactory(factories, new GameDescriptor(9, "quanguo2997", "金花", GameCategory.POKER,
                "poker:compare-hand", RegionScope.NATIONAL, "", "", "zjh-v1.0.0"));
    }

    private static void assertUniqueFactory(List<PokerFamilyProviderFactory> factories, GameDescriptor descriptor) {
        var matches = factories.stream().map(factory -> factory.create(descriptor)).filter(java.util.Optional::isPresent).toList();
        assertEquals(1, matches.size(), descriptor.code() + " must resolve through exactly one factory");
        assertEquals(descriptor.code(), matches.getFirst().orElseThrow().descriptor().code());
    }

    static int catalogDataRows() {
        try (var input = GameProviderServiceLoaderTest.class.getResourceAsStream("/game-catalog-528.tsv")) {
            assertNotNull(input, "current Catalog resource must be packaged");
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                assertNotNull(reader.readLine(), "current Catalog header must be present");
                return (int) reader.lines().filter(line -> !line.isBlank()).count();
            } catch (java.io.IOException failure) {
                throw new AssertionError("cannot read current Catalog", failure);
            }
        } catch (java.io.IOException failure) {
            throw new AssertionError("cannot open current Catalog", failure);
        }
    }
}

class CatalogLifecycleContractTest {
    @Test void catalogAndProvidersRemainUniqueAndCatalogBridgesExecutable(){
        var registry=com.aoo.bcg.gamespi.GameRegistryFactory.compatibility();
        assertEquals(GameProviderServiceLoaderTest.catalogDataRows(), GameCatalogLoader.registerMissing(registry));
        assertEquals(GameProviderServiceLoaderTest.catalogDataRows(), registry.descriptors().size());
        assertEquals(registry.descriptors().size(),registry.descriptors().stream().map(d->d.gameId()+"/"+d.code()+"/"+d.version()).distinct().count());
        long expectedBridges=registry.descriptors().stream().filter(descriptor->"catalog-bridge".equals(registry.require(descriptor.gameId(),descriptor.version()).defaultConfiguration().get("migrationMode"))).count();
        int[]checked={0};
        registry.descriptors().forEach(descriptor->{var provider=registry.require(descriptor.gameId(),descriptor.version());if(!"catalog-bridge".equals(provider.defaultConfiguration().get("migrationMode")))return;long roomId=1_000_000L+descriptor.gameId();var room=provider.roomFactory().create(new RoomCreationContext(roomId,10L,Map.of()));String prefix=switch(descriptor.category()){case MAHJONG->"mahjong.";case POKER->"poker.";case LONG_CARD->"long_card.";case WORD_CARD->"word_card.";};var start=new GameCommandRequest(prefix+descriptor.code()+".start","start-"+descriptor.gameId(),1,roomId,1,descriptor.version(),"10",0,Map.of());assertTrue(provider.ruleComponents().stream().allMatch(rule->rule.execute(start).accepted()),descriptor.code());assertDoesNotThrow(()->provider.commandHandler().orElseThrow().handle(room,start),descriptor.code());assertNotNull(provider.reconnectViewProvider().orElseThrow().buildFor(10L,room),descriptor.code());assertEquals(roomId,provider.settlementProvider().orElseThrow().settle(room,1).roomId(),descriptor.code());checked[0]++;});
        assertEquals(expectedBridges,checked[0]);
    }
}
