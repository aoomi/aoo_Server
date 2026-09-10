package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameRoomHandle;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.RoomMembershipLifecycle;

final class PokerDispatchCommandHandlerTest {
    @Test void canonicalEnvelopeAuthenticatesSeatAndDrivesReadyState() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        assertThrows(SecurityException.class, () -> handler.handle(room,
                new GameCommandRequest("poker.LS201.dispatch", "wrong-code", 1, 9, 1, "pdk-v1", "20", 0,
                        Map.of("action", "njpdk.CNJPDKEnterRoom", "payload", Map.of()))));
        handler.handle(room, request("j", 1, "20", 0, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("u", 2, "20", 0, "njpdk.CNJPDKUnReadyRoom", Map.of()));
        assertThrows(IllegalStateException.class,
                () -> handler.handle(room, request("s", 3, "20", 0, "njpdk.CNJPDKStartGame", Map.of())));
        assertThrows(SecurityException.class,
                () -> handler.handle(room, request("r-bad", 4, "21", 0, "njpdk.CNJPDKReadyRoom", Map.of())));
        var result = handler.handle(room, request("r", 5, "20", 0, "njpdk.CNJPDKReadyRoom", Map.of()));
        assertEquals("njpdk.CNJPDKReadyRoom", result.body().get("action"));
        assertEquals(session.stateVersion(), result.body().get("stateVersion"));
        assertNotEquals(5L, session.stateVersion(), "stateVersion must not copy the client sequence");
    }

    @Test void canonicalEnvelopeExposesHintAndContinueOperations() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        handler.handle(room, request("join-1", 1, "21", 1, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("join-2", 2, "22", 2, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("join-3", 3, "23", 3, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("ready-0", 4, "20", 0, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-1", 5, "21", 1, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-2", 6, "22", 2, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-3", 7, "23", 3, "njpdk.CNJPDKReadyRoom", Map.of()));
        int seat = ((Number) session.viewFor(20).get("currentSeat")).intValue();
        String player = String.valueOf(20 + seat);
        @SuppressWarnings("unchecked") var seats = (Map<Integer,Object>) session.viewFor(Long.parseLong(player)).get("seats");
        @SuppressWarnings("unchecked") var next = (Map<String,Object>) seats.get((seat + 1) % 4);
        int nextPlayerCardCount = ((Number) next.get("cardCount")).intValue();
        var hint = handler.handle(room, request("hint", 8, player, seat, "hint", Map.of("nextPlayerCardCount", nextPlayerCardCount)));
        assertEquals("hint", hint.body().get("action"));
        assertThrows(IllegalStateException.class,
                () -> handler.handle(room, request("continue", 9, player, seat, "njpdk.CNJPDKContinueGame", Map.of())));
    }

    @Test void ownerDissolvesWaitingRoomAndMemberCannotEscalatePrivileges() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        handler.handle(room, request("join", 1, "21", 1, "njpdk.CNJPDKEnterRoom", Map.of()));
        assertThrows(SecurityException.class, () -> handler.handle(room,
                request("bad", 2, "21", 1, "njpdk.CNJPDKDissolveRoom", Map.of())));
        var result = handler.handle(room,
                request("owner", 3, "20", 0, "njpdk.CNJPDKDissolveRoom", Map.of()));
        assertEquals(true, result.body().get("roomTerminal"));
        assertEquals("OWNER_DISSOLVED", result.body().get("roomTerminalReason"));
    }

    @Test void canonicalCommonRoomActionRunsInsideStableBusinessEnvelope() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var result = new PokerDispatchCommandHandler().handle(room,
                request("canonical-dissolve", 1, "20", 0, "common.room.dissolve_req", Map.of()));
        assertEquals("common.room.dissolve_req", result.body().get("action"));
        assertEquals(true, result.body().get("roomTerminal"));
        assertEquals("OWNER_DISSOLVED", result.body().get("roomTerminalReason"));
    }

    @Test void directCommonRoomApprovalCarriesTerminalMarkerForGatewayCompletion() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        handler.handle(room, new GameCommandRequest("common.room.join_req", "join", 1, 9, 0,
                "pdk-v1", "21", 1, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.join_req", "join-22", 2, 9, 0,
                "pdk-v1", "22", 2, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.join_req", "join-23", 3, 9, 0,
                "pdk-v1", "23", 3, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.ready_req", "ready-owner", 4, 9, 0,
                "pdk-v1", "20", 0, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.ready_req", "ready-guest", 5, 9, 0,
                "pdk-v1", "21", 1, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.ready_req", "ready-22", 6, 9, 0,
                "pdk-v1", "22", 2, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.ready_req", "ready-23", 7, 9, 0,
                "pdk-v1", "23", 3, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.dissolve_req", "dissolve", 8, 9, 1,
                "pdk-v1", "20", 0, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.dissolve_agree_req",
                "agree-21", 9, 9, 1, "pdk-v1", "21", 1, Map.of()));
        handler.handle(room, new GameCommandRequest("common.room.dissolve_agree_req",
                "agree-22", 10, 9, 1, "pdk-v1", "22", 2, Map.of()));
        var approved = handler.handle(room, new GameCommandRequest("common.room.dissolve_agree_req",
                "agree-23", 11, 9, 1, "pdk-v1", "23", 3, Map.of()));

        assertTrue(session.isTerminal());
        assertEquals("DISSOLVED", approved.body().get("phase"));
        assertEquals(true, approved.body().get("roomTerminal"));
        assertEquals("VOTE_APPROVED", approved.body().get("roomTerminalReason"));
    }

    @Test void playerControlsOnlyOwnTrusteeshipAfterRoundStarts() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        handler.handle(room, request("join", 1, "21", 1, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("join-2", 2, "22", 2, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("join-3", 3, "23", 3, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("ready-0", 4, "20", 0, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-1", 5, "21", 1, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-2", 6, "22", 2, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-3", 7, "23", 3, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("trust", 8, "21", 1, "njpdk.CNJPDKTrusteeship", Map.of("trusteeship", true)));
        @SuppressWarnings("unchecked") var seats = (Map<Integer,Object>) session.viewFor(21).get("seats");
        @SuppressWarnings("unchecked") var ownSeat = (Map<String,Object>) seats.get(1);
        assertEquals(true, ownSeat.get("hosting"));
        assertThrows(SecurityException.class, () -> handler.handle(room,
                request("spoof", 9, "20", 1, "njpdk.CNJPDKTrusteeship", Map.of("trusteeship", false))));
        handler.handle(room, request("cancel", 10, "21", 1, "njpdk.CNJPDKTrusteeship", Map.of("trusteeship", false)));
        @SuppressWarnings("unchecked") var updatedSeats = (Map<Integer,Object>) session.viewFor(21).get("seats");
        @SuppressWarnings("unchecked") var updatedOwnSeat = (Map<String,Object>) updatedSeats.get(1);
        assertEquals(false, updatedOwnSeat.get("hosting"));
    }

    @Test void memberLeavesWaitingRoomWithoutClosingIt() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 4, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        handler.handle(room, request("join", 1, "21", 1, "njpdk.CNJPDKEnterRoom", Map.of()));
        var result = handler.handle(room, request("leave", 2, "21", 1, "njpdk.CNJPDKExitRoom", Map.of()));
        assertEquals(true, result.body().get(RoomMembershipLifecycle.MEMBER_LEFT_FIELD));
        assertEquals(21L, result.body().get(RoomMembershipLifecycle.MEMBER_LEFT_ACCOUNT_ID_FIELD));
        @SuppressWarnings("unchecked") var players = (Map<Integer,Long>) session.authoritativeState().get("players");
        assertEquals(Map.of(0, 20L), players);
        assertFalse(session.isTerminal());
    }

    @Test void memberCannotLeaveAfterCardsAreDealt() {
        var family = new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
        var session = new PokerAuthoritativeSession(9, 20, 2, 7, family);
        var room = new GameRoomHandle(9, 629, "pdk-v1", session);
        var handler = new PokerDispatchCommandHandler();
        handler.handle(room, request("join", 1, "21", 1, "njpdk.CNJPDKEnterRoom", Map.of()));
        handler.handle(room, request("ready-0", 2, "20", 0, "njpdk.CNJPDKReadyRoom", Map.of()));
        handler.handle(room, request("ready-1", 3, "21", 1, "njpdk.CNJPDKReadyRoom", Map.of()));
        assertThrows(IllegalStateException.class, () -> handler.handle(room,
                request("leave", 4, "21", 1, "njpdk.CNJPDKExitRoom", Map.of())));
    }

    private static GameCommandRequest request(String id, long sequence, String user, int seat,
                                               String action, Map<String,Object> payload) {
        return new GameCommandRequest("poker.NJ201.dispatch", id, sequence, 9, 1, "pdk-v1", user, seat,
                Map.of("action", action, "payload", payload));
    }
}
