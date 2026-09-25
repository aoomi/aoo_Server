package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.poker.ComparePokerFamily;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.ServiceLoader;
import static org.junit.jupiter.api.Assertions.*;

class ZJHGameProviderTest {
    @Test void publishesCanonicalLifecycleMarkersBeforeAndAfterRestore() {
        ZJHGameProvider provider = new ZJHGameProvider();
        AuthoritativeGameSession created = provider.createAuthoritativeSession(
                new RoomCreationContext(858345, 619, Map.of())).orElseThrow();
        assertEquals("WAITING", created.authoritativeState().get("phase"));
        assertEquals(false, created.authoritativeState().get("started"));

        AuthoritativeGameSession restored = provider.restoreAuthoritativeSession(created.authoritativeState())
                .orElseThrow();
        assertEquals("WAITING", restored.authoritativeState().get("phase"));
        assertEquals(false, restored.authoritativeState().get("started"));
        assertEquals(created.authoritativeState(), restored.authoritativeState());
    }
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
        table.ready(table.seatOf(20), true); table.ready(table.seatOf(21), true);
        table.ready(table.seatOf(22), true); table.start();
        assertEquals(ZJHTable.State.PLAYING, table.state());
        int ownerSeat = table.seatOf(20);
        table.look(ownerSeat);
        assertEquals(3, table.handView(20, ownerSeat).size());
        assertEquals(java.util.List.of(0, 0, 0), table.handView(20, table.seatOf(21)));
        assertNotEquals(88L, table.randomSeed(), "client/room rules must not choose the shuffle seed");
    }

    @Test void providerPublishesOneRestorableAuthorityWithoutShadowState() {
        ZJHGameProvider provider = new ZJHGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(14, 2004,
                Map.of("seatLimit", 8, "minimumPlayers", 2, "mustBlindRounds", 0)));
        AuthoritativeGameSession authority = room.requireAuthoritativeSession();
        ZJHTable table = room.requireLegacyRoom(ZJHTable.class);

        authority.execute(command(14, "sit-owner", 1, "2004", 0,
                ZJHCommandHandler.SIT, Map.of()));
        authority.execute(command(14, "sit-member", 2, "2005", 1,
                ZJHCommandHandler.SIT, Map.of()));
        authority.execute(command(14, "start", 3, "2004", 0,
                ZJHCommandHandler.START, Map.of()));

        assertSame(table, room.requireLegacyRoom(ZJHTable.class));
        assertEquals(table.stateVersion(), authority.stateVersion());
        assertEquals(14L, authority.authoritativeState().get("roomId"));
        assertTrue(authority.invariantViolations().isEmpty());

        AuthoritativeGameSession restored = provider.restoreAuthoritativeSession(authority.authoritativeState())
                .orElseThrow();
        assertEquals(authority.authoritativeState(), restored.authoritativeState());
        assertEquals(authority.viewFor(2004), restored.viewFor(2004));
        assertInstanceOf(ZJHTable.class, ((com.aoo.bcg.gamespi.LegacyCompatibleRoom) restored).legacyRoom());
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
        for (Map<String, Object> invalid : java.util.List.<Map<String, Object>>of(
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

    private static GameCommandRequest command(long roomId, String requestId, long sequence,
            String playerId, int seatId, String action, Map<String, Object> payload) {
        return new GameCommandRequest(action, requestId, sequence, roomId, 1,
                ZJHGameProvider.PLAY_VERSION, playerId, seatId, payload);
    }
}
