package com.aoo.bcg.poker.cd299;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.aoo.bcg.common.room.AuthoritativeRandomSeatAllocator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Cd299SessionTurnTest {
    // Existing turn tests use physical seats to describe ordering. Choose a fixed
    // authority RNG stream that allocates 0..5; the submitted seat values do not select them.
    private static final long SEQUENTIAL_SEAT_SEED = sequentialSeatSeed();

    private static long sequentialSeatSeed() {
        var allocator = new AuthoritativeRandomSeatAllocator();
        for (long seed = 0; seed < 1_000_000L; seed++) {
            Set<Integer> occupied = new java.util.HashSet<>();
            boolean sequential = true;
            for (int seat = 0; seat < 6; seat++) {
                int assigned = allocator.allocate(8, Set.copyOf(occupied),
                        new SeededGameRandomSource(Cd299Session.seatDrawSeed(seed, seat)));
                if (assigned != seat) { sequential = false; break; }
                occupied.add(assigned);
            }
            if (sequential) return seed;
        }
        throw new AssertionError("no sequential CD299 seat fixture seed");
    }
    @Test
    void completeRoundDealsTwoThenThirdThenFourthBeforeSplitAndSettlement() {
        Cd299Session session = bettingSession(2, 48L);
        assertHandSizes(session, 2, 2);

        followAll(session, "first");
        assertTurn(session, "BETTING", 0);
        assertHandSizes(session, 3, 3);

        followAll(session, "second");
        assertTurn(session, "BETTING", 0);
        assertHandSizes(session, 4, 4);

        followAll(session, "third");
        assertTurn(session, "SPLITTING", 0);
        assertHandSizes(session, 4, 4);

        session.split(0, legalSplit(hand(session, 0)), "split-0");
        assertTurn(session, "SPLITTING", 1);
        session.split(1, legalSplit(hand(session, 1)), "split-1");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
        assertEquals(-1, state(session).get("currentSeat"));
        assertEquals(2, ((Map<?, ?>) state(session).get("lastDelta")).size());
        Map<?, ?> wins = (Map<?, ?>) state(session).get("winCounts");
        Map<?, ?> losses = (Map<?, ?>) state(session).get("loseCounts");
        assertEquals(1L, wins.values().stream().mapToInt(value -> ((Number) value).intValue()).sum());
        assertEquals(1L, losses.values().stream().mapToInt(value -> ((Number) value).intValue()).sum());
        assertEquals(state(session), Cd299Session.restore(state(session)).authoritativeState());
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
    void firstBankerIsAuthoritativeAndFollowingRoundRotatesCounterclockwise() {
        Cd299Session session = bettingSession(4, 47L);
        int first = ((Number) state(session).get("bankerSeat")).intValue();
        assertTrue(first >= 0 && first <= 3);
        for (int seat = 0; seat < 4; seat++) {
            session.bet(seat, Cd299Session.BetAction.REST, 0, "banker-rest-" + seat);
        }
        execute(session, "poker.cd299.continue_req", "banker-continue", 0, 100L, Map.of());
        assertEquals(first == 0 ? 3 : first - 1, state(session).get("bankerSeat"));
        assertEquals(10, state(session).get("schemaVersion"));
        assertEquals(state(session), Cd299Session.restore(state(session)).authoritativeState());
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
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2, "roundCount", 10));
        Cd299Session session = new Cd299Session(9299L, 100L, 299L, rules,
                new AuthoritativeTimeSource(clock), SEQUENTIAL_SEAT_SEED);
        session.sit(0, 100L, 100L, "sit-0"); session.sit(1, 101L, 100L, "sit-1");

        followAll(session, "round-1"); followAll(session, "round-2"); followAll(session, "round-3");
        assertEquals("SPLITTING", state(session).get("phase"));
        @SuppressWarnings("unchecked") Map<Integer, Long> deadlines =
                (Map<Integer, Long>) state(session).get("splitDeadlineEpochMillis");
        assertEquals(Set.of(0, 1), deadlines.keySet());

        // Seat 1 may finish before seat 0; splitting is not a turn queue.
        session.split(1, legalSplit(hand(session, 1)), "split-1-first");
        assertEquals("SPLITTING", state(session).get("phase"));
        assertEquals(List.of(1), state(session).get("splitSeats"));
        @SuppressWarnings("unchecked") List<Integer> hiddenOpponentSplit =
                (List<Integer>) ((Map<?, ?>) session.viewFor(100L).get("hands")).get(1);
        assertEquals(List.of(0, 0, 0, 0), hiddenOpponentSplit);
        assertEquals(hand(session, 1), ((Map<?, ?>) session.viewFor(101L).get("hands")).get(1));
        session.delaySplit(0, "delay-seat-0");
        @SuppressWarnings("unchecked") Map<Integer, Long> extended =
                (Map<Integer, Long>) state(session).get("splitDeadlineEpochMillis");
        assertEquals(deadlines.get(0) + 30_000L, extended.get(0));
        session.split(0, legalSplit(hand(session, 0)), "split-0-last");
        assertEquals("ROUND_SETTLEMENT", state(session).get("phase"));
        assertEquals(hand(session, 1), ((Map<?, ?>) session.viewFor(100L).get("hands")).get(1));
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
    void opponentsSeeOnlyCardBacksUntilTheRoundReveal() {
        Cd299Session session = bettingSession(2, 303L);
        followAll(session, "first-round");
        @SuppressWarnings("unchecked") List<Integer> opponent =
                (List<Integer>) ((Map<?, ?>) session.viewFor(100L).get("hands")).get(1);
        assertEquals(3, opponent.size());
        assertEquals(List.of(0, 0, 0), opponent);
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
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2, "roundCount", 10));
        Cd299Session session = new Cd299Session(92990L, 999L, 299L, rules,
                AuthoritativeTimeSource.systemUtc(), SEQUENTIAL_SEAT_SEED);
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
    void terminalPlayersChooseNewCarryBeforeSameRoomSeriesRestarts() {
        Cd299Session session = terminalSession(Long.MAX_VALUE);
        execute(session, "poker.cd299.restart_req", "restart-0", 0, 100L, Map.of("carryScore", 200L));
        assertEquals("FINISHED", state(session).get("phase"));
        assertEquals(Map.of(0, 200L), state(session).get("restartCarryScores"));

        execute(session, "poker.cd299.restart_req", "restart-1", 1, 101L, Map.of("carryScore", 300L));
        assertEquals("BETTING", state(session).get("phase"));
        assertEquals(1, state(session).get("round"));
        assertEquals(Map.of(0, 200L, 1, 300L), state(session).get("carryScores"));
        assertEquals(Map.of(0, 200L, 1, 300L), state(session).get("scores"));
        assertEquals(0L, state(session).get("terminalDeadlineEpochMillis"));
    }

    @Test
    void finalRoundOpensRetentionWithoutDoubleAdvancingSettlementVersion() {
        Instant now = Instant.parse("2026-09-23T12:00:00Z");
        MutableClock clock = new MutableClock(now);
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2, "roundCount", 10));
        Cd299Session session = new Cd299Session(92991L, 100L, 299L, rules,
                new AuthoritativeTimeSource(clock), SEQUENTIAL_SEAT_SEED);
        session.sit(0, 100L, 600L, "sit-0");
        session.sit(1, 101L, 600L, "sit-1");
        for (int completed = 1; completed < 10; completed++) {
            session.bet(0, Cd299Session.BetAction.DROP, 0, "pre-final-drop-" + completed);
            execute(session, "poker.cd299.continue_req", "pre-final-continue-" + completed,
                    0, 100L, Map.of());
        }
        long beforeFinalAction = session.stateVersion();

        session.bet(0, Cd299Session.BetAction.DROP, 0, "final-drop");

        Map<String, Object> terminal = state(session);
        assertEquals("FINISHED", terminal.get("phase"));
        assertEquals(now.plusSeconds(120).toEpochMilli(), terminal.get("terminalDeadlineEpochMillis"));
        // One version for the player's bet and one for the settlement; entering
        // terminal retention must not add a second settlement transition.
        assertEquals(beforeFinalAction + 2, session.stateVersion());
        execute(session, "poker.cd299.restart_req", "restart-before-expiry", 0, 100L,
                Map.of("carryScore", 200L));
        assertEquals(Map.of(0, 200L), state(session).get("restartCarryScores"));
    }

    @Test
    void terminalStandRemovesOnlyAuthenticatedSeatAndExpiredRetentionRejectsRestart() {
        Cd299Session session = terminalSession(Long.MAX_VALUE);
        execute(session, "poker.cd299.stand_req", "stand-1", 1, 101L, Map.of());
        assertEquals(Map.of(0, 100L), state(session).get("players"));
        assertEquals(Map.of(0, 100L), state(session).get("carryScores"));
        assertEquals(Map.of(0, 100L), state(session).get("scores"));
        assertEquals(Map.of(0, ((Map<?, ?>) state(session).get("hands")).get(0)), state(session).get("hands"));
        assertEquals("SPECTATOR", session.viewFor(101L).get("viewerRole"));

        Cd299Session expired = terminalSession(1L);
        assertThrows(IllegalStateException.class, () -> execute(expired,
                "poker.cd299.restart_req", "expired", 0, 100L, Map.of("carryScore", 100L)));
    }

    @Test
    void settlementDeadlineElectsOnePlayerAndAutomaticallyStartsNextRound() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-23T13:00:00Z"));
        Cd299Session session = new Cd299Session(92992L, 100L, 300L,
                Cd299Rules.from(Map.of("startPlayers", 2, "roundCount", 10)),
                new AuthoritativeTimeSource(clock), SEQUENTIAL_SEAT_SEED);
        session.sit(0, 100L, 600L, "sit-0");
        session.sit(1, 101L, 600L, "sit-1");
        session.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        session.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");

        Map<String, Object> settlement = state(session);
        assertEquals("ROUND_SETTLEMENT", settlement.get("phase"));
        assertEquals(0, settlement.get("roundSettlementTriggerSeat"));
        assertEquals(clock.instant().plusSeconds(2).toEpochMilli(),
                settlement.get("roundSettlementDeadlineEpochMillis"));

        clock.advance(Duration.ofSeconds(2));
        execute(session, "poker.cd299.timeout_req", "auto-next", 0, 100L, Map.of());
        assertEquals("BETTING", state(session).get("phase"));
        assertEquals(2, state(session).get("round"));
        assertEquals(0L, state(session).get("roundSettlementDeadlineEpochMillis"));
        assertEquals(-1, state(session).get("roundSettlementTriggerSeat"));
        assertThrows(IllegalStateException.class,
                () -> execute(session, "poker.cd299.timeout_req", "duplicate-auto-next", 0, 100L, Map.of()));
    }

    @Test
    void depletedSeatCanRebuyDuringItsOwnRetentionWindow() {
        Cd299Session live = bettingSession(2, 301L);
        live.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        live.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");
        Map<String, Object> saved = new java.util.LinkedHashMap<>(live.authoritativeState());
        saved.put("scores", Map.of(0, 100L, 1, 0L));
        saved.put("seatRetentionDeadlineEpochMillis", Map.of(1, Long.MAX_VALUE));
        saved.put("roundSettlementDeadlineEpochMillis", 0L);
        saved.put("roundSettlementTriggerSeat", -1);
        Cd299Session restored = Cd299Session.restore(saved);

        execute(restored, "poker.cd299.rebuy_req", "rebuy-1", 1, 101L, Map.of("carryScore", 250L));

        assertEquals(250L, ((Map<?, ?>) state(restored).get("scores")).get(1));
        assertEquals(250L, ((Map<?, ?>) state(restored).get("carryScores")).get(1));
        assertEquals(Map.of(), state(restored).get("seatRetentionDeadlineEpochMillis"));
        assertEquals(0, state(restored).get("roundSettlementTriggerSeat"));
        assertTrue(((Number) state(restored).get("roundSettlementDeadlineEpochMillis")).longValue()
                > System.currentTimeMillis());
    }

    @Test
    void fundedSpectatorSittingDuringSettlementRestoresNextRoundSchedule() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-24T08:00:00Z"));
        Cd299Session live = new Cd299Session(93312L, 100L, 312L,
                Cd299Rules.from(Map.of("startPlayers", 2, "roomDurationMinutes", 30)),
                new AuthoritativeTimeSource(clock), SEQUENTIAL_SEAT_SEED);
        live.sit(0, 100L, 600L, "sit-0");
        live.sit(1, 101L, 600L, "sit-1");
        live.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        live.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");
        execute(live, "poker.cd299.stand_req", "stand-1", 1, 101L, Map.of());

        Map<String, Object> saved = new java.util.LinkedHashMap<>(live.authoritativeState());
        saved.put("roundSettlementDeadlineEpochMillis", 0L);
        saved.put("roundSettlementTriggerSeat", -1);
        Cd299Session restored = Cd299Session.restore(saved);
        long beforeSit = System.currentTimeMillis();
        restored.sit(2, 102L, 600L, "sit-2");

        assertEquals("ROUND_SETTLEMENT", state(restored).get("phase"));
        assertEquals(0, state(restored).get("roundSettlementTriggerSeat"));
        long deadline = ((Number) state(restored).get("roundSettlementDeadlineEpochMillis")).longValue();
        assertTrue(deadline >= beforeSit + 2_000L);
        assertTrue(deadline <= System.currentTimeMillis() + 2_000L);
    }

    @Test
    void seatUnableToFundNextBaseAndMangoWaitsForRebuyInsteadOfFinishingDurationRoom() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-24T07:00:00Z"));
        Cd299Session live = new Cd299Session(93311L, 100L, 311L,
                Cd299Rules.from(Map.of("startPlayers", 2, "roomDurationMinutes", 30)),
                new AuthoritativeTimeSource(clock), SEQUENTIAL_SEAT_SEED);
        live.sit(0, 100L, 8L, "sit-0");
        live.sit(1, 101L, 8L, "sit-1");
        live.bet(0, Cd299Session.BetAction.REST, 0, "rest-0");
        live.bet(1, Cd299Session.BetAction.REST, 0, "rest-1");

        Map<String, Object> state = state(live);
        assertEquals("ROUND_SETTLEMENT", state.get("phase"));
        assertEquals(1, state.get("round"));
        assertEquals(Set.of(0, 1), ((Map<?, ?>) state.get("seatRetentionDeadlineEpochMillis")).keySet());
        assertEquals(0L, state.get("roundSettlementDeadlineEpochMillis"));
        assertThrows(IllegalStateException.class,
                () -> execute(live, "poker.cd299.rebuy_req", "rebuy-too-small", 0, 100L,
                        Map.of("carryScore", 7L)));
        execute(live, "poker.cd299.rebuy_req", "rebuy-0", 0, 100L, Map.of("carryScore", 100L));
        execute(live, "poker.cd299.rebuy_req", "rebuy-1", 1, 101L, Map.of("carryScore", 100L));
        assertEquals(Map.of(), state(live).get("seatRetentionDeadlineEpochMillis"));
        assertEquals(0, state(live).get("roundSettlementTriggerSeat"));
    }

    @Test
    void standIsAllowedBetweenHandsButRejectedDuringAnActiveHand() {
        Cd299Session waiting = new Cd299Session(92993L, 100L, 302L,
                Cd299Rules.from(Map.of("startPlayers", 4, "roundCount", 10)),
                AuthoritativeTimeSource.systemUtc(), SEQUENTIAL_SEAT_SEED);
        waiting.sit(0, 100L, 100L, "sit-0");
        waiting.sit(1, 101L, 100L, "sit-1");
        execute(waiting, "poker.cd299.stand_req", "stand-waiting", 0, 100L, Map.of());
        assertEquals("SPECTATOR", waiting.viewFor(100L).get("viewerRole"));
        assertEquals(Map.of(1, 101L), state(waiting).get("players"));

        Cd299Session active = bettingSession(2, 303L);
        assertThrows(IllegalStateException.class,
                () -> execute(active, "poker.cd299.stand_req", "stand-active", 0, 100L, Map.of()));
        assertEquals("SEATED", active.viewFor(100L).get("viewerRole"));
    }

    @Test
    void zeroCarryReservesSeatFor120SecondsUntilConfirmCancelOrExpiry() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-24T10:00:00Z"));
        Cd299Session session = new Cd299Session(92994L, 100L, 304L,
                Cd299Rules.from(Map.of("startPlayers", 2, "roundCount", 10)), new AuthoritativeTimeSource(clock));

        session.sit(3, 100L, 0L, "reserve-3");
        int reservedSeat = ((Number) session.viewFor(100L).get("viewerSeat")).intValue();
        assertEquals(Map.of(reservedSeat, 100L), state(session).get("players"));
        assertEquals(clock.millis() + 120_000L,
                ((Map<?, ?>) state(session).get("seatRetentionDeadlineEpochMillis")).get(reservedSeat));
        assertEquals("SEATED", session.viewFor(100L).get("viewerRole"));

        session.sit(3, 100L, 100L, "confirm-3");
        assertEquals(Map.of(), state(session).get("seatRetentionDeadlineEpochMillis"));
        assertEquals(reservedSeat, session.viewFor(100L).get("viewerSeat"));
        assertEquals(100L, ((Map<?, ?>) state(session).get("scores")).get(reservedSeat));

        execute(session, "poker.cd299.stand_req", "cancel-confirmed-3", reservedSeat, 100L, Map.of());
        session.sit(5, 100L, 0L, "reserve-5");
        int expiringSeat = ((Number) session.viewFor(100L).get("viewerSeat")).intValue();
        clock.advance(Duration.ofSeconds(120));
        session.timeout(expiringSeat, "expire-5");
        assertEquals(Map.of(), state(session).get("players"));
        assertEquals("SPECTATOR", session.viewFor(100L).get("viewerRole"));
    }

    private static Cd299Session terminalSession(long deadline) {
        Cd299Session live = bettingSession(2, 909L);
        Map<String,Object> saved = new java.util.LinkedHashMap<>(live.authoritativeState());
        saved.put("phase", "FINISHED");
        saved.put("terminalDeadlineEpochMillis", deadline);
        saved.put("operationDeadline", Map.of());
        return Cd299Session.restore(saved);
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
    void authenticatedPlayerCommandAtDeadlineLinearizesAsAuthoritativeTimeout() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-19T00:00:00Z"));
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 2, "operationSeconds", 15, "roundCount", 10));
        Cd299Session session = new Cd299Session(9099L, 100L, 99L, rules,
                new AuthoritativeTimeSource(clock), SEQUENTIAL_SEAT_SEED);
        session.sit(0, 100L, "sit-0");
        session.sit(1, 101L, "sit-1");
        long versionBeforeLateCommand = session.stateVersion();
        clock.advance(Duration.ofSeconds(15));

        GameCommandRequest late = new GameCommandRequest("poker.cd299.bet_req", "late-bet", 1,
                9099L, 0, Cd299Rules.VERSION, "100", 0, Map.of("action", "FOLLOW", "amount", 0));
        session.execute(late);
        assertEquals("BETTING", state(session).get("phase"));
        assertTrue(session.stateVersion() > versionBeforeLateCommand);
        assertEquals(1, state(session).get("currentSeat"));
        assertEquals("REST", ((Map<?, ?>) state(session).get("lastBetActions")).get(0));
    }

    private static Cd299Session bettingSession(int players, long seed) {
        return bettingSessionWithCarry(players, seed, 100L);
    }

    private static Cd299Session bettingSessionWithCarry(int players, long seed, long carryScore) {
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", players, "roundCount", 10));
        Cd299Session session = new Cd299Session(9000L + seed, 100L, seed, rules,
                AuthoritativeTimeSource.systemUtc(), SEQUENTIAL_SEAT_SEED);
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
