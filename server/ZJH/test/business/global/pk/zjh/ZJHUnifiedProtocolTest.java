package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameRoomHandle;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.aoo.bcg.common.event.InMemoryRoomEventJournal;
import com.aoo.bcg.common.recovery.InMemoryRoomSnapshotStore;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ZJHUnifiedProtocolTest {
    @Test void commandsEnforceSeatOwnershipAndReconnectHidesOtherHands() {
        ZJHTable table = new ZJHTable(88, 1001, 5, 7);
        GameRoomHandle room = new GameRoomHandle(88, 9, ZJHGameProvider.PLAY_VERSION, table);
        ZJHCommandHandler handler = new ZJHCommandHandler();
        handler.handle(room, request(ZJHCommandHandler.JOIN, "j1", 1, "1001", 0, Map.of()));
        handler.handle(room, request(ZJHCommandHandler.JOIN, "j2", 2, "1002", 1, Map.of()));
        assertThrows(IllegalStateException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.START, "too-early", 3, "1001", 0, Map.of())));
        handler.handle(room, request(ZJHCommandHandler.READY, "r1", 4, "1001", 0, Map.of("ready", true)));
        handler.handle(room, request(ZJHCommandHandler.READY, "r2", 5, "1002", 1, Map.of("ready", true)));
        handler.handle(room, request(ZJHCommandHandler.START, "s1", 6, "1001", 0, Map.of()));

        @SuppressWarnings("unchecked") Map<Integer, Object> seats = (Map<Integer, Object>)
                new ZJHReconnectViewService().build(table, 1001).get("seats");
        @SuppressWarnings("unchecked") Map<String, Object> own = (Map<String, Object>) seats.get(0);
        @SuppressWarnings("unchecked") Map<String, Object> other = (Map<String, Object>) seats.get(1);
        assertTrue(((List<?>) own.get("cards")).stream().anyMatch(card -> ((Number) card).intValue() != 0));
        assertTrue(((List<?>) other.get("cards")).stream().allMatch(card -> ((Number) card).intValue() == 0));
        assertThrows(SecurityException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.FOLD, "f1", 4, "1002", 0, Map.of())));
    }

    @Test void persistsRestoresAndReplaysAuthoritativeState() {
        ZJHTable table = new ZJHTable(88, 1001, 3, 77);
        table.join(0, 1001); table.join(1, 1002); table.join(2, 1003);
        table.ready(0, true); table.ready(1, true); table.ready(2, true); table.start();
        InMemoryRoomEventJournal events = new InMemoryRoomEventJournal();
        InMemoryRoomSnapshotStore snapshots = new InMemoryRoomSnapshotStore();
        ZJHPersistenceService persistence = new ZJHPersistenceService(events, snapshots,
                Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC));
        persistence.persist(table, 1, 1, 9, "start-1", ZJHCommandHandler.START, Map.of());
        RoomSnapshot snapshot = snapshots.latest(88).orElseThrow();
        ZJHTable restored = new ZJHStateRestorer().fromSnapshot(snapshot);
        assertEquals(table.authoritativeState(), restored.authoritativeState());
        assertEquals(table.viewFor(1001), restored.viewFor(1001));

        restored.fold(restored.operatorSeat());
        Map<String, Object> envelope = Map.of("eventType", ZJHCommandHandler.FOLD,
                "payload", Map.of("seatId", 0), "stateAfter", restored.authoritativeState());
        ZJHTable replayed = new ZJHStateRestorer().replay(table, java.util.List.of(envelope));
        assertEquals(restored.authoritativeState(), replayed.authoritativeState());
        assertEquals(restored.viewFor(1002), replayed.viewFor(1002));
        assertThrows(IllegalArgumentException.class, () -> new ZJHStateRestorer().replay(table,
                java.util.List.of(Map.of("eventType", "broken"))));
    }

    private static GameCommandRequest request(String msgId, String requestId, long seq,
            String playerId, int seatId, Map<String, Object> body) {
        return new GameCommandRequest(msgId, requestId, seq, 88, 1,
                ZJHGameProvider.PLAY_VERSION, playerId, seatId, body);
    }
}
