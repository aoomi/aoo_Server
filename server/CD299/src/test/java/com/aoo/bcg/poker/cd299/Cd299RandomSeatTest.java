package com.aoo.bcg.poker.cd299;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Cd299RandomSeatTest {
    private static final Cd299Rules RULES = Cd299Rules.from(Map.of("startPlayers", 2, "roundCount", 10));

    @Test
    void submittedVisualSeatCannotChooseTheAuthoritativeSeat() {
        Cd299Session left = session(9100L, 99L, 12345L);
        Cd299Session right = session(9100L, 99L, 12345L);
        int leftSeat = ((Number) sit(left, 101L, 0L, 0, "reserve").get("viewerSeat")).intValue();
        int rightSeat = ((Number) sit(right, 101L, 0L, 7, "reserve").get("viewerSeat")).intValue();
        assertTrue(leftSeat >= 0 && leftSeat < 8);
        assertEquals(leftSeat, rightSeat);
        assertEquals(1L, left.authoritativeState().get("seatRandomSequence"));
        assertEquals(left.authoritativeState(), right.authoritativeState());
        assertFalse(left.viewFor(101L).containsKey("seatRandomSeed"));
        assertFalse(left.viewFor(101L).containsKey("seatRandomSequence"));
    }

    @Test
    void zeroCarryReservationAndPositiveConfirmationKeepTheSameSeatAndDrawCount() {
        Cd299Session session = session(9101L, 88L, 177L);
        Map<String, Object> reserved = sit(session, 101L, 0L, 6, "reserve");
        int seat = ((Number) reserved.get("viewerSeat")).intValue();
        assertEquals(Map.of(seat, 101L), session.authoritativeState().get("players"));
        assertTrue(((Map<?, ?>) session.authoritativeState().get("seatRetentionDeadlineEpochMillis"))
                .containsKey(seat));
        assertEquals(1L, session.authoritativeState().get("seatRandomSequence"));

        Map<String, Object> confirmed = sit(session, 101L, 500L, 1, "confirm");
        assertEquals(seat, confirmed.get("viewerSeat"));
        assertEquals(500L, ((Map<?, ?>) confirmed.get("carryScores")).get(seat));
        assertEquals(Map.of(), confirmed.get("seatRetentionDeadlineEpochMillis"));
        assertEquals(1L, session.authoritativeState().get("seatRandomSequence"));
        Map<String, Object> beforeDuplicate = session.authoritativeState();
        assertThrows(IllegalStateException.class, () -> sit(session, 101L, 500L, 7, "confirm"));
        assertEquals(beforeDuplicate, session.authoritativeState());
        assertEquals(beforeDuplicate, Cd299Session.restore(beforeDuplicate).authoritativeState());
    }

    @Test
    void legacySnapshotUpgradeIsDeterministicAndContinuesFromOccupiedCount() {
        Cd299Session original = session(9102L, 44L, 355L);
        sit(original, 101L, 0L, 0, "reserve");
        Map<String, Object> legacy = new LinkedHashMap<>(original.authoritativeState());
        legacy.put("schemaVersion", 8);
        legacy.remove("seatRandomSeed");
        legacy.remove("seatRandomSequence");

        Cd299Session first = Cd299Session.restore(legacy);
        Cd299Session second = Cd299Session.restore(legacy);
        assertEquals(first.authoritativeState(), second.authoritativeState());
        assertEquals(10, first.authoritativeState().get("schemaVersion"));
        assertEquals(1L, first.authoritativeState().get("seatRandomSequence"));
        first.sit(0, 102L, 0L, "next");
        second.sit(7, 102L, 0L, "next");
        assertEquals(first.authoritativeState(), second.authoritativeState());
        assertNotEquals(first.viewFor(101L).get("viewerSeat"), first.viewFor(102L).get("viewerSeat"));
    }

    @Test
    void eightReservationsExcludeOccupiedSeatsAndRejectTheNinthWithoutDrawing() {
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 6, "roundCount", 10));
        Cd299Session session = new Cd299Session(9103L, 101L, 77L, rules,
                AuthoritativeTimeSource.systemUtc(), 98765L);
        for (long player = 101L; player < 109L; player++) {
            session.sit(0, player, 0L, "reserve-" + player);
        }
        Map<String, Object> full = session.authoritativeState();
        assertEquals(8, ((Map<?, ?>) full.get("players")).size());
        assertEquals(8L, full.get("seatRandomSequence"));
        assertEquals("WAITING", full.get("phase"));
        assertThrows(IllegalStateException.class, () -> session.sit(0, 109L, 0L, "full"));
        assertEquals(full, session.authoritativeState());
        assertTrue(session.invariantViolations().isEmpty());
    }

    @Test
    void seatRandomStreamDoesNotConsumeTheDeckShuffleStream() {
        Cd299Session left = session(9104L, 71L, 1L);
        Cd299Session right = session(9104L, 71L, 9999L);
        Cd299Session differentDeck = session(9104L, 72L, 1L);
        left.sit(0, 101L, 100L, "first");
        left.sit(0, 102L, 100L, "second");
        right.sit(7, 101L, 100L, "first");
        right.sit(7, 102L, 100L, "second");
        differentDeck.sit(7, 101L, 100L, "first");
        differentDeck.sit(7, 102L, 100L, "second");
        assertEquals("BETTING", left.authoritativeState().get("phase"));
        assertEquals("BETTING", right.authoritativeState().get("phase"));
        assertEquals(left.authoritativeState().get("deck"), right.authoritativeState().get("deck"));
        assertEquals(left.authoritativeState().get("seatJoinOrder"),
                differentDeck.authoritativeState().get("seatJoinOrder"));
        assertNotEquals(left.authoritativeState().get("deck"),
                differentDeck.authoritativeState().get("deck"));
    }

    @Test
    void shuffledPlayerMapRestoresJoinOrderAndKeepsNextRoundHandsBySeat() {
        Cd299Rules rules = Cd299Rules.from(Map.of("startPlayers", 4, "roundCount", 10));
        Cd299Session original = new Cd299Session(9105L, 101L, 73L, rules,
                new AuthoritativeTimeSource(Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"),
                        ZoneOffset.UTC)), 13579L);
        for (long player = 101L; player < 105L; player++) {
            original.sit(0, player, 100L, "sit-" + player);
        }
        Map<String, Object> live = original.authoritativeState();
        @SuppressWarnings("unchecked")
        List<Integer> joinOrder = (List<Integer>) live.get("seatJoinOrder");
        assertEquals(4, joinOrder.size());
        List<Integer> turnOrder = new ArrayList<>(joinOrder);
        turnOrder.sort(Integer::compareTo);
        for (int seat : turnOrder) {
            original.bet(seat, Cd299Session.BetAction.REST, 0, "rest-" + seat);
        }
        Map<String, Object> settled = original.authoritativeState();
        assertEquals("ROUND_SETTLEMENT", settled.get("phase"));
        Map<String, Object> shuffled = new LinkedHashMap<>(settled);
        Map<?, ?> originalPlayers = (Map<?, ?>) settled.get("players");
        LinkedHashMap<Integer, Long> reversePlayers = new LinkedHashMap<>();
        for (int index = joinOrder.size() - 1; index >= 0; index--) {
            int seat = joinOrder.get(index);
            reversePlayers.put(seat, ((Number) originalPlayers.get(seat)).longValue());
        }
        shuffled.put("players", reversePlayers);
        Cd299Session recovered = Cd299Session.restore(shuffled);
        assertEquals(joinOrder, recovered.authoritativeState().get("seatJoinOrder"));

        int trigger = ((Number) settled.get("roundSettlementTriggerSeat")).intValue();
        long player = ((Number) originalPlayers.get(trigger)).longValue();
        GameCommandRequest next = new GameCommandRequest("poker.cd299.continue_req", "next-round",
                original.stateVersion() + 1, 9105L, 0, Cd299Rules.VERSION,
                String.valueOf(player), trigger, Map.of());
        original.execute(next);
        recovered.execute(next);
        Map<?, ?> originalHands = (Map<?, ?>) original.authoritativeState().get("hands");
        Map<?, ?> recoveredHands = (Map<?, ?>) recovered.authoritativeState().get("hands");
        for (int seat : joinOrder) {
            assertEquals(originalHands.get(seat), recoveredHands.get(seat), "seat=" + seat);
        }
        assertEquals(original.authoritativeState().get("deck"), recovered.authoritativeState().get("deck"));
    }

    @Test
    void invalidJoinOrderIsRejectedInsteadOfFallingBackToMapIteration() {
        Cd299Session session = session(9106L, 70L, 55L);
        session.sit(0, 101L, 0L, "first");
        session.sit(0, 102L, 0L, "second");
        Map<String, Object> malformed = new LinkedHashMap<>(session.authoritativeState());
        @SuppressWarnings("unchecked")
        List<Integer> order = (List<Integer>) malformed.get("seatJoinOrder");
        malformed.put("seatJoinOrder", List.of(order.getFirst(), order.getFirst()));
        assertThrows(IllegalArgumentException.class, () -> Cd299Session.restore(malformed));
    }

    @Test
    void legacyMultiPlayerSnapshotWithoutJoinOrderIsNotGuessed() {
        Cd299Session session = session(9107L, 70L, 55L);
        session.sit(0, 101L, 0L, "first");
        session.sit(0, 102L, 0L, "second");
        Map<String, Object> legacy = new LinkedHashMap<>(session.authoritativeState());
        legacy.put("schemaVersion", 9);
        legacy.remove("seatJoinOrder");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Cd299Session.restore(legacy));
        assertTrue(error.getMessage().contains("legacy multi-player snapshot missing seatJoinOrder"));
    }

    private static Cd299Session session(long roomId, long deckSeed, long seatSeed) {
        return new Cd299Session(roomId, 101L, deckSeed, RULES,
                new AuthoritativeTimeSource(Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"),
                        ZoneOffset.UTC)), seatSeed);
    }

    private static Map<String, Object> sit(Cd299Session session, long playerId, long carryScore,
            int clickedSeat, String operationId) {
        return session.execute(new GameCommandRequest("poker.cd299.sit_req", operationId,
                session.stateVersion() + 1, ((Number) session.authoritativeState().get("roomId")).longValue(),
                0, Cd299Rules.VERSION, String.valueOf(playerId), -1,
                Map.of("seatId", clickedSeat, "carryScore", carryScore))).body().asMap();
    }
}
