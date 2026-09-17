package com.aoo.bcg.poker.cd299;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Cd299SessionTurnTest {
    @Test
    void middleSeatDropDoesNotEndBettingBeforeRemainingPlayersAct() {
        Cd299Session session = bettingSession(4, 40L);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "bet-0");
        session.bet(1, Cd299Session.BetAction.DROP, 0, "drop-1");
        assertTurn(session, "BETTING", 2);

        session.bet(2, Cd299Session.BetAction.FOLLOW, 0, "bet-2");
        assertTurn(session, "BETTING", 3);
        session.bet(3, Cd299Session.BetAction.FOLLOW, 0, "bet-3");
        assertTurn(session, "ADD_CARD", 0);
    }

    @Test
    void firstSeatDropContinuesAtNextStableSeat() {
        Cd299Session session = bettingSession(4, 41L);
        session.bet(0, Cd299Session.BetAction.DROP, 0, "drop-0");
        assertTurn(session, "BETTING", 1);
    }

    @Test
    void consecutiveDropsWaitForEveryRemainingRealPlayer() {
        Cd299Session session = bettingSession(6, 42L);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "bet-0");
        session.bet(1, Cd299Session.BetAction.DROP, 0, "drop-1");
        session.bet(2, Cd299Session.BetAction.DROP, 0, "drop-2");
        assertTurn(session, "BETTING", 3);
        session.bet(3, Cd299Session.BetAction.FOLLOW, 0, "bet-3");
        session.bet(4, Cd299Session.BetAction.FOLLOW, 0, "bet-4");
        assertTurn(session, "BETTING", 5);
        session.bet(5, Cd299Session.BetAction.FOLLOW, 0, "bet-5");
        assertTurn(session, "ADD_CARD", 0);
    }

    @Test
    void finalTwoDropsSettleAsSoonAsOneRealPlayerRemains() {
        Cd299Session session = bettingSession(4, 43L);
        session.bet(0, Cd299Session.BetAction.DROP, 0, "drop-0");
        assertTurn(session, "BETTING", 1);
        session.bet(1, Cd299Session.BetAction.DROP, 0, "drop-1");
        assertTurn(session, "BETTING", 2);
        session.bet(2, Cd299Session.BetAction.DROP, 0, "drop-2");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
    }

    @Test
    void reconnectViewKeepsAuthoritativeCurrentSeat() {
        Cd299Session session = bettingSession(4, 44L);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "bet-0");
        session.bet(1, Cd299Session.BetAction.DROP, 0, "drop-1");
        assertEquals(2, session.viewFor(100L).get("currentSeat"));
        assertEquals(2, session.viewFor(103L).get("currentSeat"));
    }

    @Test
    void actingSeatBecomingThreeFlowerDoesNotSkipOtherPlayers() {
        boolean verified = false;
        for (long seed = 1; seed <= 10_000 && !verified; seed++) {
            Cd299Session session = bettingSession(4, seed);
            session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "bet-0");
            session.bet(1, Cd299Session.BetAction.FOLLOW, 0, "bet-1");
            session.bet(2, Cd299Session.BetAction.FOLLOW, 0, "bet-2");
            session.bet(3, Cd299Session.BetAction.FOLLOW, 0, "bet-3");
            session.addCard(0, "add-0");
            @SuppressWarnings("unchecked")
            java.util.List<Integer> flowers = (java.util.List<Integer>) state(session).get("threeFlowerSeats");
            if (flowers.contains(0)) {
                assertTurn(session, "ADD_CARD", 1);
                verified = true;
            }
        }
        assertTrue(verified, "test seeds must include a seat-0 three-flower deal");
    }

    private static Cd299Session bettingSession(int players, long seed) {
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", players));
        Cd299Session session = new Cd299Session(9000L + seed, 100L, seed, rules);
        for (int seat = 0; seat < players; seat++) session.sit(seat, 100L + seat, "sit-" + seat);
        for (int seat = 0; seat < players; seat++) session.preset(seat, 1, 3, "preset-" + seat);
        assertTurn(session, "BETTING", 0);
        return session;
    }

    private static void assertTurn(Cd299Session session, String phase, int seat) {
        Map<String, Object> state = state(session);
        assertEquals(phase, state.get("phase"));
        assertEquals(seat, state.get("currentSeat"));
    }

    private static Map<String, Object> state(Cd299Session session) {
        return session.authoritativeState();
    }
}
