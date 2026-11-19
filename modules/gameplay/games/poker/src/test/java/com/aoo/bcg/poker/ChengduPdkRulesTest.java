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
    @Test void productionShuffleSeedIsServerOwnedAndRoomRulesCannotFixIt() {
        PdkGameProvider provider = new PdkGameProvider();
        RoomCreationContext context = new RoomCreationContext(
                445566L, 10L, Map.of("playerCount", 2, "shuffleSeed", 77L));
        PokerAuthoritativeSession first = (PokerAuthoritativeSession) provider.roomFactory()
                .create(context).requireAuthoritativeSession();
        PokerAuthoritativeSession second = (PokerAuthoritativeSession) provider.roomFactory()
                .create(context).requireAuthoritativeSession();

        long firstSeed = ((Number) first.authoritativeState().get("seed")).longValue();
        long secondSeed = ((Number) second.authoritativeState().get("seed")).longValue();
        assertNotEquals(77L, firstSeed);
        assertNotEquals(445566L, firstSeed);
        assertNotEquals(firstSeed, secondSeed);
    }

    @Test void xqpDecksContainExactlyThreeAcesAndOneTwo() {
        List<Integer> standardDeck = ChengduPdkRules.standardDeck();
        List<Integer> cutDeck = ChengduPdkRules.cutDeck();
        assertDeck(standardDeck, 48, true);
        assertDeck(cutDeck, 40, false);
        assertEquals(3, standardDeck.stream()
                .filter(card -> StandardPokerRuleSet.rank(card) == 14).count());
        assertEquals(1, cutDeck.stream()
                .filter(card -> StandardPokerRuleSet.rank(card) == 15).count());
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
        assertEquals(PaoDeKuaiConfig.AttachmentMode.EITHER, config.fourAttachmentMode());
        assertTrue(config.advancedRules().directWinPatterns()
                .contains(java.util.Set.of(103, 203, 303, 403)));
        assertEquals(5, config.advancedRules().bombScore().points());
        assertTrue(config.advancedRules().governance().uniqueIpRequired());
        assertFalse(config.advancedRules().governance().textChatEnabled());
        assertTrue(config.advancedRules().governance().distanceWarningEnabled());
        assertEquals(5, config.advancedRules().hostingMissThreshold());
    }

    @Test void threeAircraftMayUseADisconnectedCompleteTripleAsRequiredWings() {
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(ChengduPdkRules.defaults(),
                ChengduPdkRules.profile("foreign-triple-wings", false));
        List<Integer> play = List.of(
                109, 209, 309,
                110, 210, 310,
                111, 211, 311,
                106, 206, 306,
                108, 112, 113);

        CardCombination combination = rules.recognize(play,
                new PaoDeKuaiContext(false, 8, play));
        assertEquals("AIRPLANE_WITH_TWO", combination.type());
        assertEquals(11, combination.primaryRank());
        assertDoesNotThrow(() -> rules.validatePlay(play,
                new PaoDeKuaiContext(false, 8, play)));
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
        assertEquals(1, config.advancedRules().requiredFirstCardRounds());
        assertEquals(103, config.advancedRules().bankerSelectionCard());
        assertEquals(java.util.Set.of(14), config.specialTripleBombRanks());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.DISABLED, config.fourAttachmentMode());
        assertTrue(config.compareTripleAttachments());
        assertEquals(30, config.advancedRules().bombScore().points());
        assertTrue(config.advancedRules().governance().gpsAdmissionRequired());
        assertFalse(config.advancedRules().governance().interactionEnabled());
        assertFalse(config.advancedRules().governance().uniqueIpRequired());
    }

    @Test void everyPublishedChengduWorkbookSelectionReachesTheRuntimePolicy() {
        Map<String,Object> selected = Map.of(
                "playerCount", 2,
                "roundCount", 16,
                "operationTimeoutSeconds", 20,
                "firstPlayRule", "spade_three_first",
                "bombScore", 30,
                "rule_chengdu2001_0001", List.of("option_0001"),
                "playRule", List.of("three_no_attachment", "four_with_two",
                        "triple_ace_bomb", "remove_three_four", "require_spade_three"),
                "roomRestriction", List.of("ip_limit", "gps_limit", "timeout_auto_play",
                        "distance_warning", "interaction_forbidden", "chat_muted"));

        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(
                selected, ChengduPdkRules.defaults());
        PdkAdvancedRules advanced = config.advancedRules();
        PdkAdvancedRules.RoomGovernance governance = advanced.governance();

        assertTrue(ChengduPdkRules.cutDeckSelected(selected));
        assertEquals(103, config.requiredFirstCard());
        assertEquals(1, advanced.requiredFirstCardRounds());
        assertEquals(103, advanced.bankerSelectionCard());
        assertEquals(30, advanced.bombScore().points());
        assertEquals(20, advanced.operationTimeoutSeconds());
        assertEquals(5, advanced.hostingMissThreshold());
        assertEquals(java.util.Set.of(14), config.specialTripleBombRanks());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.SINGLES, config.fourAttachmentMode());
        assertEquals(PaoDeKuaiConfig.PlayTiming.ANYTIME,
                config.tripleWithoutAttachmentTiming());
        assertEquals(PaoDeKuaiConfig.PlayTiming.ANYTIME,
                config.airplaneWithoutAttachmentTiming());
        assertTrue(governance.uniqueIpRequired());
        assertTrue(governance.gpsAdmissionRequired());
        assertTrue(governance.distanceWarningEnabled());
        assertFalse(governance.interactionEnabled());
        assertFalse(governance.textChatEnabled());
        assertEquals(PdkAdvancedRules.SettlementPresentation.POPUP,
                governance.settlementPresentation());

        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config,
                ChengduPdkRules.profile("published-all-options", true));
        assertEquals("SPECIAL_TRIPLE_BOMB", rules.recognize(
                List.of(114, 214, 314), null).type());
        PaoDeKuaiContext first = new PaoDeKuaiContext(
                true, 8, List.of(103, 105), 103, true);
        assertThrows(IllegalStateException.class,
                () -> rules.validatePlay(List.of(105), first));
        assertDoesNotThrow(() -> rules.validatePlay(List.of(103), first));
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
                .thenComparing(Comparator.comparingInt(PokerCardCodec::suit).reversed())
                .thenComparingInt(Integer::intValue);
        int currentSeat = turn.currentSeat();
        List<Integer> bankerHand = turn.hands().get(currentSeat);
        int minimum = bankerHand.stream().min(cardOrder).orElseThrow();
        assertEquals(currentSeat, session.viewFor(10).get("currentSeat"));
        assertEquals(minimum, session.viewFor(10).get("activeRequiredFirstCard"));
        assertEquals(103, ((Map<?,?>) session.authoritativeState().get("pdkRuleOptions"))
                .get("requiredFirstCard"));
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
        return command(room, sequence, seat, player, id, Map.of());
    }

    private static GameCommandRequest command(long room, long sequence, int seat, long player,
            String id, Map<String,Object> body) {
        return new GameCommandRequest(id, "chengdu-" + sequence, sequence, room, 1,
                PdkGameProvider.VERSION, String.valueOf(player), seat, body);
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
