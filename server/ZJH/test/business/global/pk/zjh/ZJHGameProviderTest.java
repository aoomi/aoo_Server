package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.poker.ComparePokerFamily;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.ServiceLoader;
import static org.junit.jupiter.api.Assertions.*;

class ZJHGameProviderTest {
    @Test void stableCodeAndPlayVersionUseTheirRequiredCases() {
        assertEquals("CN297", ZJHGameProvider.GAME_CODE);
        assertEquals("cn297-v1.0.0", ZJHGameProvider.PLAY_VERSION);
        assertTrue(ZJHGameProvider.PLAY_VERSION.matches("^[a-z][a-z0-9._-]*$"));
    }

    @Test void productionProviderPublishesCompleteRuntimeContracts() {
        ZJHGameProvider provider = new ZJHGameProvider();
        assertEquals("CN297", provider.descriptor().code());
        assertEquals("poker:compare-hand", provider.descriptor().family());
        assertEquals("cn297-v1.0.0", provider.descriptor().version());
        assertTrue(provider.commandHandler().isPresent());
        assertTrue(provider.reconnectViewProvider().isPresent());
        assertTrue(provider.settlementProvider().isPresent());
        assertFalse(provider.ruleComponents().isEmpty());
        assertEquals(provider.descriptor().version(), provider.ruleComponents().getFirst().componentVersion());
    }


    @Test void providerCreatesServerSeededAuthoritativeTable() {
        ZJHGameProvider provider = new ZJHGameProvider();
        ZJHTable table = provider.roomFactory().create(new RoomCreationContext(10, 20,
                Map.of("seatLimit", 8, "mustBlindRounds", 0, "randomSeed", 88L))).requireLegacyRoom(ZJHTable.class);
        table.join(0, 20); table.join(1, 21); table.join(2, 22);
        table.ready(0, true); table.ready(1, true); table.ready(2, true); table.start();
        assertEquals(ZJHTable.State.PLAYING, table.state());
        table.look(0);
        assertEquals(3, table.handView(20, 0).size());
        assertEquals(java.util.List.of(0, 0, 0), table.handView(20, 1));
        assertNotEquals(88L, table.randomSeed(), "client/room rules must not choose the shuffle seed");
    }

    @Test void publishedCreateRulesPreserveDefaultsOwnerAndEveryClientField() {
        ZJHGameProvider provider = new ZJHGameProvider();
        ZJHTable defaults = provider.roomFactory().create(new RoomCreationContext(11, 2001, Map.of()))
                .requireLegacyRoom(ZJHTable.class);
        assertEquals(Map.ofEntries(
                Map.entry("seatLimit", 8), Map.entry("minimumPlayers", 2), Map.entry("totalRounds", 10),
                Map.entry("operationSeconds", 10), Map.entry("compareStartRound", 5), Map.entry("maximumBet", 50),
                Map.entry("mustBlindRounds", 1), Map.entry("baseBet", 1), Map.entry("aaaBonus", 20),
                Map.entry("leopardBonus", 10), Map.entry("straightFlushBonus", 5)),
                defaults.authoritativeState().get("rules"));
        assertEquals(2001L, defaults.ownerId());
        assertEquals(2001L, defaults.viewFor(9999).get("ownerPlayerId"));

        Map<String, Object> published = Map.ofEntries(
                Map.entry("seatLimit", 10), Map.entry("minimumPlayers", 6), Map.entry("totalRounds", 30),
                Map.entry("operationSeconds", 20), Map.entry("compareStartRound", 1), Map.entry("maximumBet", 20),
                Map.entry("mustBlindRounds", 2), Map.entry("baseBet", 10), Map.entry("aaaBonus", 40),
                Map.entry("leopardBonus", 25), Map.entry("straightFlushBonus", 15));
        ZJHTable configured = provider.roomFactory().create(new RoomCreationContext(12, 2002, published))
                .requireLegacyRoom(ZJHTable.class);
        assertEquals(published, configured.authoritativeState().get("rules"));
        assertEquals(2002L, configured.ownerId());
    }

    @Test void providerRejectsEveryOutOfWorkbookCreateOptionBeforeRoomCreation() {
        ZJHGameProvider provider = new ZJHGameProvider();
        for (Map<String, Object> invalid : java.util.List.of(
                Map.of("totalRounds", 15), Map.of("minimumPlayers", 3), Map.of("operationSeconds", 12),
                Map.of("compareStartRound", 2), Map.of("maximumBet", 30), Map.of("mustBlindRounds", 3),
                Map.of("baseBet", 3), Map.of("totalRounds", 10.5), Map.of("seatLimit", "8"))) {
            assertThrows(IllegalArgumentException.class, () -> provider.roomFactory()
                    .create(new RoomCreationContext(13, 2003, invalid)), invalid::toString);
        }
    }

    @Test void pokerFamilyFactoryServiceLoaderDiscoversZjh() {
        GameDescriptor descriptor = new ZJHGameProvider().descriptor();
        GameProvider provider = ServiceLoader.load(PokerFamilyProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .map(factory -> factory.create(descriptor))
                .flatMap(java.util.Optional::stream)
                .findFirst()
                .orElseThrow();
        assertEquals(ZJHGameProvider.GAME_CODE, provider.descriptor().code());
    }

    @Test void pokerFamilyFactoryRejectsDescriptorsItDoesNotOwn() {
        ZJHPokerFamilyProviderFactory factory = new ZJHPokerFamilyProviderFactory();
        GameDescriptor owned = new ZJHGameProvider().descriptor();
        assertTrue(factory.create(owned).isPresent());
        assertTrue(factory.create(descriptor("OTHER", ComparePokerFamily.CODE, GameCategory.POKER)).isEmpty());
        assertTrue(factory.create(descriptor(ZJHGameProvider.GAME_CODE, "poker:other", GameCategory.POKER)).isEmpty());
        assertTrue(factory.create(descriptor(ZJHGameProvider.GAME_CODE, ComparePokerFamily.CODE, GameCategory.MAHJONG)).isEmpty());
    }

    @Test void zjhIsNotRegisteredAsGenericGameProvider() {
        assertTrue(ServiceLoader.load(GameProvider.class).stream()
                .noneMatch(provider -> provider.type().equals(ZJHGameProvider.class)));
    }

    private static GameDescriptor descriptor(String code, String family, GameCategory category) {
        return new GameDescriptor(9, code, "test", category, family,
                RegionScope.NATIONAL, "", "", ZJHGameProvider.PLAY_VERSION);
    }
}
