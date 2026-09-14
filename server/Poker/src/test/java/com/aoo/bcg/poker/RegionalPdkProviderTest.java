package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
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
                () -> assertEquals("PAIRS", ls.get("tripleAttachmentMode")),
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
        CardCombination lsPrevious = lsRules.recognize(List.of(103, 203, 303, 106, 206), null);
        CardCombination lsLowPair = lsRules.recognize(List.of(104, 204, 304, 105, 205), null);
        CardCombination lsHighPair = lsRules.recognize(List.of(104, 204, 304, 107, 207), null);
        assertAll(
                () -> assertEquals("TRIPLE_WITH_PAIR", lsPrevious.type()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> lsRules.recognize(List.of(104, 204, 304, 105), null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> lsRules.recognize(List.of(104, 204, 304, 105, 106), null)),
                () -> assertFalse(lsRules.canBeat(lsLowPair, lsPrevious, null)),
                () -> assertTrue(lsRules.canBeat(lsHighPair, lsPrevious, null)));
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
                        "playRule", List.of("compare_attachments", "triple_with_one",
                                "four_with_two", "four_ace_rank", "all_special_patterns"),
                        "roomRestriction", List.of("ip_limit", "timeout_auto_play",
                                "distance_warning", "chat_muted"))))
                .requireAuthoritativeSession();
        Map<String,Object> saved = session.authoritativeState();
        Map<?,?> rules = (Map<?,?>) saved.get("pdkRuleOptions");
        assertEquals(10, rules.get("cardsPerPlayer"));
        assertEquals(40, ((List<?>) rules.get("deckCards")).size());
        assertEquals(105, rules.get("bankerSelectionCard"));
        assertEquals(4, rules.get("jinHuaScoreUnit"));
        assertEquals(true, rules.get("competeDealerStartAfterBanker"));
        assertEquals(List.of(5), rules.get("fourOfKindPatternRanks"));
        assertTrue(((List<?>) rules.get("initialHandPatterns")).containsAll(List.of(
                "ALL_BIG", "ALL_SMALL", "ALL_RED", "ALL_BLACK", "FULL_STRAIGHT", "ALL_PAIRS")));
        assertEquals(saved, provider.restoreAuthoritativeSession(saved).orElseThrow()
                .authoritativeState());
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
        return new GameCommandRequest(action + "_req", "ls201-" + action + '-' + sequence,
                sequence, roomId, 1, "xqp-equivalent-1", String.valueOf(player), seat,
                Map.of());
    }
}
