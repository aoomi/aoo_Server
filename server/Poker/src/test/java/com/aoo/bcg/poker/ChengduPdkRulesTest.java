package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.SettlementPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ChengduPdkRulesTest {
    @Test void xqpDecksContainExactlyThreeAcesAndOneTwo() {
        assertDeck(ChengduPdkRules.standardDeck(), 48, true);
        assertDeck(ChengduPdkRules.cutDeck(), 40, false);
    }

    @Test void xqpPrefabDefaultsAreTheServerBaseline() {
        PaoDeKuaiConfig config = ChengduPdkRules.defaults();
        assertEquals(PaoDeKuaiConfig.PlayTiming.ANYTIME,
                config.tripleWithoutAttachmentTiming());
        assertEquals(PaoDeKuaiConfig.PlayTiming.ANYTIME,
                config.airplaneWithoutAttachmentTiming());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.EITHER,
                config.tripleAttachmentMode());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.EITHER,
                config.airplaneAttachmentMode());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.SINGLES, config.fourAttachmentMode());
        assertTrue(config.advancedRules().directWinPatterns()
                .contains(java.util.Set.of(103, 203, 303, 403)));
        assertEquals(5, config.advancedRules().bombScore().points());
        assertTrue(config.advancedRules().governance().uniqueIpRequired());
        assertFalse(config.advancedRules().governance().textChatEnabled());
        assertTrue(config.advancedRules().governance().distanceWarningEnabled());
        assertEquals(5, config.advancedRules().hostingMissThreshold());
    }

    @Test void compactRoomSelectionsExpandToImmutableServerRules() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(Map.of(
                "firstPlayRule", "spade_three_first",
                "bombScore", 30,
                "attachmentComparison", "compare",
                "playRule", List.of("three_no_attachment", "triple_ace_bomb",
                        "require_spade_three", "four_threes_direct_win"),
                "roomRestriction", List.of("gps_limit", "interaction_forbidden")),
                ChengduPdkRules.defaults());
        assertEquals(103, config.requiredFirstCard());
        assertEquals(99999, config.advancedRules().requiredFirstCardRounds());
        assertEquals(103, config.advancedRules().bankerSelectionCard());
        assertEquals(java.util.Set.of(14), config.specialTripleBombRanks());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.DISABLED, config.fourAttachmentMode());
        assertTrue(config.compareTripleAttachments());
        assertEquals(30, config.advancedRules().bombScore().points());
        assertTrue(config.advancedRules().governance().gpsAdmissionRequired());
        assertFalse(config.advancedRules().governance().interactionEnabled());
        assertFalse(config.advancedRules().governance().uniqueIpRequired());
    }

    @Test void defaultCutDealAndStandardThreePlayerDealConserveEveryCard() {
        PdkGameProvider provider = new PdkGameProvider();
        PokerAuthoritativeSession cut = started(provider, 901, 2, Map.of());
        assertConserved(cut, ChengduPdkRules.cutDeck(), 8);
        PokerAuthoritativeSession standard = started(provider, 902, 3,
                Map.of("playRule", List.of("three_no_attachment", "four_with_two",
                        "remove_three_four", "four_threes_direct_win")));
        assertConserved(standard, ChengduPdkRules.standardDeck(), 0);
        assertEquals(16, ((Map<?,?>) standard.viewFor(10).get("seats")).values().stream()
                .map(Map.class::cast).mapToInt(seat -> ((Number) seat.get("cardCount")).intValue())
                .min().orElseThrow());
        assertTrue(cut.invariantViolations().isEmpty());
        assertTrue(standard.invariantViolations().isEmpty());
    }

    @Test void retiredFourTwosPayloadCannotOverrideANewChengduRoom() {
        List<Integer> retiredDeck = new ArrayList<>();
        for (int suit = 1; suit <= 4; suit++)
            for (int rank = 4; rank <= 15; rank++) retiredDeck.add(suit * 100 + rank);
        assertEquals(4, retiredDeck.stream()
                .filter(card -> StandardPokerRuleSet.rank(card) == 15).count());

        PokerAuthoritativeSession session = started(new PdkGameProvider(), 905, 2,
                Map.of("deckCards", retiredDeck, "deckMode", "CUT_40"));
        @SuppressWarnings("unchecked")
        Map<String,Object> options = (Map<String,Object>) session.authoritativeState()
                .get("pdkRuleOptions");
        @SuppressWarnings("unchecked")
        List<Integer> actualDeck = (List<Integer>) options.get("deckCards");
        assertEquals(ChengduPdkRules.cutDeck(), actualDeck);
        assertConserved(session, ChengduPdkRules.cutDeck(), 8);
    }

    @Test void minimumCardHolderLeadsAndSnapshotRestoreReplayAreExact() {
        PdkGameProvider provider = new PdkGameProvider();
        PokerAuthoritativeSession session = started(provider, 903, 2, Map.of());
        PokerTurnState turn = (PokerTurnState) session.authoritativeState().get("state");
        Comparator<Integer> cardOrder = Comparator.comparingInt(
                (Integer card) -> StandardPokerRuleSet.rank(card))
                .thenComparingInt(Integer::intValue);
        int expected = turn.hands().entrySet().stream().min(Comparator.comparing(entry ->
                entry.getValue().stream().min(cardOrder).orElseThrow(), cardOrder))
                .orElseThrow().getKey();
        assertEquals(expected, session.viewFor(10).get("currentSeat"));
        Map<String,Object> saved = session.authoritativeState();
        assertEquals(saved, provider.restoreAuthoritativeSession(saved).orElseThrow()
                .authoritativeState());
        assertEquals(saved, PokerAuthoritativeSession.replay(
                com.aoo.bcg.gamespi.StatePayload.copyOf(saved), List.of()).asMap());

        Map<String,Object> corrupt = new LinkedHashMap<>(saved);
        corrupt.put("undealtCards", List.of());
        assertThrows(IllegalStateException.class,
                () -> provider.restoreAuthoritativeSession(corrupt).orElseThrow());
    }

    @Test void requiredSpadeThreeFallsBackToMinimumWhenCutDeckRemovesIt() {
        PdkGameProvider provider = new PdkGameProvider();
        PokerAuthoritativeSession session = started(provider, 904, 2,
                Map.of("playRule", List.of("remove_three_four", "require_spade_three")));
        PokerTurnState turn = (PokerTurnState) session.authoritativeState().get("state");
        Comparator<Integer> cardOrder = Comparator.comparingInt(
                (Integer card) -> StandardPokerRuleSet.rank(card))
                .thenComparingInt(Integer::intValue);
        Map.Entry<Integer,List<Integer>> expected = turn.hands().entrySet().stream()
                .min(Comparator.comparing(entry ->
                        entry.getValue().stream().min(cardOrder).orElseThrow(), cardOrder))
                .orElseThrow();
        int minimum = expected.getValue().stream().min(cardOrder).orElseThrow();
        assertEquals(expected.getKey(), session.viewFor(10).get("currentSeat"));
        assertEquals(minimum, session.viewFor(10).get("activeRequiredFirstCard"));
        assertEquals(103, ((Map<?,?>) session.authoritativeState().get("pdkRuleOptions"))
                .get("requiredFirstCard"));
    }

    @Test void cardTypeComparisonHintReportSingleAndSettlementMatrixMatchXqp() {
        PaoDeKuaiConfig config = ChengduPdkRules.defaults();
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config,
                ChengduPdkRules.profile("matrix", false));
        PaoDeKuaiContext turn = new PaoDeKuaiContext(false, 8,
                ChengduPdkRules.standardDeck());
        assertEquals("SINGLE", rules.recognize(List.of(105), turn).type());
        assertEquals("PAIR", rules.recognize(List.of(105, 205), turn).type());
        assertEquals("TRIPLE", rules.recognize(List.of(105, 205, 305), turn).type());
        assertEquals("TRIPLE_WITH_ONE",
                rules.recognize(List.of(105, 205, 305, 106), turn).type());
        CardCombination lower = rules.recognize(List.of(105, 205, 305, 114, 214), turn);
        CardCombination higher = rules.recognize(List.of(106, 206, 306, 107, 207), turn);
        assertEquals("TRIPLE_WITH_PAIR", lower.type());
        assertTrue(rules.canBeat(higher, lower, turn));
        CardCombination kingsWithSingles = rules.recognize(
                List.of(113, 213, 313, 109, 106), turn);
        CardCombination acesWithPair = rules.recognize(
                List.of(114, 214, 314, 112, 212), turn);
        assertEquals("TRIPLE_WITH_TWO", kingsWithSingles.type());
        assertEquals("TRIPLE_WITH_PAIR", acesWithPair.type());
        assertFalse(config.compareTripleAttachments());
        assertTrue(rules.canBeat(acesWithPair, kingsWithSingles, turn));
        assertEquals("STRAIGHT", rules.recognize(List.of(105, 106, 107, 108, 109), turn).type());
        assertEquals("CONSECUTIVE_PAIRS",
                rules.recognize(List.of(105, 205, 106, 206), turn).type());
        assertEquals("AIRPLANE", rules.recognize(
                List.of(105, 205, 305, 106, 206, 306), turn).type());
        assertEquals("AIRPLANE_WITH_SINGLES", rules.recognize(
                List.of(105, 205, 305, 106, 206, 306, 107, 108), turn).type());
        assertEquals("AIRPLANE_WITH_PAIRS", rules.recognize(
                List.of(105, 205, 305, 106, 206, 306, 107, 207, 108, 208), turn).type());
        assertEquals("FOUR_WITH_TWO",
                rules.recognize(List.of(105, 205, 305, 405, 106, 207), turn).type());
        CardCombination bomb = rules.recognize(List.of(105, 205, 305, 405), turn);
        assertEquals("BOMB", bomb.type());
        assertTrue(rules.canBeat(bomb, higher, turn));

        PaoDeKuaiContext reportedSingle = new PaoDeKuaiContext(false, 1,
                List.of(114, 115));
        assertThrows(IllegalStateException.class,
                () -> rules.validatePlay(List.of(114), reportedSingle));
        List<CardCombination> singleHints = rules.hints(List.of(114, 115), null,
                reportedSingle).stream().filter(hint -> hint.type().equals("SINGLE")).toList();
        assertEquals(List.of(List.of(115)), singleHints.stream().map(CardCombination::cards).toList());

        PaoDeKuaiConfig finalOnly = PdkPublishedRuleOptions.apply(
                Map.of("playRule", List.of("four_with_two")), config);
        PaoDeKuaiRuleSet finalRules = new PaoDeKuaiRuleSet(finalOnly,
                ChengduPdkRules.profile("final", false));
        assertThrows(IllegalArgumentException.class, () -> finalRules.recognize(
                List.of(105, 205, 305), new PaoDeKuaiContext(false, 8,
                        List.of(105, 205, 305, 106))));
        assertEquals("TRIPLE", finalRules.recognize(List.of(105, 205, 305),
                new PaoDeKuaiContext(false, 8, List.of(105, 205, 305))).type());

        PaoDeKuaiConfig optional = PdkPublishedRuleOptions.apply(Map.of("playRule",
                List.of("triple_ace_bomb", "require_spade_three")), config);
        PaoDeKuaiRuleSet optionalRules = new PaoDeKuaiRuleSet(optional,
                ChengduPdkRules.profile("optional", false));
        assertEquals("SPECIAL_TRIPLE_BOMB", optionalRules.recognize(
                List.of(114, 214, 314), turn).type());
        PaoDeKuaiContext first = new PaoDeKuaiContext(true, 8, List.of(103, 104), 103, true);
        assertThrows(IllegalStateException.class,
                () -> optionalRules.validatePlay(List.of(104), first));
        assertDoesNotThrow(() -> optionalRules.validatePlay(List.of(103), first));

        SettlementPayload score = PdkScoringPolicy.standard().settle(new PdkSettlementContext(
                99, 1, "matrix", 0, 0, Map.of(0, 10L, 1, 11L),
                Map.of(0, List.of(), 1, List.of(105, 106, 107, 108, 109)),
                Map.of(0, 3, 1, 2), Map.of(0, 1, 1, 0),
                Map.of(0, 6, 1, 4), Map.of(0, 0, 1, 0), -1, -1,
                ChengduPdkRules.profile("matrix", false), config));
        assertEquals(10L, score.scoreDelta().get(10L));
        assertEquals(-10L, score.scoreDelta().get(11L));
    }

    private static PokerAuthoritativeSession started(PdkGameProvider provider, long room,
            int players, Map<String,Object> extra) {
        Map<String,Object> rules = new LinkedHashMap<>(extra);
        rules.put("playerCount", players);
        rules.put("shuffleSeed", 77L);
        PokerAuthoritativeSession session = (PokerAuthoritativeSession) provider.roomFactory()
                .create(new RoomCreationContext(room, 10, rules)).requireAuthoritativeSession();
        long sequence = 1;
        for (int seat = 1; seat < players; seat++)
            session.execute(command(room, sequence++, seat, 10 + seat, "join_req"));
        for (int seat = 0; seat < players; seat++)
            session.execute(command(room, sequence++, seat, 10 + seat, "ready_req"));
        return session;
    }

    private static GameCommandRequest command(long room, long sequence, int seat, long player,
            String id) {
        return new GameCommandRequest(id, "chengdu-" + sequence, sequence, room, 1,
                PdkGameProvider.VERSION, String.valueOf(player), seat, Map.of());
    }

    private static void assertConserved(PokerAuthoritativeSession session,
            List<Integer> expectedDeck, int expectedStock) {
        Map<String,Object> saved = session.authoritativeState();
        PokerTurnState turn = (PokerTurnState) saved.get("state");
        List<Integer> cards = new ArrayList<>();
        turn.hands().values().forEach(cards::addAll);
        @SuppressWarnings("unchecked") List<Integer> stock = (List<Integer>) saved.get("undealtCards");
        cards.addAll(stock);
        assertEquals(expectedStock, stock.size());
        assertEquals(expectedDeck.size(), cards.size());
        assertEquals(new HashSet<>(expectedDeck), new HashSet<>(cards));
    }

    private static void assertDeck(List<Integer> deck, int size, boolean hasThreesAndFours) {
        assertEquals(size, deck.size());
        assertEquals(size, new HashSet<>(deck).size());
        assertEquals(3, deck.stream().filter(card -> StandardPokerRuleSet.rank(card) == 14).count());
        assertEquals(1, deck.stream().filter(card -> StandardPokerRuleSet.rank(card) == 15).count());
        assertEquals(hasThreesAndFours,
                deck.stream().anyMatch(card -> StandardPokerRuleSet.rank(card) == 3));
        assertEquals(hasThreesAndFours,
                deck.stream().anyMatch(card -> StandardPokerRuleSet.rank(card) == 4));
    }
}
