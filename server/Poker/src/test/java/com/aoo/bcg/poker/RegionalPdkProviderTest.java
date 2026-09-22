package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RegionalPdkProviderTest {
    @Test void regionalRuleMatrixPublishesOneAtomicSnapshotAndEnforcesAttachments() {
        GameProvider chengdu = provider(8, "CD201", "成都跑得快", "chengdu", "matrix-1");
        GameProvider neijiang = provider(629, "NJ201", "内江跑得快", "neijiang", "matrix-1");
        GameProvider liangshan = provider(90005, "LS201", "凉山跑得快", "liangshan", "matrix-1");

        var cdSession = session(chengdu, 81001, Map.of("playerCount", 2));
        var njSession = session(neijiang, 81002, Map.of("playerCount", 2,
                "attachmentComparison", "compare"));
        var lsSession = session(liangshan, 81003, Map.of("playerCount", 2,
                "playRule", List.of("triple_with_one", "compare_attachments")));

        Map<String,Object> cd = ruleOptions(cdSession);
        Map<String,Object> nj = ruleOptions(njSession);
        Map<String,Object> ls = ruleOptions(lsSession);
        assertAll(
                () -> assertEquals(5, cd.get("minimumStraightLength")),
                () -> assertEquals(5, nj.get("minimumStraightLength")),
                () -> assertEquals(3, ls.get("minimumStraightLength")),
                () -> assertEquals("EITHER", cd.get("tripleAttachmentMode")),
                () -> assertEquals("EITHER", nj.get("tripleAttachmentMode")),
                () -> assertEquals("SINGLE_OR_PAIR", ls.get("tripleAttachmentMode")),
                () -> assertEquals(false, cd.get("compareTripleAttachments")),
                () -> assertEquals(true, nj.get("compareTripleAttachments")),
                () -> assertEquals(true, ls.get("compareTripleAttachments")));

        PaoDeKuaiRuleSet cdRules = rules(cd);
        CardCombination cdPrevious = cdRules.recognize(List.of(113, 213, 313, 109, 106), null);
        CardCombination cdCandidate = cdRules.recognize(List.of(114, 214, 314, 112, 212), null);
        assertTrue(cdRules.canBeat(cdCandidate, cdPrevious, null));

        PaoDeKuaiRuleSet njRules = rules(nj);
        CardCombination njPrevious = njRules.recognize(List.of(103, 203, 303, 106), null);
        CardCombination njLowWing = njRules.recognize(List.of(104, 204, 304, 105), null);
        CardCombination njHighWing = njRules.recognize(List.of(104, 204, 304, 107), null);
        assertFalse(njRules.canBeat(njLowWing, njPrevious, null));
        assertTrue(njRules.canBeat(njHighWing, njPrevious, null));

        PaoDeKuaiRuleSet lsRules = rules(ls);
        CardCombination lsSingle = lsRules.recognize(List.of(104, 204, 304, 105), null);
        CardCombination lsPair = lsRules.recognize(List.of(104, 204, 304, 105, 205), null);
        CardCombination lsFourWithSingles = lsRules.recognize(
                List.of(104, 204, 304, 404, 105, 106), null);
        CardCombination lsFourWithPairs = lsRules.recognize(
                List.of(104, 204, 304, 404, 105, 205, 106, 206), null);
        assertAll(
                () -> assertEquals("TRIPLE_WITH_ONE", lsSingle.type()),
                () -> assertEquals("TRIPLE_WITH_PAIR", lsPair.type()),
                () -> assertThrows(IllegalArgumentException.class, () -> lsRules.recognize(
                        List.of(104, 204, 304, 105, 106), null)),
                () -> assertEquals("FOUR_WITH_TWO", lsFourWithSingles.type()),
                () -> assertEquals("FOUR_WITH_TWO_PAIRS", lsFourWithPairs.type()),
                () -> assertEquals("FOUR_WITH_TWO", lsRules.recognize(
                        List.of(104, 204, 304, 404, 105, 205), null).type()));

        CardCombination previousSingle = lsRules.recognize(List.of(107), null);
        List<CardCombination> maximumControlHints = lsRules.hints(
                List.of(114, 214, 110), previousSingle,
                new PaoDeKuaiContext(false, 3, List.of(114, 214, 110)));
        assertEquals(14, maximumControlHints.getFirst().primaryRank(),
                "Liangshan AA+10 must answer a single with its rule-maximum A");
    }

    @Test void neijiangKeepsIndependentIdentityAndUsesPublishedTwoPlayerCut() {
        GameProvider provider = provider(629, "NJ201", "内江跑得快", "neijiang",
                "legacy-equivalent-1");
        assertEquals("native-pdk-NJ201", provider.defaultConfiguration().get("regionalProvider"));
        assertEquals("NJ201", provider.defaultConfiguration().get("gameCode"));
        assertEquals(40, provider.defaultConfiguration().get("deckSize"));

        var session = provider.roomFactory().create(new RoomCreationContext(62901, 10,
                Map.of("playerCount", 2, "roundCount", 8,
                        "operationTime", 15, "firstPlayRule", "spade_three_first",
                        "playRule", List.of("three_no_attachment", "four_with_two",
                                "triple_ace_bomb", "remove_three_four"))))
                .requireAuthoritativeSession();
        Map<String,Object> saved = session.authoritativeState();
        Map<?,?> rules = (Map<?,?>) saved.get("pdkRuleOptions");
        assertEquals(16, rules.get("cardsPerPlayer"));
        assertEquals(40, ((List<?>) rules.get("deckCards")).size());
        assertEquals(15, rules.get("operationTimeoutSeconds"));
        assertEquals(true, rules.get("selectBankerEveryRound"));
        assertEquals(saved, provider.restoreAuthoritativeSession(saved).orElseThrow()
                .authoritativeState());
    }

    @Test void neijiangThreePlayersUseTheFortyEightCardPublicDeck() {
        GameProvider provider = provider(629, "NJ201", "内江跑得快", "neijiang",
                "legacy-equivalent-1");
        var session = provider.roomFactory().create(new RoomCreationContext(62902, 10,
                Map.of("playerCount", 3, "playRule", List.of("remove_three_four"))))
                .requireAuthoritativeSession();
        Map<?,?> rules = (Map<?,?>) session.authoritativeState().get("pdkRuleOptions");
        assertEquals(48, ((List<?>) rules.get("deckCards")).size());
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(62903, 10,
                        Map.of("playerCount", 4))));
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(62904, 10,
                        Map.of("playerCount", 2, "operationTime", 9))));
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(62905, 10,
                        Map.of("playerCount", 2, "playRule", List.of("unknown_rule")))));
    }

    @Test void liangshanUsesExactXqpDeckAndRestoresDealerAndSettlementOptions() {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        assertEquals("native-pdk-LS201", provider.defaultConfiguration().get("regionalProvider"));
        assertEquals("LS201", provider.defaultConfiguration().get("gameCode"));
        assertEquals(32, provider.defaultConfiguration().get("deckSize"));
        assertEquals(8, provider.defaultConfiguration().get("cardsPerPlayer"));
        assertEquals(3, provider.defaultConfiguration().get("minimumStraightLength"));

        var session = provider.roomFactory().create(new RoomCreationContext(9000501, 10,
                Map.of("playerCount", 4, "roundCount", 8, "dealCardCount", 10,
                        "operationTime", 15, "jinHuaScore", 4,
                        "robDealerRule", "dealer_last",
                        "playRule", List.of("compare_attachments", "triple_with_one_or_pair",
                                "four_with_two_or_pairs", "four_ace_rank", "all_special_patterns"),
                        "roomRestriction", List.of("ip_limit", "timeout_auto_play",
                                "distance_warning", "chat_muted"))))
                .requireAuthoritativeSession();
        Map<String,Object> saved = session.authoritativeState();
        Map<?,?> rules = (Map<?,?>) saved.get("pdkRuleOptions");
        assertEquals(10, rules.get("cardsPerPlayer"));
        assertEquals(40, ((List<?>) rules.get("deckCards")).size());
        assertEquals(105, rules.get("bankerSelectionCard"));
        assertEquals(false, rules.get("selectBankerEveryRound"));
        assertEquals(1, rules.get("baseScore"));
        assertEquals(4, rules.get("jinHuaScoreUnit"));
        assertEquals(true, rules.get("competeDealerStartAfterBanker"));
        assertEquals(List.of(5), rules.get("fourOfKindPatternRanks"));
        assertTrue(((List<?>) rules.get("initialHandPatterns")).containsAll(List.of(
                "ALL_SINGLES", "FULL_STRAIGHT", "FULL_CONSECUTIVE_PAIRS", "ALL_PAIRS",
                "ALL_BIG", "ALL_SMALL", "ALL_RED", "ALL_BLACK")));
        assertEquals(saved, provider.restoreAuthoritativeSession(saved).orElseThrow()
                .authoritativeState());
    }

    @Test void liangshanWorkbookOperationTimeReachesTheAuthoritativeRuntimeUnchanged() {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        var session = provider.roomFactory().create(new RoomCreationContext(9000510, 10,
                Map.of("playerCount", 2, "roundCount", 8, "dealCardCount", 8,
                        "operationTime", 10_000)))
                .requireAuthoritativeSession();
        Map<?,?> rules = (Map<?,?>) session.authoritativeState().get("pdkRuleOptions");
        assertEquals(10_000, rules.get("operationTime"));
        assertEquals(10_000, rules.get("operationTimeoutSeconds"));
    }

    @Test void liangshanNextRoundBankerIsPreviousRoundWinner() {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        long roomId = 9000599L;
        var session = provider.roomFactory().create(new RoomCreationContext(roomId, 10,
                Map.of("playerCount", 2, "roundCount", 8, "dealCardCount", 8,
                        "shuffleSeed", 9000599L))).requireAuthoritativeSession();
        long sequence = 1;
        session.execute(command(roomId, 1, 11, "join", sequence++));
        session.execute(command(roomId, 0, 10, "ready", sequence++));
        session.execute(command(roomId, 1, 11, "ready", sequence++));
        int guard = 0;
        while (!Boolean.TRUE.equals(session.viewFor(10).get("finished"))) {
            assertTrue(guard++ < 200, "Liangshan authority round did not converge");
            int seat = ((Number) session.viewFor(10).get("currentSeat")).intValue();
            long player = 10L + seat;
            var hint = session.execute(command(roomId, seat, player, "hint", sequence++));
            @SuppressWarnings("unchecked")
            List<CardCombination> hints = (List<CardCombination>) hint.body().get("hints");
            if (hints.isEmpty())
                session.execute(command(roomId, seat, player, "pass", sequence++));
            else
                session.execute(command(roomId, seat, player, "play", sequence++,
                        Map.of("cards", hints.getFirst().cards())));
        }
        int winner = ((Number) session.viewFor(10).get("winnerSeat")).intValue();
        session.execute(command(roomId, 0, 10, "continue", sequence++));
        session.execute(command(roomId, 1, 11, "continue", sequence));
        assertEquals(2, session.viewFor(10).get("roundNo"));
        assertEquals(winner, session.viewFor(10).get("bankerSeat"));
        assertEquals(winner, session.viewFor(10).get("currentSeat"));
    }

    @Test void liangshanRecoveryRepairsLegacyLooseTripleWings() {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        var session = provider.roomFactory().create(new RoomCreationContext(9000598, 10,
                Map.of("playerCount", 2,
                        "playRule", List.of("triple_with_one_or_pair"))))
                .requireAuthoritativeSession();
        Map<String,Object> legacy = new LinkedHashMap<>(session.authoritativeState());
        @SuppressWarnings("unchecked")
        Map<String,Object> oldRules = new LinkedHashMap<>(
                (Map<String,Object>) legacy.get("pdkRuleOptions"));
        oldRules.put("tripleAttachmentMode", "SINGLES");
        legacy.put("pdkRuleOptions", Map.copyOf(oldRules));

        Map<?,?> restoredRules = (Map<?,?>) provider.restoreAuthoritativeSession(legacy)
                .orElseThrow().authoritativeState().get("pdkRuleOptions");
        assertEquals("SINGLE_OR_PAIR", restoredRules.get("tripleAttachmentMode"));
        assertEquals(true, restoredRules.get("prioritizeMaximumWithOneOrdinaryPlay"));
        assertEquals(true, restoredRules.get("prioritizeLargestLeadWithoutMaximum"));
        assertEquals(true, restoredRules.get("prioritizeMaximumLeadUnlessConnectedRun"));
        assertEquals(true, restoredRules.get("prioritizeMaximumResponseWithinThreePlays"));
        assertEquals(true, restoredRules.get("prioritizeLargestLeadUnlessMaximumStraight"));
    }

    @Test void chengduRecoveryRepairsLegacyRequiredCardLeakAndOpeningTurn() {
        GameProvider provider = provider(8, "CD201", "成都跑得快", "chengdu",
                "xqp-equivalent-1");
        long roomId = 81015L;
        var session = provider.roomFactory().create(new RoomCreationContext(roomId, 10,
                Map.of("playerCount", 2, "roundCount", 8,
                        "playRule", List.of("remove_three_four", "require_spade_three"))))
                .requireAuthoritativeSession();
        long sequence = 1;
        session.execute(command(roomId, 1, 11, "join", sequence++));
        session.execute(command(roomId, 0, 10, "ready", sequence++));
        session.execute(command(roomId, 1, 11, "ready", sequence++));
        int guard = 0;
        while (!Boolean.TRUE.equals(session.viewFor(10).get("finished"))) {
            assertTrue(guard++ < 200, "Chengdu authority round did not converge");
            int seat = ((Number) session.viewFor(10).get("currentSeat")).intValue();
            long player = 10L + seat;
            @SuppressWarnings("unchecked")
            List<CardCombination> hints = (List<CardCombination>) session.execute(
                    command(roomId, seat, player, "hint", sequence++)).body().get("hints");
            if (hints.isEmpty())
                session.execute(command(roomId, seat, player, "pass", sequence++));
            else
                session.execute(command(roomId, seat, player, "play", sequence++,
                        Map.of("cards", hints.getFirst().cards())));
        }
        int winner = ((Number) session.viewFor(10).get("winnerSeat")).intValue();
        session.execute(command(roomId, 0, 10, "continue", sequence++));
        session.execute(command(roomId, 1, 11, "continue", sequence));

        Map<String,Object> legacy = new LinkedHashMap<>(session.authoritativeState());
        @SuppressWarnings("unchecked")
        Map<String,Object> oldRules = new LinkedHashMap<>(
                (Map<String,Object>) legacy.get("pdkRuleOptions"));
        oldRules.put("requiredFirstCardRounds", 99_999);
        legacy.put("pdkRuleOptions", Map.copyOf(oldRules));
        int wrongSeat = winner == 0 ? 1 : 0;
        PokerTurnState turn = (PokerTurnState) legacy.get("state");
        int staleCard = turn.hands().get(wrongSeat).getFirst();
        legacy.put("activeRequiredFirstCard", staleCard);
        legacy.put("bankerSeat", wrongSeat);
        legacy.put("initialLeadSeat", wrongSeat);
        legacy.put("state", new PokerTurnState(turn.hands(), wrongSeat, null, -1,
                java.util.Set.of(), false, -1));
        @SuppressWarnings("unchecked")
        Map<String,Object> oldDeadline = new LinkedHashMap<>(
                (Map<String,Object>) legacy.get("operationDeadline"));
        oldDeadline.put("seatId", wrongSeat);
        legacy.put("operationDeadline", Map.copyOf(oldDeadline));

        var restored = provider.restoreAuthoritativeSession(legacy).orElseThrow();
        Map<String,Object> restoredState = restored.authoritativeState();
        Map<?,?> restoredRules = (Map<?,?>) restoredState.get("pdkRuleOptions");
        assertEquals(1, restoredRules.get("requiredFirstCardRounds"));
        assertFalse(restoredState.containsKey("activeRequiredFirstCard"));
        assertEquals(winner, restoredState.get("bankerSeat"));
        assertEquals(winner, restoredState.get("initialLeadSeat"));
        assertEquals(winner, restored.viewFor(10).get("currentSeat"));
        assertEquals(List.of(), restored.invariantViolations());
    }

    @Test void liangshanTwoAndThreePlayerRoomsDealExactHandsAndKeepOnlyTheXqpStock() {
        assertAll(
                () -> assertLiangshanDeal(2, 8, 8, 16, 7),
                () -> assertLiangshanDeal(3, 8, 8, 8, 7),
                () -> assertLiangshanDeal(2, 10, 10, 20, 5),
                () -> assertLiangshanDeal(3, 10, 10, 10, 5));
    }

    @Test void liangshanDefaultsMatchTheXqpArea950RuntimeRules() {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        var session = provider.roomFactory().create(new RoomCreationContext(9000510, 10,
                Map.of("playerCount", 2))).requireAuthoritativeSession();
        Map<?,?> rules = (Map<?,?>) session.authoritativeState().get("pdkRuleOptions");
        assertEquals(3, rules.get("minimumStraightLength"));
        assertEquals("EITHER", rules.get("airplaneAttachmentMode"));
        assertEquals("DISABLED", rules.get("tripleAttachmentMode"));
        assertEquals("DISABLED", rules.get("fourAttachmentMode"));
        assertEquals("ANYTIME", rules.get("tripleWithoutAttachmentTiming"));
        assertEquals("ALL_IN_ORDER", rules.get("playedCardVisibility"));
        assertEquals(false, rules.get("mustBeatWhenPossible"));
        assertEquals(true, rules.get("forceHighestSingleAgainstReportedSingle"));
        assertEquals(false, rules.get("forceHighestPairAgainstReportedPair"));
        assertEquals(103, rules.get("requiredFirstCard"));
        assertEquals(1, rules.get("requiredFirstCardRounds"));
        assertEquals(15, rules.get("operationTimeoutSeconds"));
        assertEquals(1, rules.get("jinHuaScoreUnit"));
        assertEquals("DISABLED", rules.get("bombScoreMode"));
        assertEquals(false, rules.get("competeDealerEnabled"));
        assertEquals(List.of(), rules.get("initialHandPatterns"));
        assertEquals(List.of(), rules.get("fourOfKindPatternRanks"));
        assertEquals(60, rules.get("offlineDissolveSeconds"));
        assertEquals(86400, rules.get("declaredRoundTimeoutSeconds"));
        assertEquals(false, rules.get("uniqueIpRequired"));
        assertEquals(true, rules.get("textChatEnabled"));
    }

    @Test void liangshanRejectsUnpublishedCardCountsAndPlayers() {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(9000502, 10,
                        Map.of("playerCount", 3, "dealCardCount", 9))));
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(9000503, 10,
                        Map.of("playerCount", 5, "dealCardCount", 8))));
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(9000504, 10,
                        Map.of("playerCount", 2, "jinHuaScore", 6))));
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(9000505, 10,
                        Map.of("playerCount", 2, "robDealerRule", "legacy_fallback"))));
    }

    private static GameProvider provider(int id, String code, String name, String city,
            String version) {
        return PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(id, code, name,
                GameCategory.POKER, PaoDeKuaiFamily.CODE, RegionScope.CITY,
                "sichuan", city, version)).orElseThrow();
    }

    private static com.aoo.bcg.gamespi.AuthoritativeGameSession session(GameProvider provider,
            long roomId, Map<String,Object> rules) {
        return provider.roomFactory().create(new RoomCreationContext(roomId, 10, rules))
                .requireAuthoritativeSession();
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> ruleOptions(
            com.aoo.bcg.gamespi.AuthoritativeGameSession session) {
        Map<String,Object> persisted = (Map<String,Object>) session.authoritativeState()
                .get("pdkRuleOptions");
        assertEquals(persisted, session.viewFor(10).get("ruleOptions"));
        return persisted;
    }

    private static PaoDeKuaiRuleSet rules(Map<String,Object> options) {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(options,
                PaoDeKuaiConfig.defaults());
        PokerRuleProfile profile = PdkPublishedRuleOptions.profile("matrix", options, config,
                PdkRuleProfiles.flexibleTwoToFourPlayers("matrix", null));
        return new PaoDeKuaiRuleSet(config, profile);
    }

    private static void assertLiangshanDeal(int players, int selectedCards, int handSize,
            int stockSize, int minimumRank) {
        GameProvider provider = provider(90005, "LS201", "凉山跑得快", "liangshan",
                "xqp-equivalent-1");
        long roomId = 9000520L + players * 10L + selectedCards;
        var session = provider.roomFactory().create(new RoomCreationContext(roomId, 10,
                Map.of("playerCount", players, "dealCardCount", selectedCards,
                        "shuffleSeed", roomId))).requireAuthoritativeSession();
        for (int seat = 1; seat < players; seat++)
            session.execute(command(roomId, seat, 10L + seat, "join", seat));
        for (int seat = 0; seat < players; seat++)
            session.execute(command(roomId, seat, 10L + seat, "ready", players + seat));
        assertEquals("PLAYING", session.viewFor(10).get("phase"));
        assertEquals(stockSize, session.viewFor(10).get("stockCount"));
        for (int seat = 0; seat < players; seat++) {
            @SuppressWarnings("unchecked") Map<Integer,Object> seats =
                    (Map<Integer,Object>) session.viewFor(10L + seat).get("seats");
            @SuppressWarnings("unchecked") List<Integer> cards =
                    (List<Integer>) ((Map<String,Object>) seats.get(seat)).get("cards");
            assertEquals(handSize, cards.size());
            assertTrue(cards.stream().allMatch(card -> PokerCardCodec.rank(card) >= minimumRank
                    && PokerCardCodec.rank(card) <= 14));
            assertTrue(cards.stream().noneMatch(card -> PokerCardCodec.rank(card) == 15));
        }
        @SuppressWarnings("unchecked") List<Integer> deck = (List<Integer>)
                ((Map<String,Object>) session.authoritativeState().get("pdkRuleOptions"))
                        .get("deckCards");
        assertEquals(selectedCards == 8 ? 32 : 40, deck.size());
        assertTrue(deck.stream().noneMatch(card -> PokerCardCodec.rank(card) == 15
                || PokerCardCodec.suit(card) == 5));
        assertEquals(List.of(), session.invariantViolations());
    }

    private static GameCommandRequest command(long roomId, int seat, long player,
            String action, long sequence) {
        return command(roomId, seat, player, action, sequence, Map.of());
    }

    private static GameCommandRequest command(long roomId, int seat, long player,
            String action, long sequence, Map<String,Object> body) {
        return new GameCommandRequest(action + "_req", "ls201-" + action + '-' + sequence,
                sequence, roomId, 1, "xqp-equivalent-1", String.valueOf(player), seat,
                body);
    }
}
