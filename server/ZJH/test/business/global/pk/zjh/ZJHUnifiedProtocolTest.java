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
    @Test void spectatorsSitAtomicallyAndReconnectKeepsTheirAuthoritativeRole() {
        ZJHTable table = new ZJHTable(88, 1001,
                ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 2)), 77);
        GameRoomHandle room = new GameRoomHandle(88, 9, ZJHGameProvider.PLAY_VERSION, table);
        ZJHCommandHandler handler = new ZJHCommandHandler();
        assertEquals("SPECTATOR", handler.handle(room,
                request(ZJHCommandHandler.STATE, "owner-watch", 1, "1001", 0, Map.of()))
                .body().asMap().get("viewerRole"));
        assertThrows(SecurityException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.BET, "watcher-bet", 2, "1001", 0, Map.of("amount", 1))));

        Map<String, Object> owner = handler.handle(room,
                request(ZJHCommandHandler.SIT, "owner-sit", 3, "1001", 0, Map.of("seatId", 0))).body().asMap();
        Map<String, Object> member = handler.handle(room,
                request(ZJHCommandHandler.SIT, "member-sit", 4, "1002", 0, Map.of("seatId", 1))).body().asMap();
        assertEquals("SEATED", owner.get("viewerRole"));
        assertEquals("SEATED", member.get("viewerRole"));
        assertTrue(((Map<?, ?>)((Map<?, ?>)member.get("seats")).get(0)).get("ready").equals(true));
        assertTrue(((Map<?, ?>)((Map<?, ?>)member.get("seats")).get(1)).get("ready").equals(true));
        assertThrows(IllegalStateException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.SIT, "duplicate-player", 5, "1001", 0, Map.of("seatId", 2))));
        assertThrows(IllegalStateException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.SIT, "occupied-seat", 6, "1003", 0, Map.of("seatId", 1))));
        assertThrows(IllegalArgumentException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.SIT, "invalid-seat", 7, "1003", 0, Map.of("seatId", 8))));
        assertEquals("SPECTATOR", new ZJHReconnectViewService().build(table, 1003).get("viewerRole"));
        assertEquals("SEATED", new ZJHReconnectViewService().build(table, 1002).get("viewerRole"));
        handler.handle(room, request(ZJHCommandHandler.START, "owner-start", 8, "1001", 0, Map.of()));
        assertEquals(ZJHTable.State.PLAYING, table.state(), "two seated humans automatically meet the start condition");
    }

    @Test void ownerAuthorityComesFromSnapshotsAndSurvivesReconnect() {
        ZJHTable table = new ZJHTable(88, 1001, 8, 77);
        GameRoomHandle room = new GameRoomHandle(88, 9, ZJHGameProvider.PLAY_VERSION, table);
        ZJHCommandHandler handler = new ZJHCommandHandler();
        Map<String, Object> ownerJoin = handler.handle(room,
                request(ZJHCommandHandler.JOIN, "owner-join", 1, "1001", 0, Map.of())).body().asMap();
        Map<String, Object> memberJoin = handler.handle(room,
                request(ZJHCommandHandler.JOIN, "member-join", 2, "1002", 1, Map.of())).body().asMap();
        assertEquals(1001L, ownerJoin.get("ownerPlayerId"));
        assertEquals(1001L, memberJoin.get("ownerPlayerId"));
        assertEquals(1001L, new ZJHReconnectViewService().build(table, 1002).get("ownerPlayerId"));

        handler.handle(room, request(ZJHCommandHandler.READY, "owner-ready", 3, "1001", 0, Map.of("ready", true)));
        handler.handle(room, request(ZJHCommandHandler.READY, "member-ready", 4, "1002", 1, Map.of("ready", true)));
        assertThrows(SecurityException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.START, "member-start", 5, "1002", 1, Map.of())));
        handler.handle(room, request(ZJHCommandHandler.START, "owner-start", 6, "1001", 0, Map.of()));
        handler.handle(room, request(ZJHCommandHandler.FOLD, "owner-fold", 7, "1001", 0, Map.of()));
        assertEquals(ZJHTable.State.ROUND_FINISHED, table.state());
        assertThrows(SecurityException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.CONTINUE, "member-continue", 8, "1002", 1, Map.of())));
        handler.handle(room, request(ZJHCommandHandler.CONTINUE, "owner-continue", 9, "1001", 0, Map.of()));
        assertEquals(2, table.roundNo());
        assertEquals(1001L, new ZJHReconnectViewService().build(table, 1002).get("ownerPlayerId"));
    }

    @Test void commandsEnforceSeatOwnershipAndReconnectHidesOtherHands() {
        ZJHTable table = new ZJHTable(88, 1001,
                ZJHRules.from(Map.of("seatLimit", 8, "mustBlindRounds", 0)), 7);
        GameRoomHandle room = new GameRoomHandle(88, 9, ZJHGameProvider.PLAY_VERSION, table);
        ZJHCommandHandler handler = new ZJHCommandHandler();
        handler.handle(room, request(ZJHCommandHandler.JOIN, "j1", 1, "1001", 0, Map.of()));
        handler.handle(room, request(ZJHCommandHandler.JOIN, "j2", 2, "1002", 1, Map.of()));
        assertThrows(IllegalStateException.class, () -> handler.handle(room,
                request(ZJHCommandHandler.START, "too-early", 3, "1001", 0, Map.of())));
        handler.handle(room, request(ZJHCommandHandler.READY, "r1", 4, "1001", 0, Map.of("ready", true)));
        handler.handle(room, request(ZJHCommandHandler.READY, "r2", 5, "1002", 1, Map.of("ready", true)));
        handler.handle(room, request(ZJHCommandHandler.START, "s1", 6, "1001", 0, Map.of()));
        handler.handle(room, request(ZJHCommandHandler.LOOK, "l1", 7, "1001", 0, Map.of()));

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
        ZJHTable table = new ZJHTable(88, 1001, 8, 77);
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

    @Test void rejectsMismatchedRoomAndPlayVersionBeforeMutation() {
        ZJHTable table = new ZJHTable(88, 1001, 8, 77);
        ZJHCommandHandler handler = new ZJHCommandHandler();
        GameRoomHandle room = new GameRoomHandle(88, 9, ZJHGameProvider.PLAY_VERSION, table);
        assertThrows(SecurityException.class, () -> handler.handle(room,
                new GameCommandRequest(ZJHCommandHandler.JOIN, "wrong-room", 1, 89, 0,
                        ZJHGameProvider.PLAY_VERSION, "1001", 0, Map.of())));
        assertThrows(SecurityException.class, () -> handler.handle(room,
                new GameCommandRequest(ZJHCommandHandler.JOIN, "wrong-version", 2, 88, 0,
                        "cn297-v0", "1001", 0, Map.of())));
        assertTrue(table.players().isEmpty());
    }

    @Test void dispatchBridgeUnwrapsSharedProtocolClientAuthorityEnvelope() {
        ZJHTable table = new ZJHTable(88, 1001, 8, 77);
        GameRoomHandle room = new GameRoomHandle(88, 9, ZJHGameProvider.PLAY_VERSION, table);
        Map<String, Object> clientBody = Map.of("roomId", 88, "roundNo", 0,
                "playVersion", ZJHGameProvider.PLAY_VERSION, "expectedStateVersion", 0,
                "action", ZJHCommandHandler.JOIN, "payload", Map.of());
        GameCommandRequest dispatch = new GameCommandRequest(ZJHCommandHandler.DISPATCH, "dispatch-1", 1,
                88, 0, ZJHGameProvider.PLAY_VERSION, "1001", 0, clientBody);
        Map<String, Object> view = new ZJHCommandHandler().handle(room, dispatch).body().asMap();
        assertEquals("CN297", view.get("gameCode"));
        assertEquals(ZJHGameProvider.PLAY_VERSION, view.get("playVersion"));
        assertTrue(table.ownsSeat(1001, 0));
        assertThrows(SecurityException.class, () -> new ZJHCommandHandler().handle(room,
                new GameCommandRequest(ZJHCommandHandler.DISPATCH, "dispatch-stale", 2,
                        88, 0, ZJHGameProvider.PLAY_VERSION, "1002", 1,
                        Map.of("action", ZJHCommandHandler.JOIN, "payload", Map.of(),
                                "expectedStateVersion", 0))));
        assertFalse(table.ownsSeat(1002, 1));
    }

    private static GameCommandRequest request(String msgId, String requestId, long seq,
            String playerId, int seatId, Map<String, Object> body) {
        return new GameCommandRequest(msgId, requestId, seq, 88, 1,
                ZJHGameProvider.PLAY_VERSION, playerId, seatId, body);
    }
}
