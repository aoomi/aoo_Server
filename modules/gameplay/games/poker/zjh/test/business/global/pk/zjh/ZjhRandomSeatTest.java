package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameRoomHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ZjhRandomSeatTest {
    @Test void sitAndLegacyJoinIgnoreRequestedSeatsOnEightAndTenSeatTables() {
        for (int limit : new int[] {8, 10}) {
            ZjhTable first = table(limit, 71, 83);
            ZjhTable second = table(limit, 71, 83);
            for (int index = 0; index < limit; index++) {
                long playerId = 1001L + index;
                String action = index % 2 == 0 ? ZjhCommandHandler.SIT : ZjhCommandHandler.JOIN;
                execute(first, action, playerId, 0, Map.of("seatId", 0));
                execute(second, action, playerId, limit - 1, Map.of("seatId", limit - 1));
                assertEquals(first.seatOf(playerId), second.seatOf(playerId));
                assertTrue(first.seatOf(playerId) >= 0 && first.seatOf(playerId) < limit);
            }
            assertEquals(limit, ((Map<?, ?>) first.authoritativeState().get("seats")).size());
            assertThrows(IllegalStateException.class,
                    () -> execute(first, ZjhCommandHandler.SIT, 9001, -1, Map.of()));
        }
    }

    @Test void duplicateAndFullRequestsDoNotAdvanceSeatSequence() {
        ZjhTable table = table(8, 101, 103);
        int actualSeat = table.sit(1001);
        Map<String, Object> beforeDuplicate = table.authoritativeState();
        assertThrows(IllegalStateException.class, () -> table.sit(1001));
        assertEquals(beforeDuplicate, table.authoritativeState());
        assertEquals(actualSeat, table.seatOf(1001));
        for (long playerId = 1002; playerId <= 1008; playerId++) table.sit(playerId);
        Map<String, Object> beforeFull = table.authoritativeState();
        assertThrows(IllegalStateException.class, () -> table.sit(1009));
        assertEquals(beforeFull, table.authoritativeState());
    }

    @Test void newSnapshotsRestoreJoinOrderAndKeepSeatRandomnessServerOnly() {
        ZjhTable table = table(10, 107, 109);
        table.sit(1001); table.sit(1002);
        ZjhTable first = ZjhTable.restore(88, table.authoritativeState());
        ZjhTable second = ZjhTable.restore(88, table.authoritativeState());
        assertEquals(3, first.authoritativeState().get("schemaVersion"));
        assertEquals(table.authoritativeState().get("seatJoinOrder"), first.authoritativeState().get("seatJoinOrder"));
        assertEquals(first.sit(1003), second.sit(1003));
        assertEquals(first.authoritativeState(), second.authoritativeState());
        assertFalse(first.viewFor(1003).containsKey("seatRandomSeed"));
        assertFalse(first.viewFor(1003).containsKey("seatRandomSequence"));
        assertFalse(first.viewFor(1003).containsKey("seatJoinOrder"));
        assertEquals(first.authoritativeState(), ZjhTable.restore(88, first.authoritativeState()).authoritativeState());
    }

    @Test void oldMultiSeatSnapshotsDoNotGuessMissingJoinOrder() {
        ZjhTable table = table(8, 107, 109);
        table.sit(1001); table.sit(1002);
        Map<String, Object> v2 = new LinkedHashMap<>(table.authoritativeState());
        v2.put("schemaVersion", 2);
        v2.remove("seatJoinOrder");
        assertThrows(IllegalArgumentException.class, () -> ZjhTable.restore(88, v2));
        Map<String, Object> v1 = new LinkedHashMap<>(v2);
        v1.remove("schemaVersion");
        v1.remove("seatRandomSeed");
        v1.remove("seatRandomSequence");
        assertThrows(IllegalArgumentException.class, () -> ZjhTable.restore(88, v1));
    }

    @Test void seatRandomnessDoesNotConsumeCardRandomness() {
        ZjhTable first = table(8, 113, 127);
        ZjhTable second = table(8, 113, 131);
        for (long playerId = 1001; playerId <= 1004; playerId++) {
            first.sit(playerId);
            second.sit(playerId);
        }
        first.start(); second.start();
        for (long playerId = 1001; playerId <= 1004; playerId++) {
            assertEquals(cardsFor(first, playerId), cardsFor(second, playerId));
        }
    }

    @Test void seededDeckPreservesCanonicalCardSet() {
        ZjhSetCard cards = new ZjhSetCard(null);
        cards.onXiPai();
        java.util.List<Integer> drawn = cards.popList(jsproto.c2s.cclass.pk.BasePocker.PockerList_AEnd.length);
        assertEquals(jsproto.c2s.cclass.pk.BasePocker.PockerList_AEnd.length, drawn.size());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(jsproto.c2s.cclass.pk.BasePocker.PockerList_AEnd)),
                new java.util.HashSet<>(drawn));
        assertEquals(drawn.size(), new java.util.HashSet<>(drawn).size());
    }

    @Test void differentCardSeedsChangePlayerHands() {
        ZjhTable first = table(8, 113, 127);
        ZjhTable second = table(8, 114, 127);
        for (long playerId = 1001; playerId <= 1004; playerId++) {
            first.sit(playerId);
            second.sit(playerId);
        }
        first.start(); second.start();
        assertNotEquals(cardsFor(first, 1001), cardsFor(second, 1001));
    }

    private static ZjhTable table(int limit, long cardSeed, long seatSeed) {
        return new ZjhTable(88, 1001, ZjhRules.from(Map.of("seatLimit", limit,
                "minimumPlayers", 2)), cardSeed, seatSeed);
    }

    private static void execute(ZjhTable table, String action, long playerId,
            int requestedSeat, Map<String, Object> body) {
        GameRoomHandle room = new GameRoomHandle(88, 9, ZjhGameProvider.PLAY_VERSION, table);
        new ZjhCommandHandler().handle(room, new GameCommandRequest(action,
                "seat-" + playerId, playerId, 88, 0, ZjhGameProvider.PLAY_VERSION,
                Long.toString(playerId), requestedSeat, body));
    }

    private static Object cardsFor(ZjhTable table, long playerId) {
        Map<?, ?> seats = (Map<?, ?>) table.authoritativeState().get("seats");
        Map<?, ?> seat = (Map<?, ?>) seats.get(table.seatOf(playerId));
        return seat.get("cards");
    }
}
