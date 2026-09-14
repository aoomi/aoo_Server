package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 凉山跑得快：XQP ScLs 的地区配置，复用公共 PDK 状态机。 */
final class LiangshanPdkRules implements PdkRegionRules {
    private static final Set<String> PLAY_RULES = Set.of("compare_attachments",
            "triple_with_one", "four_with_two", "four_ace_rank", "four_aces",
            "four_configured_rank", "all_single",
            "full_consecutive_pairs", "all_big", "all_small", "all_red", "all_black",
            "full_straight", "all_pair", "all_special_patterns");
    private static final Set<String> ROOM_RESTRICTIONS = Set.of("ip_limit", "gps_limit",
            "timeout_auto_play", "distance_warning", "interaction_forbidden", "chat_muted");
    private static final List<Integer> DECK_7_TO_ACE = List.of(
            107,108,109,110,111,112,113,114,
            207,208,209,210,211,212,213,214,
            307,308,309,310,311,312,313,314,
            407,408,409,410,411,412,413,414);
    private static final List<Integer> DECK_5_TO_ACE = List.of(
            105,106,107,108,109,110,111,112,113,114,
            205,206,207,208,209,210,211,212,213,214,
            305,306,307,308,309,310,311,312,313,314,
            405,406,407,408,409,410,411,412,413,414);

    public String gameCode() { return PdkBusinessCodes.LIANGSHAN; }
    public String providerKey() { return "native-pdk-LS201"; }
    public String defaultDeckMode() { return "LS_7_TO_ACE"; }
    public int defaultPlayers() { return 2; }
    public int defaultCardsPerPlayer() { return 8; }

    public PaoDeKuaiConfig defaults() {
        PdkAdvancedRules advanced = new PdkAdvancedRules(
                1, 1, 107, true, PdkAdvancedRules.ScoreRule.disabled(),
                PdkAdvancedRules.ScoreRule.disabled(), scoreTable(8),
                new PdkAdvancedRules.BombScore(PdkAdvancedRules.BombMode.DISABLED, 0, 0),
                new PdkAdvancedRules.DealerRule(false, false, false, false, false),
                Set.of(), Set.of(), 9999, 3, 1, List.of(), 15, -1,
                new PdkAdvancedRules.RoomGovernance(PdkAdvancedRules.PayerMode.OWNER,
                        60, false, 86400, false, 0, false, true,
                        PdkAdvancedRules.SettlementPresentation.POPUP,
                        PdkAdvancedRules.EntryMode.PARTICIPANT, List.of(), false, true));
        return new PaoDeKuaiConfig(3, false, false, false, 103, true,
                null, false, false, 1, 2, 1, false, 2,
                PaoDeKuaiConfig.AttachmentMode.DISABLED,
                PaoDeKuaiConfig.AttachmentMode.EITHER,
                PaoDeKuaiConfig.AttachmentMode.DISABLED,
                PaoDeKuaiConfig.PlayTiming.ANYTIME,
                PaoDeKuaiConfig.PlayTiming.ANYTIME, false, false, false,
                true, true, 8, false, Set.of(),
                PaoDeKuaiConfig.PlayedCardVisibility.ALL_IN_ORDER, advanced);
    }

    public Map<String,Object> authoritativeRules(Map<String,Object> published, int players) {
        if (players < 2 || players > 4)
            throw new IllegalArgumentException("Liangshan PDK supports two to four players");
        Map<String,Object> rules = new LinkedHashMap<>(published);
        PdkRegionalRuleValidation.integerChoice(published, "roundCount", 8,
                Set.of(8, 12, 16));
        int timeout = PdkRegionalRuleValidation.integerRange(published, "operationTime", 15,
                1, 3600);
        int cards = PdkRegionalRuleValidation.integerChoice(published, "dealCardCount",
                integer(published, "cardsPerPlayer", defaultCardsPerPlayer()), Set.of(8, 10));
        PdkRegionalRuleValidation.stringChoice(published, "robDealerRule", "no_compete",
                Set.of("dealer_first", "dealer_last", "first_round_no_compete", "no_compete"));
        PdkRegionalRuleValidation.stringChoices(published, "playRule",
                Set.of(), PLAY_RULES);
        PdkRegionalRuleValidation.stringChoices(published, "roomRestriction",
                Set.of(),
                ROOM_RESTRICTIONS);
        rules.put("cardsPerPlayer", cards);
        rules.put("deckMode", cards == 8 ? "LS_7_TO_ACE" : "LS_5_TO_ACE");
        rules.put("deckCards", cards == 8 ? DECK_7_TO_ACE : DECK_5_TO_ACE);
        rules.put("bankerSelectionCard", cards == 8 ? 107 : 105);
        rules.put("operationTimeoutSeconds", timeout);
        rules.put("selectBankerEveryRound", true);
        rules.put("requiredFirstCard", 103);
        rules.put("requiredFirstCardRounds", 1);
        rules.put("minimumStraightLength", 3);
        rules.put("minimumPairRunLength", 2);
        rules.put("airplaneAttachmentMode", "EITHER");
        rules.put("airplaneWithoutAttachmentTiming", "ANYTIME");
        rules.put("allowAirplaneWithTwo", false);
        rules.put("allowTerminalAttachmentShortage", false);
        rules.put("forceHighestSingleAgainstReportedSingle", true);
        rules.put("forceHighestPairAgainstReportedPair", false);
        // 凉山默认必须压牌；只有开房规则显式配置“非必吃”时，
        // PdkPublishedRuleOptions 才可以用发布值 false 覆盖该默认值。
        rules.put("mustBeatWhenPossible", true);
        rules.put("playedCardVisibility", "ALL_IN_ORDER");
        rules.put("bombScoreMode", "DISABLED");
        rules.put("bombScoreCap", 0);
        rules.put("bombFixedPoints", 0);
        rules.put("handScoreTable", scoreTableFlat(cards));
        rules.putIfAbsent("firstLead", PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER.name());
        normalizeJinHua(published, rules);
        normalizeDealer(published, rules);
        normalizePlayRules(published, rules, cards);
        return Map.copyOf(rules);
    }

    public PokerRuleProfile profile(String version, Map<String,Object> rules,
            PaoDeKuaiConfig config) {
        boolean eight = config.cardsPerPlayer() == 8;
        List<Integer> deck = eight ? DECK_7_TO_ACE : DECK_5_TO_ACE;
        PokerRuleProfile base = new PokerRuleProfile(version, deck.size(), 2, 4,
                PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER, null, 5, 2,
                false, false, true, true, 1, 16, 2, 10, deck);
        return PdkPublishedRuleOptions.profile(version, rules, config, base);
    }

    private static void normalizeJinHua(Map<String,Object> published, Map<String,Object> rules) {
        Object raw = published.get("jinHuaScore");
        if (raw == null) return;
        if (raw instanceof Number n && Set.of(1, 2, 3, 4, 5).contains(n.intValue()))
            rules.put("jinHuaScoreUnit", n.intValue());
        else if ("no_compare".equals(String.valueOf(raw))) rules.put("jinHuaScoreUnit", 0);
        else throw new IllegalArgumentException("invalid Liangshan jinHuaScore");
    }

    private static void normalizeDealer(Map<String,Object> published, Map<String,Object> rules) {
        String mode = String.valueOf(published.getOrDefault("robDealerRule", "no_compete"));
        switch (mode) {
            case "dealer_first" -> dealer(rules, true, false, false);
            case "dealer_last" -> dealer(rules, true, true, false);
            case "first_round_no_compete" -> dealer(rules, true, false, true);
            case "no_compete" -> dealer(rules, false, false, false);
            default -> throw new IllegalArgumentException("invalid Liangshan robDealerRule");
        }
    }

    private static void dealer(Map<String,Object> rules, boolean enabled, boolean startAfter,
            boolean skipFirst) {
        rules.put("competeDealerEnabled", enabled);
        rules.put("competeDealerStartAfterBanker", startAfter);
        rules.put("skipCompeteDealerFirstRound", skipFirst);
        // XQP ScLs type-119 500055/500056 的结算参数为 1：抢庄者必须打春天。
        rules.put("competeDealerMustSpringToWin", enabled);
    }

    private static void normalizePlayRules(Map<String,Object> published, Map<String,Object> rules,
            int cards) {
        Set<String> selected = PdkRegionalRuleValidation.stringChoices(published, "playRule",
                Set.of(), PLAY_RULES);
        // 凉山规则表的“三带一”是三带一对。用 PAIRS 原子能力表达，不能借用
        // EITHER 放行三带单张或两张散牌；比较带牌仍由独立原子能力控制。
        rules.put("tripleAttachmentMode", selected.contains("triple_with_one") ? "PAIRS" : "DISABLED");
        rules.put("tripleWithoutAttachmentTiming", selected.contains("triple_with_one")
                ? "DEALER_RESPONSE_OR_FINAL" : "ANYTIME");
        rules.put("airplaneAttachmentMode", "EITHER");
        rules.put("fourAttachmentMode", selected.contains("four_with_two") ? "EITHER" : "DISABLED");
        rules.put("compareTripleAttachments", selected.contains("compare_attachments"));
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        LinkedHashSet<Integer> ranks = new LinkedHashSet<>();
        if (selected.contains("four_ace_rank") || selected.contains("four_aces"))
            patterns.add("FOUR_ACES");
        if (selected.contains("four_ace_rank") || selected.contains("four_configured_rank")) {
            patterns.add("FOUR_CONFIGURED_RANK");
            ranks.add(cards == 8 ? 7 : 5);
        }
        if (selected.contains("all_single")) patterns.add("ALL_SINGLES");
        boolean allSpecialPatterns = selected.contains("all_special_patterns");
        if (selected.contains("full_straight") || allSpecialPatterns) patterns.add("FULL_STRAIGHT");
        if (selected.contains("full_consecutive_pairs")) patterns.add("FULL_CONSECUTIVE_PAIRS");
        if (selected.contains("all_pair") || allSpecialPatterns) patterns.add("ALL_PAIRS");
        if (selected.contains("all_black") || allSpecialPatterns) patterns.add("ALL_BLACK");
        if (selected.contains("all_red") || allSpecialPatterns) patterns.add("ALL_RED");
        if (selected.contains("all_big") || allSpecialPatterns) patterns.add("ALL_BIG");
        if (selected.contains("all_small") || allSpecialPatterns) patterns.add("ALL_SMALL");
        rules.put("initialHandPatterns", List.copyOf(patterns));
        rules.put("fourOfKindPatternRanks", List.copyOf(ranks));
    }

    private static int integer(Map<String,Object> source, String key, int fallback) {
        Object value = source.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw new IllegalArgumentException(key + " must be numeric");
        return number.intValue();
    }

    private static List<PdkAdvancedRules.HandScoreBand> scoreTable(int cards) {
        List<Integer> flat = scoreTableFlat(cards);
        ArrayList<PdkAdvancedRules.HandScoreBand> result = new ArrayList<>();
        for (int i = 0; i < flat.size(); i += 2)
            result.add(new PdkAdvancedRules.HandScoreBand(flat.get(i), flat.get(i + 1)));
        return List.copyOf(result);
    }

    private static List<Integer> scoreTableFlat(int cards) {
        return cards == 8
                ? List.of(1,0, 2,1, 3,1, 4,1, 5,2, 6,2, 7,2, 8,3)
                : List.of(1,0, 2,1, 3,1, 4,1, 5,1, 6,2, 7,2, 8,2, 9,2, 10,3);
    }
}
