package business.global.pk.zypk;

import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.aoo.bcg.common.event.InMemoryRoomEventJournal;
import com.aoo.bcg.common.recovery.InMemoryRoomSnapshotStore;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.common.settlement.SettlementValidator;

import static org.junit.jupiter.api.Assertions.*;

class ZYPKTableTest {
    @Test void protectsPrivateCardsAndAdvancesAfterLegalAction() {
        ZYPKTable table = new ZYPKTable(1L, 10L, 3, 3, 1, Set.of(), 20260822L);
        table.join(0, 10L); table.join(1, 11L); table.join(2, 12L); table.start(1);
        assertEquals(1, table.operatorSeat());
        assertTrue(table.handView(11L, 1).stream().allMatch(card -> card != 0));
        assertTrue(table.handView(10L, 1).stream().allMatch(card -> card == 0));
        assertThrows(IllegalStateException.class, () -> table.operate(0, ZYPK_AnNiu.KanPai, List.of()));
        table.operate(1, ZYPK_AnNiu.KanPai, List.of());
        assertEquals(2, table.operatorSeat());
    }

    @Test void rejectsUnownedCardsWithoutChangingTurn() {
        ZYPKTable table = new ZYPKTable(2L, 20L, 2, 2, 0, Set.of(), 7L);
        table.join(0, 20L); table.join(1, 21L); table.start(0);
        assertThrows(IllegalArgumentException.class,
                () -> table.operate(0, ZYPK_AnNiu.OutCard, List.of(0x7fffffff)));
        assertEquals(0, table.operatorSeat());
    }

    @Test void appliesBetRevealCompareFoldAndRollbackSemantics() {
        ZYPKTable table = new ZYPKTable(3L, 30L, 3, 3, 0, Set.of(), 11L);
        table.join(0, 30L); table.join(1, 31L); table.join(2, 32L); table.start(0);

        table.operate(0, ZYPK_AnNiu.YaZhu, List.of(), 5, null);
        assertEquals(5, table.currentBet());
        assertEquals(5, table.seatState(0).wager());
        table.operate(1, ZYPK_AnNiu.GenZhu, List.of(), 0, null);
        assertEquals(5, table.seatState(1).wager());
        table.operate(2, ZYPK_AnNiu.KanPai, List.of());
        assertTrue(table.seatState(2).viewed());
        table.operate(0, ZYPK_AnNiu.BiPai, List.of(), 0, 1);
        assertTrue(table.handView(30L, 1).stream().allMatch(card -> card != 0));
        assertTrue(table.handView(31L, 0).stream().allMatch(card -> card != 0));
        table.operate(1, ZYPK_AnNiu.QiPai, List.of());
        assertTrue(table.seatState(1).folded());
        assertEquals(2, table.operatorSeat());
        table.rollbackLastOperation(1);
        assertFalse(table.seatState(1).folded());
        assertEquals(1, table.operatorSeat());
    }

    @Test void rejectsInvalidCompareAndBetWithoutAdvancing() {
        ZYPKTable table = new ZYPKTable(4L, 40L, 2, 2, 0, Set.of(), 13L);
        table.join(0, 40L); table.join(1, 41L); table.start(0);
        assertThrows(IllegalArgumentException.class,
                () -> table.operate(0, ZYPK_AnNiu.YaZhu, List.of(), 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> table.operate(0, ZYPK_AnNiu.BiPai, List.of(), 0, 0));
        assertEquals(0, table.operatorSeat());
    }

    @Test void enforcesConfiguredActions() {
        ZYPKTable table = new ZYPKTable(5L, 50L, 2, 2, 0, Set.of(),
                Set.of(ZYPK_AnNiu.OutCard), 17L);
        table.join(0, 50L); table.join(1, 51L); table.start(0);
        assertThrows(IllegalStateException.class,
                () -> table.operate(0, ZYPK_AnNiu.KanPai, List.of()));
        assertEquals(0, table.operatorSeat());
    }

    @Test void selectsDealerByFixedRotateRandomAndRobPolicies() {
        ZYPKTable rotated = tableWithThreePlayers(6L, 19L);
        rotated.start(ZYPKTable.DealerPolicy.ROTATE, null, 1, Set.of());
        assertEquals(2, rotated.dealerSeat());

        ZYPKTable fixed = tableWithThreePlayers(7L, 19L);
        fixed.start(ZYPKTable.DealerPolicy.FIXED, 1, null, Set.of());
        assertEquals(1, fixed.dealerSeat());

        ZYPKTable robbed = tableWithThreePlayers(8L, 19L);
        robbed.start(ZYPKTable.DealerPolicy.ROB, null, null, Set.of(2));
        assertEquals(2, robbed.dealerSeat());

        ZYPKTable randomA = tableWithThreePlayers(9L, 23L);
        ZYPKTable randomB = tableWithThreePlayers(10L, 23L);
        randomA.start(ZYPKTable.DealerPolicy.RANDOM, null, null, Set.of());
        randomB.start(ZYPKTable.DealerPolicy.RANDOM, null, null, Set.of());
        assertEquals(randomA.dealerSeat(), randomB.dealerSeat());
    }

    @Test void reconnectSnapshotNeverLeaksUnauthorizedCards() {
        ZYPKTable table = tableWithThreePlayers(11L, 29L);
        table.start(0);
        ZYPKPlayerView initial = table.reconnectView(60L);
        assertEquals(0, initial.viewerSeat());
        assertTrue(initial.seats().get(0).cards().stream().allMatch(card -> card != 0));
        assertTrue(initial.seats().get(1).cards().stream().allMatch(card -> card == 0));
        assertEquals(initial.seats().get(1).cardCount(), initial.seats().get(1).cards().size());

        table.operate(0, ZYPK_AnNiu.BiPai, List.of(), 0, 1);
        ZYPKPlayerView compared = table.reconnectView(60L);
        assertTrue(compared.seats().get(1).cards().stream().allMatch(card -> card != 0));
        assertTrue(compared.seats().get(2).cards().stream().allMatch(card -> card == 0));
        assertThrows(IllegalArgumentException.class, () -> table.reconnectView(999L));
    }

    @Test void persistsOrderedEventAndAuthoritativeRecoverySnapshot() {
        ZYPKTable table = tableWithThreePlayers(12L, 31L);
        table.start(0);
        InMemoryRoomEventJournal events = new InMemoryRoomEventJournal();
        InMemoryRoomSnapshotStore snapshots = new InMemoryRoomSnapshotStore();
        ZYPKPersistenceService persistence = new ZYPKPersistenceService(events, snapshots,
                Clock.fixed(Instant.parse("2026-08-22T12:00:00Z"), ZoneOffset.UTC));

        persistence.persist(table, 1, 1L, 3L, "request-1", "zypk.round.started", Map.of("dealerSeat", 0));
        var snapshot = snapshots.latest(12L).orElseThrow();
        assertEquals(62, snapshot.gameId());
        assertEquals(1L, snapshot.lastEventSequence());
        assertEquals(3L, snapshot.fencingToken());
        assertEquals("PLAYING", snapshot.authoritativeState().get("state"));
        assertTrue(snapshot.authoritativeState().containsKey("hands"));
        assertTrue(snapshot.authoritativeState().containsKey("deck"));
        assertEquals(1, events.after(12L, 0L).size());
    }

    @Test void restoresAuthoritativeStateWithoutChangingPlayerPerspective() {
        ZYPKTable original = tableWithThreePlayers(13L, 37L);
        original.start(0);
        original.operate(0, ZYPK_AnNiu.YaZhu, List.of(), 8, null);
        original.operate(1, ZYPK_AnNiu.GenZhu, List.of(), 0, null);
        ZYPKPersistenceService persistence = new ZYPKPersistenceService(new InMemoryRoomEventJournal(),
                new InMemoryRoomSnapshotStore(), Clock.systemUTC());
        RoomSnapshot snapshot = new RoomSnapshot(13L, ZYPKPersistenceService.GAME_ID,
                ZYPKPersistenceService.PLAY_VERSION, ZYPKPersistenceService.COMPONENT_VERSION,
                4L, 2L, Instant.now(), original.authoritativeState());

        ZYPKTable restored = new ZYPKStateRestorer().fromSnapshot(snapshot);
        assertEquals(original.operatorSeat(), restored.operatorSeat());
        assertEquals(original.currentBet(), restored.currentBet());
        assertEquals(original.reconnectView(60L), restored.reconnectView(60L));
        RoomSnapshot wrongGame = new RoomSnapshot(13L, 9, snapshot.playVersion(),
                snapshot.componentVersion(), 4L, 2L, Instant.now(), snapshot.authoritativeState());
        assertThrows(IllegalArgumentException.class, () -> new ZYPKStateRestorer().fromSnapshot(wrongGame));
    }

    @Test void replaysSelfDescribingEventsAfterSnapshot() {
        ZYPKTable before = tableWithThreePlayers(14L, 41L);
        before.start(0);
        RoomSnapshot snapshot = new RoomSnapshot(14L, ZYPKPersistenceService.GAME_ID,
                ZYPKPersistenceService.PLAY_VERSION, ZYPKPersistenceService.COMPONENT_VERSION,
                1L, 1L, Instant.now(), before.authoritativeState());
        ZYPKTable after = new ZYPKStateRestorer().fromSnapshot(snapshot);
        after.operate(0, ZYPK_AnNiu.YaZhu, List.of(), 6, null);
        Map<String, Object> event = Map.of("eventType", "zypk.player.bet",
                "payload", Map.of("seatId", 0, "amount", 6),
                "stateAfter", after.authoritativeState());

        ZYPKTable recovered = new ZYPKStateRestorer().replay(before, List.of(event));
        assertEquals(6, recovered.currentBet());
        assertEquals(1, recovered.operatorSeat());
        assertEquals(after.reconnectView(60L), recovered.reconnectView(60L));
        assertThrows(IllegalArgumentException.class,
                () -> new ZYPKStateRestorer().replay(before, List.of(Map.of("eventType", "broken"))));
    }

    @Test void routesStateAndOperationCommandsThroughGameSpi() {
        ZYPKTable table = tableWithThreePlayers(15L, 43L);
        table.start(0);
        ZYPKCommandHandler handler = new ZYPKCommandHandler();
        GameRoomHandle room = new GameRoomHandle(15L, 62, "zypk-v1.0.0", table);
        var state = handler.handle(room, new GameCommandRequest(ZYPKCommandHandler.STATE_REQUEST,
                "req-state", 1L, 15L, 1, "zypk-v1.0.0", "60", 0, Map.of()));
        assertEquals("poker.zypk.state_resp", state.msgId());
        var operation = handler.handle(room, new GameCommandRequest(ZYPKCommandHandler.OPERATE_REQUEST,
                "req-op", 2L, 15L, 1, "zypk-v1.0.0", "60", 0,
                Map.of("action", "YaZhu", "amount", 4)));
        assertEquals("poker.zypk.operate_resp", operation.msgId());
        assertEquals(4, table.currentBet());
        assertThrows(SecurityException.class, () -> handler.handle(room,
                new GameCommandRequest(ZYPKCommandHandler.STATE_REQUEST, "req-bad", 3L,
                        15L, 1, "zypk-v1.0.0", "61", 0, Map.of())));
    }

    @Test void transfersChipsAndProducesZeroSumSettlement() {
        ZYPKTable table = new ZYPKTable(16L, 70L, 3, 2, 0, Set.of(),
                Set.of(ZYPK_AnNiu.KanPai), 100, 47L);
        table.join(0, 70L); table.join(1, 71L); table.join(2, 72L); table.start(0);
        table.transferChips(0, 1, 25);
        assertEquals(75, table.seatState(0).chips());
        assertEquals(125, table.seatState(1).chips());
        var settlement = table.settlement(1);
        SettlementValidator.validate(settlement, 16L, 1, "zypk-v1.0.0", true);
        assertEquals(-25L, settlement.entries().stream().filter(e -> e.playerId() == 70L).findFirst().orElseThrow().scoreDelta());
        assertEquals(25L, settlement.entries().stream().filter(e -> e.playerId() == 71L).findFirst().orElseThrow().scoreDelta());
        assertThrows(IllegalArgumentException.class, () -> table.transferChips(0, 1, 1000));
    }

    @Test void supportsSimultaneousModeAndPublicDrawWithoutLeakingPrivateCards() {
        ZYPKTable table = new ZYPKTable(17L, 80L, 3, 2, 0, Set.of(),
                Set.of(ZYPK_AnNiu.BuMingPai, ZYPK_AnNiu.KanPai), 0,
                ZYPKTable.OperationMode.SIMULTANEOUS, 53L);
        table.join(0, 80L); table.join(1, 81L); table.join(2, 82L); table.start(0);
        table.operate(2, ZYPK_AnNiu.BuMingPai, List.of());
        List<Integer> outsiderView = table.handView(80L, 2);
        assertEquals(3, outsiderView.size());
        assertEquals(1, outsiderView.stream().filter(card -> card != 0).count());
        assertEquals(0, table.operatorSeat());
    }

    @Test void rollbackRestoresCardsDeckAndPublicVisibility() {
        ZYPKTable table = new ZYPKTable(18L, 90L, 2, 1, 0, Set.of(), 59L);
        table.join(0, 90L); table.join(1, 91L); table.start(0);
        ZYPKPlayerView before = table.reconnectView(90L);
        table.operate(0, ZYPK_AnNiu.BuMingPai, List.of());
        table.rollbackLastOperation(0);
        assertEquals(before, table.reconnectView(90L));
    }

    @Test void routesPassGiveAndSettlementCommands() {
        ZYPKTable table = new ZYPKTable(19L, 100L, 2, 2, 0, Set.of(),
                Set.of(ZYPK_AnNiu.KanPai), 100, 61L);
        table.join(0, 100L); table.join(1, 101L); table.start(0);
        ZYPKCommandHandler handler = new ZYPKCommandHandler();
        GameRoomHandle room = new GameRoomHandle(19L, 62, "zypk-v1.0.0", table);
        handler.handle(room, new GameCommandRequest(ZYPKCommandHandler.GIVE_REQUEST,
                "give", 1, 19L, 1, "zypk-v1.0.0", "100", 0,
                Map.of("targetSeat", 1, "amount", 10)));
        handler.handle(room, new GameCommandRequest(ZYPKCommandHandler.PASS_REQUEST,
                "pass", 2, 19L, 1, "zypk-v1.0.0", "100", 0, Map.of()));
        assertEquals(1, table.operatorSeat());
        var settled = handler.handle(room, new GameCommandRequest(ZYPKCommandHandler.SETTLE_REQUEST,
                "settle", 3, 19L, 1, "zypk-v1.0.0", "101", 1, Map.of("roundNo", 1)));
        assertEquals("poker.zypk.settle_resp", settled.msgId());
    }

    @Test void cannotRollbackAnotherPlayersOperationAndPassClearsOwnRollbackPoint() {
        ZYPKTable table = new ZYPKTable(20L, 110L, 2, 2, 0, Set.of(), 67L);
        table.join(0, 110L); table.join(1, 111L); table.start(0);
        table.operate(0, ZYPK_AnNiu.KanPai, List.of());
        assertThrows(SecurityException.class, () -> table.rollbackLastOperation(1));
        table.pass(1);
        assertDoesNotThrow(() -> table.rollbackLastOperation(0));
    }

    private static ZYPKTable tableWithThreePlayers(long roomId, long seed) {
        ZYPKTable table = new ZYPKTable(roomId, 60L, 3, 2, 0, Set.of(), seed);
        table.join(0, 60L); table.join(1, 61L); table.join(2, 62L);
        return table;
    }
}
