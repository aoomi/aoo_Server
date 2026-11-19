package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 安岳使用 40 张牌，并将规则表的“四个三”解释为四个五。 */
final class AnyuePdkRegionRules implements PdkRegionRules {
    private static final int ANYUE_HOSTING_MISS_THRESHOLD_FALLBACK = 3;
    public String gameCode() { return PdkBusinessCodes.ANYUE; }
    public String providerKey() { return "native-pdk-" + PdkBusinessCodes.ANYUE; }
    public String defaultDeckMode() { return "CUT_40"; }
    public int defaultPlayers() { return 2; }
    public int defaultCardsPerPlayer() { return 16; }
    public PaoDeKuaiConfig defaults() { return ChengduPdkRules.defaults(); }

    public Map<String,Object> authoritativeRules(Map<String,Object> published, int players) {
        if (!Set.of(2,3).contains(players)) throw new IllegalArgumentException("anyue2005 supports 2/3 players");
        Map<String,Object> rules = new LinkedHashMap<>(published);
        rules.put("cardsPerPlayer", 16); rules.put("deckMode", "CUT_40");
        rules.put("deckCards", ChengduPdkRules.cutDeck());
        rules.put("bankerSelectionCard", 105); rules.put("requiredFirstCard", 105);
        rules.put("requiredFirstCardRounds", 1); rules.put("selectBankerEveryRound", false);
        rules.put("operationTimeoutSeconds", integer(rules, "operationTimeoutSeconds", 10));
        MianningPdkRegionRules.normalizePayer(rules);
        Set<String> play = strings(rules.get("playRule"));
        rules.put("tripleWithoutAttachmentTiming", play.contains("three_no_attachment") ? "ANYTIME" : "FINAL_ONLY");
        rules.put("airplaneWithoutAttachmentTiming", play.contains("plane_no_attachment") ? "ANYTIME" : "FINAL_ONLY");
        rules.put("fourAttachmentMode", play.contains("four_with_two") ? "SINGLES" : "DISABLED");
        rules.put("specialTripleBombRanks", play.contains("triple_ace_bomb") ? List.of(14) : List.of());
        List<List<Integer>> direct = new ArrayList<>();
        if (play.contains("four_threes_direct_win")) direct.add(List.of(105,205,305,405));
        if (play.contains("triple_ace_plus_two_direct_win")) direct.add(List.of(114,214,314,115));
        rules.put("directWinPatterns", List.copyOf(direct));
        int bomb = integer(rules, "bombScore", 5);
        rules.put("bombScoreMode", "FIXED_POINTS"); rules.put("bombScoreCap", 0);
        rules.put("bombFixedPoints", bomb);
        rules.put("hostingMissThreshold", MianningPdkRegionRules.threshold(rules,
                ANYUE_HOSTING_MISS_THRESHOLD_FALLBACK));
        rules.put("settlementPresentation", MianningPdkRegionRules.selected(
                rules.get("rule_anyue2005_0001"), "option_0001") ? "POPUP" : "FLOATING");
        rules.putIfAbsent("firstLead", PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER.name());
        return Map.copyOf(rules);
    }

    public PokerRuleProfile profile(String version, Map<String,Object> rules, PaoDeKuaiConfig config) {
        return PdkPublishedRuleOptions.profile(version, rules, config,
                new PokerRuleProfile(version, 40, 2, 3,
                        PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER, 105, 5, 2,
                        false, false, true, true, 1, 16, 2, 16,
                        ChengduPdkRules.cutDeck()));
    }
    private static int integer(Map<String,Object> rules, String key, int fallback) {
        Object raw = rules.get(key); return raw instanceof Number number ? number.intValue() : fallback;
    }
    private static Set<String> strings(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) collection.forEach(value -> values.add(String.valueOf(value)));
        return Set.copyOf(values);
    }
}
