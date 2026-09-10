package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.RoomCreationContext;
import org.junit.jupiter.api.Test;

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
                "business.global.pk.zjh.ZJHGameProvider",
                "business.global.pk.zypk.ZYPKGameProvider");
        var providers = ServiceLoader.load(GameProvider.class).stream()
                .filter(entry -> !com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry.isInternalRuntimeProvider(entry.get()))
                .filter(entry -> !entry.type().getName().equals("business.global.pk.njpdk.NJPDKGameProvider"))
                .toList();
        Set<String> actual = providers.stream()
                .map(provider -> provider.type().getName())
                .collect(Collectors.toSet());
        assertEquals(expected.size(), providers.size(), "only standalone lifecycle providers belong in ServiceLoader");
        assertEquals(expected, actual, "all native providers must survive production classpath assembly");
    }

    @Test void catalogExpandsLongAndWordRowsThroughFamilyProviders() {
        var registry = new com.aoo.bcg.gamespi.GameRegistry();
        assertEquals(530, GameCatalogLoader.registerMissing(registry));
        for (int id : List.of(80,138,210,211))
            assertEquals("com.aoo.bcg.longcard.LongCardFamilyProvider", registry.require(id,"1.0.0").getClass().getName());
        for (int id : List.of(136,153,176,302,342,404,407,462,490,596))
            assertEquals("com.aoo.bcg.wordcard.WordCardFamilyProvider", registry.require(id,"1.0.0").getClass().getName());
    }

    @Test void productionGatewayRegistryIncludesCatalogBackedPdkProvider() {
        var registry = ProductionGatewayRuntimeProvider.loadGames();
        var provider = registry.require(8, "1.0.0");
        assertEquals("CD201", provider.descriptor().code());
        assertEquals("poker:pao-de-kuai", provider.descriptor().family());
    }
}

class CatalogLifecycleContractTest {
    @Test void catalogAndProvidersRemainUniqueAndCatalogBridgesExecutable(){var registry=BootstrapAPP.loadRegistry();assertEquals(534,registry.descriptors().size());assertEquals(534,registry.descriptors().stream().map(d->d.gameId()+"/"+d.code()+"/"+d.version()).distinct().count());int[]checked={0};registry.descriptors().forEach(descriptor->{var provider=registry.require(descriptor.gameId());if(!"catalog-bridge".equals(provider.defaultConfiguration().get("migrationMode")))return;long roomId=1_000_000L+descriptor.gameId();var room=provider.roomFactory().create(new RoomCreationContext(roomId,10L,Map.of()));String prefix=switch(descriptor.category()){case MAHJONG->"mahjong.";case POKER->"poker.";case LONG_CARD->"long_card.";case WORD_CARD->"word_card.";};var start=new GameCommandRequest(prefix+descriptor.code()+".start","start-"+descriptor.gameId(),1,roomId,1,descriptor.version(),"10",0,Map.of());assertTrue(provider.ruleComponents().stream().allMatch(rule->rule.execute(start).accepted()),descriptor.code());assertDoesNotThrow(()->provider.commandHandler().orElseThrow().handle(room,start),descriptor.code());assertNotNull(provider.reconnectViewProvider().orElseThrow().buildFor(10L,room),descriptor.code());assertEquals(roomId,provider.settlementProvider().orElseThrow().settle(room,1).roomId(),descriptor.code());checked[0]++;});assertEquals(146,checked[0]);}
}
