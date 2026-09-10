package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 成都跑得快（XQP area=1/gameType=50）的不可变服务端基线。 */
final class ChengduPdkRules {
    private static final List<Integer> STANDARD_DECK = List.of(
            103,104,105,106,107,108,109,110,111,112,113,114,115,
            203,204,205,206,207,208,209,210,211,212,213,214,
            303,304,305,306,307,308,309,310,311,312,313,314,
            403,404,405,406,407,408,409,410,411,412,413);
    private static final List<Integer> CUT_DECK = List.of(
            105,106,107,108,109,110,111,112,113,114,115,
            205,206,207,208,209,210,211,212,213,214,
            305,306,307,308,309,310,311,312,313,314,
            405,406,407,408,409,410,411,412,413);

    private ChengduPdkRules() { }

    static List<Integer> standardDeck() { return STANDARD_DECK; }
    static List<Integer> cutDeck() { return CUT_DECK; }

    static PaoDeKuaiConfig defaults() {
        PdkAdvancedRules advanced = new PdkAdvancedRules(
                1, 0, null, false,
                new PdkAdvancedRules.ScoreRule(PdkAdvancedRules.ScoreMode.HAND_TABLE,
                        2, Integer.MAX_VALUE, true),
                new PdkAdvancedRules.ScoreRule(PdkAdvancedRules.ScoreMode.HAND_TABLE,
                        2, 999, false),
                handScoreTable(),
                new PdkAdvancedRules.BombScore(PdkAdvancedRules.BombMode.FIXED_POINTS,
                        0, 5),
                new PdkAdvancedRules.DealerRule(false, false, false, false, false),
                Set.of(), Set.of(), 0, 3, 0,
                List.of(Set.of(103, 203, 303, 403)),
                10, 5,
                new PdkAdvancedRules.RoomGovernance(PdkAdvancedRules.PayerMode.OWNER,
                        60, false, 1800, false, 0, true, false,
                        PdkAdvancedRules.SettlementPresentation.POPUP,
                        PdkAdvancedRules.EntryMode.PARTICIPANT, List.of(), true, true));
        return new PaoDeKuaiConfig(5, true, true, false, null, true,
                null, false, false, 1, 2, 1, false, 2,
                PaoDeKuaiConfig.AttachmentMode.EITHER,
                PaoDeKuaiConfig.AttachmentMode.EITHER,
                PaoDeKuaiConfig.AttachmentMode.SINGLES,
                PaoDeKuaiConfig.PlayTiming.ANYTIME,
                PaoDeKuaiConfig.PlayTiming.ANYTIME,
                true, false, false, true, true, 16, false, Set.of(),
                PaoDeKuaiConfig.PlayedCardVisibility.LAST_ONLY, advanced);
    }

    static PokerRuleProfile profile(String playVersion, boolean cut) {
        List<Integer> deck = cut ? CUT_DECK : STANDARD_DECK;
        return new PokerRuleProfile(playVersion, deck.size(), 2, cut ? 2 : 3,
                PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER, null,
                5, 2, false, false, true, true,
                1, 16, 2, 16, deck);
    }

    static boolean cutDeckSelected(java.util.Map<String,Object> rules) {
        Object explicit = rules.get("deckMode");
        if (explicit != null) return "CUT_40".equalsIgnoreCase(String.valueOf(explicit));
        Object deck = rules.get("deckCards");
        if (deck instanceof java.util.Collection<?> cards) return cards.size() == CUT_DECK.size();
        return !rules.containsKey("playRule")
                || selections(rules.get("playRule")).contains("remove_three_four");
    }

    private static Set<String> selections(Object raw) {
        if (raw instanceof java.util.Collection<?> values) {
            java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
            values.forEach(value -> result.add(String.valueOf(value)));
            return Set.copyOf(result);
        }
        return raw == null ? Set.of() : Set.of(String.valueOf(raw));
    }

    private static List<PdkAdvancedRules.HandScoreBand> handScoreTable() {
        ArrayList<PdkAdvancedRules.HandScoreBand> bands = new ArrayList<>();
        bands.add(new PdkAdvancedRules.HandScoreBand(1, 0));
        for (int cards = 2; cards <= 17; cards++)
            bands.add(new PdkAdvancedRules.HandScoreBand(cards, cards));
        return List.copyOf(bands);
    }
}
