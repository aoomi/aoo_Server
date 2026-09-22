package com.aoo.bcg.poker.cd299;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Cd299SessionTurnTest {
    @Test
    void completeRoundDealsTwoThenThirdThenFourthBeforeSplitAndSettlement() {
        Cd299Session session = bettingSession(2, 48L);
        assertHandSizes(session, 2, 2);

        followAll(session, "first");
        assertTurn(session, "BETTING", 0);
        assertHandSizes(session, 3, 3);

        followAll(session, "second");
        assertTurn(session, "SPLITTING", 0);
        assertHandSizes(session, 4, 4);

        session.split(0, legalSplit(hand(session, 0)), "split-0");
        assertTurn(session, "SPLITTING", 1);
        session.split(1, legalSplit(hand(session, 1)), "split-1");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
        assertEquals(-1, state(session).get("currentSeat"));
        assertEquals(2, ((Map<?, ?>) state(session).get("lastDelta")).size());
    }

    @Test
    void middleSeatDropDoesNotEndBettingBeforeRemainingPlayersAct() {
        Cd299Session session = bettingSession(4, 40L);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "bet-0");
        session.bet(1, Cd299Session.BetAction.DROP, 0, "drop-1");
        assertTurn(session, "BETTING", 2);

        session.bet(2, Cd299Session.BetAction.FOLLOW, 0, "bet-2");
        assertTurn(session, "BETTING", 3);
        session.bet(3, Cd299Session.BetAction.FOLLOW, 0, "bet-3");
        assertTurn(session, "BETTING", 0);
        assertHandSizes(session, 3, 2, 3, 3);
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
        assertTurn(session, "BETTING", 0);
        assertHandSizes(session, 3, 2, 2, 3, 3, 3);
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
            @SuppressWarnings("unchecked")
            java.util.List<Integer> flowers = (java.util.List<Integer>) state(session).get("threeFlowerSeats");
            if (flowers.contains(0)) {
                assertEquals(3, hand(session, 1).size());
                verified = true;
            }
        }
        assertTrue(verified, "test seeds must include a seat-0 three-flower deal");
    }

    @Test
    void raiseReopensBettingForPlayersWhoAlreadyActed() {
        Cd299Session session = bettingSession(4, 45L);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "follow-0");
        session.bet(1, Cd299Session.BetAction.FOLLOW, 0, "follow-1");
        session.bet(2, Cd299Session.BetAction.RAISE, 6, "raise-2");
        assertTurn(session, "BETTING", 3);
        session.bet(3, Cd299Session.BetAction.FOLLOW, 0, "follow-3");
        assertTurn(session, "BETTING", 0);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "refollow-0");
        assertTurn(session, "BETTING", 1);
    }

    @Test
    void restMustFollowRestWithinSameBettingRound() {
        Cd299Session session = bettingSession(4, 46L);
        session.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        session.bet(1, Cd299Session.BetAction.FOLLOW, 0, "follow-1");
        assertThrows(IllegalArgumentException.class,
                () -> session.bet(2, Cd299Session.BetAction.REST, 0, "rest-2-bad-order"));
    }

    @Test
    void allRestEndsRoundAndCarriesMangoWithoutComparingTwoCardHands() {
        Cd299Session session = bettingSession(2, 47L);
        session.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        session.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");
        Map<String, Object> state = state(session);
        assertEquals("ROUND_SETTLEMENT", state.get("phase"));
        assertEquals(6L, state.get("mangoPool"));
    }

    @Test
    void restMangoRaisesNextRoundAndLateJoinerCatchesUpLikeXqp() {
        Cd299Session session = bettingSessionWithCarry(2, 147L, 100L);
        session.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        session.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");
        assertEquals(1, state(session).get("upMangoLevel"));
        assertEquals("REST", state(session).get("lastMangoEvent"));

        session.sit(2, 102L, 100L, "late-sit-2");
        execute(session, "poker.cd299.continue_req", "continue", 0, 100L, Map.of());
        @SuppressWarnings("unchecked") Map<Integer, Integer> required =
                (Map<Integer, Integer>) state(session).get("requiredMangos");
        assertEquals(6, required.get(0));
        assertEquals(6, required.get(1));
        assertEquals(6, required.get(2));
        assertEquals(9, ((Map<?, ?>) state(session).get("mangos")).get(2));
        assertEquals(6, ((Map<?, ?>) state(session).get("mangos")).get(0));
    }

    @Test
    void firstRoundRaiseFollowedByAllOtherDropsTriggersBeatMango() {
        Cd299Session session = bettingSessionWithCarry(4, 148L, 100L);
        session.bet(0, Cd299Session.BetAction.RAISE, 6, "raise-0");
        session.bet(1, Cd299Session.BetAction.DROP, 0, "drop-1");
        session.bet(2, Cd299Session.BetAction.DROP, 0, "drop-2");
        session.bet(3, Cd299Session.BetAction.DROP, 0, "drop-3");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
        assertEquals("BEAT", state(session).get("lastMangoEvent"));
        assertEquals(1, state(session).get("upMangoLevel"));
    }

    @Test
    void insufficientScoreCannotFollowOrRaiseAndMustUseAllIn() {
        Cd299Session session = bettingSessionWithCarry(2, 149L, 5L);
        @SuppressWarnings("unchecked") List<String> allowed =
                (List<String>) session.viewFor(100L).get("allowedBetActions");
        assertFalse(allowed.contains("FOLLOW"));
        assertFalse(allowed.contains("RAISE"));
        assertTrue(allowed.contains("ALL_IN"));
        assertThrows(IllegalArgumentException.class,
                () -> session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "cannot-follow"));
        session.bet(0, Cd299Session.BetAction.ALL_IN, 0, "short-all-in");
        assertEquals(1, ((Map<?, ?>) state(session).get("bets")).get(0));
    }

    @Test
    void quickRaiseTargetsMatchXqpDynamicMultipliers() {
        Cd299Session session = bettingSessionWithCarry(2, 249L, 100L);
        Map<String, Object> view = session.viewFor(100L);
        assertEquals(3, view.get("followAmount"));
        assertEquals(3, view.get("minimumRaiseTarget"));
        // N=max(openingBet=3, two players' mango total=6); when 休 is legal XQP shows N/2-adjusted 1x/2x/4x.
        assertEquals(List.of(6, 12, 24), view.get("quickRaiseTargets"));
        assertEquals(96L, view.get("viewerAvailableScore"));

        session.bet(0, Cd299Session.BetAction.RAISE, 6, "quick-big-1");
        Map<String, Object> follower = session.viewFor(101L);
        assertEquals(6, follower.get("followAmount"));
        assertEquals(List.of(12, 24, 48), follower.get("quickRaiseTargets"));
    }

    @Test
    void splitIsConcurrentAndEachPlayerOwnsAnIndependentDeadline() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2));
        Cd299Session session = new Cd299Session(9299L, 100L, 299L, rules,
                new AuthoritativeTimeSource(clock));
        session.sit(0, 100L, 100L, "sit-0"); session.sit(1, 101L, 100L, "sit-1");
        
        followAll(session, "round-1"); followAll(session, "round-2");
        assertEquals("SPLITTING", state(session).get("phase"));
        @SuppressWarnings("unchecked") Map<Integer, Long> deadlines =
                (Map<Integer, Long>) state(session).get("splitDeadlineEpochMillis");
        assertEquals(Set.of(0, 1), deadlines.keySet());

        // Seat 1 may finish before seat 0; splitting is not a turn queue.
        session.split(1, legalSplit(hand(session, 1)), "split-1-first");
        assertEquals("SPLITTING", state(session).get("phase"));
        assertEquals(List.of(1), state(session).get("splitSeats"));
        session.delaySplit(0, "delay-seat-0");
        @SuppressWarnings("unchecked") Map<Integer, Long> extended =
                (Map<Integer, Long>) state(session).get("splitDeadlineEpochMillis");
        assertEquals(deadlines.get(0) + 30_000L, extended.get(0));
        session.split(0, legalSplit(hand(session, 0)), "split-0-last");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
    }

    @Test
    void xqpBaseAndMangoAreServerAssignedBeforeFirstBet() {
        Cd299Session session = bettingSession(2, 301L);
        Map<String, Object> state = state(session);
        assertEquals(Map.of(0, 1, 1, 1), state.get("bases"));
        assertEquals(Map.of(0, 3, 1, 3), state.get("mangos"));
        assertEquals(Map.of(0, 4, 1, 4), state.get("committed"));
        assertEquals(2, ((List<?>) ((Map<?, ?>) state.get("hands")).get(0)).size());
    }

    @Test
    void xqpPresetDropExecutesOnlyWhenThatPlayersTurnArrives() {
        Cd299Session session = bettingSession(4, 302L);
        session.presetBet(2, 2, "pre-drop-2");
        assertTurn(session, "BETTING", 0);
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, "follow-0");
        session.bet(1, Cd299Session.BetAction.FOLLOW, 0, "follow-1");
        assertTurn(session, "BETTING", 3);
        assertEquals("DROP", ((Map<?, ?>) state(session).get("lastBetActions")).get(2));
    }

    @Test
    void opponentsSeeDealtThirdCardButNotTheTwoFireproofCards() {
        Cd299Session session = bettingSession(2, 303L);
        followAll(session, "first-round");
        @SuppressWarnings("unchecked") List<Integer> opponent =
                (List<Integer>) ((Map<?, ?>) session.viewFor(100L).get("hands")).get(1);
        assertEquals(3, opponent.size());
        assertEquals(List.of(0, 0), opponent.subList(0, 2));
        assertTrue(opponent.get(2) != 0);
    }

    @Test
    void nextRoundFinishesWhenOnlyOnePlayerCanAffordBaseAndMango() {
        Cd299Session settled = bettingSessionWithCarry(2, 150L, 100L);
        settled.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        settled.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");
        Map<String, Object> saved = new java.util.LinkedHashMap<>(settled.authoritativeState());
        saved.put("scores", Map.of(0, 100L, 1, 4L));
        Cd299Session restored = Cd299Session.restore(saved);
        execute(restored, "poker.cd299.continue_req", "continue-last-funded", 0, 100L, Map.of());
        assertEquals("FINISHED", state(restored).get("phase"));
        assertEquals(List.of(0), state(restored).get("roundSeats"));
    }

    @Test
    void seatedPlayerCanAdvanceSettlementWhenRoomOwnerIsOnlySpectating() {
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2));
        Cd299Session session = new Cd299Session(92990L, 999L, 299L, rules);
        session.sit(0, 100L, 100L, "sit-0");
        session.sit(1, 101L, 100L, "sit-1");
        session.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        session.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));

        execute(session, "poker.cd299.continue_req", "continue-by-player", 1, 101L, Map.of());

        assertEquals("BETTING", state(session).get("phase"));
        assertEquals(2, state(session).get("round"));
    }

    @Test
    void spectatorCannotAdvanceSettlement() {
        Cd299Session session = bettingSession(2, 151L);
        session.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        session.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");

        assertThrows(IllegalArgumentException.class,
                () -> execute(session, "poker.cd299.continue_req", "continue-by-spectator", 0, 999L, Map.of()));
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
    }

    @Test
    void fivePlayerDropRaiseAndAllInAutoDealsFourthCardThenSplits() {
        Cd299Session session = bettingSessionWithCarry(6, 49L, 100L);
        for (int seat = 0; seat < 6; seat++) session.bet(seat, Cd299Session.BetAction.FOLLOW, 0, "first-follow-" + seat);
        assertHandSizes(session, 3, 3, 3, 3, 3, 3);

        session.bet(0, Cd299Session.BetAction.DROP, 0, "drop-0");
        session.bet(1, Cd299Session.BetAction.RAISE, 6, "raise-1");
        session.bet(2, Cd299Session.BetAction.DROP, 0, "drop-2");
        session.bet(3, Cd299Session.BetAction.ALL_IN, 0, "all-in-3");
        session.bet(4, Cd299Session.BetAction.ALL_IN, 0, "all-in-4");
        session.bet(5, Cd299Session.BetAction.DROP, 0, "drop-5");
        session.bet(1, Cd299Session.BetAction.ALL_IN, 0, "all-in-1");

        assertTurn(session, "SPLITTING", 1);
        assertHandSizes(session, 3, 4, 3, 4, 4);
        assertEquals(Set.of(1, 3, 4), Set.copyOf((List<Integer>) state(session).get("allInSeats")));
        assertEquals(96, ((Map<?, ?>) state(session).get("bets")).get(1));
        assertEquals(96, ((Map<?, ?>) state(session).get("bets")).get(3));
        assertEquals(96, ((Map<?, ?>) state(session).get("bets")).get(4));
    }

    @Test
    void authenticatedPlayerCommandCannotWinAfterAuthoritativeDeadline() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-19T00:00:00Z"));
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2, "operationSeconds", 15));
        Cd299Session session = new Cd299Session(9099L, 100L, 99L, rules,
                new AuthoritativeTimeSource(clock));
        session.sit(0, 100L, "sit-0");
        session.sit(1, 101L, "sit-1");
        long versionBeforeLateCommand = session.stateVersion();
        clock.advance(Duration.ofSeconds(15));

        GameCommandRequest late = new GameCommandRequest("poker.cd299.bet_req", "late-bet", 1,
                9099L, 0, Cd299Rules.VERSION, "100", 0, Map.of("action", "FOLLOW", "amount", 0));
        assertThrows(IllegalStateException.class, () -> session.execute(late));
        assertEquals("BETTING", state(session).get("phase"));
        assertEquals(versionBeforeLateCommand, state(session).get("stateVersion"));
    }

    private static Cd299Session bettingSession(int players, long seed) {
        return bettingSessionWithCarry(players, seed, 100L);
    }

    private static Cd299Session bettingSessionWithCarry(int players, long seed, long carryScore) {
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", players));
        Cd299Session session = new Cd299Session(9000L + seed, 100L, seed, rules);
        for (int seat = 0; seat < players; seat++) session.sit(seat, 100L + seat, carryScore, "sit-" + seat);
        assertTurn(session, "BETTING", 0);
        return session;
    }

    private static void assertTurn(Cd299Session session, String phase, int seat) {
        Map<String, Object> state = state(session);
        assertEquals(phase, state.get("phase"));
        assertEquals(seat, state.get("currentSeat"));
    }

    private static void followAll(Cd299Session session, String stage) {
        session.bet(0, Cd299Session.BetAction.FOLLOW, 0, stage + "-follow-0");
        session.bet(1, Cd299Session.BetAction.FOLLOW, 0, stage + "-follow-1");
    }

    private static void assertHandSizes(Cd299Session session, int... expected) {
        for (int seat = 0; seat < expected.length; seat++) {
            assertEquals(expected[seat], hand(session, seat).size(), "seat=" + seat);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> hand(Cd299Session session, int seat) {
        return (List<Integer>) ((Map<Integer, List<Integer>>) state(session).get("hands")).get(seat);
    }

    private static List<Integer> legalSplit(List<Integer> cards) {
        Cd299HandEvaluator evaluator = new Cd299HandEvaluator();
        for (int a = 0; a < cards.size(); a++) for (int b = a + 1; b < cards.size(); b++) {
            List<Integer> ordered = new java.util.ArrayList<>();
            ordered.add(cards.get(a)); ordered.add(cards.get(b));
            for (int index = 0; index < cards.size(); index++) if (index != a && index != b) ordered.add(cards.get(index));
            try { evaluator.split(ordered, true); return List.copyOf(ordered); }
            catch (IllegalArgumentException ignored) { /* Try the next two-card head. */ }
        }
        throw new AssertionError("no legal split for " + cards);
    }

    private static Map<String, Object> state(Cd299Session session) {
        return session.authoritativeState();
    }

    private static void execute(Cd299Session session, String action, String requestId,
            int seat, long playerId, Map<String, Object> body) {
        session.execute(new GameCommandRequest(action, requestId, session.stateVersion() + 1,
                ((Number) state(session).get("roomId")).longValue(), 0, Cd299Rules.VERSION,
                String.valueOf(playerId), seat, body));
    }

    private static final class MutableClock extends Clock {
        private Instant current;
        private MutableClock(Instant current) { this.current = current; }
        private void advance(Duration duration) { current = current.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }
}
