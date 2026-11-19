package com.aoo.bcg.poker;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 攀枝花 5-A/10-card regional policy over the common PDK engine. */
final class PanzhihuaPdkRegionRules implements PdkRegionRules {
    private static final int PANZHIHUA_HOSTING_MISS_THRESHOLD_FALLBACK = 3;
    private static final List<Integer> DECK_5_TO_ACE = List.of(
            105,106,107,108,109,110,111,112,113,114,
            205,206,207,208,209,210,211,212,213,214,
            305,306,307,308,309,310,311,312,313,314,
            405,406,407,408,409,410,411,412,413,414);
    public String gameCode() { return PdkBusinessCodes.PANZHIHUA; }
    public String providerKey() { return "native-pdk-" + PdkBusinessCodes.PANZHIHUA; }
    public String defaultDeckMode() { return "PZ_5_TO_ACE"; }
    public int defaultPlayers() { return 2; }
    public int defaultCardsPerPlayer() { return 10; }
    public PaoDeKuaiConfig defaults() { return ChengduPdkRules.defaults().withCardsPerPlayer(10); }

    public Map<String,Object> authoritativeRules(Map<String,Object> published, int players) {
        if (!Set.of(2,3,4).contains(players)) throw new IllegalArgumentException("panzhihua2006 supports 2/3/4 players");
        Map<String,Object> rules = new LinkedHashMap<>(published);
        rules.put("cardsPerPlayer", 10); rules.put("deckMode", "PZ_5_TO_ACE");
        rules.put("deckCards", DECK_5_TO_ACE); rules.put("bankerSelectionCard", 105);
        rules.put("requiredFirstCard", 105); rules.put("requiredFirstCardRounds", 1);
        rules.put("selectBankerEveryRound", false);
        MianningPdkRegionRules.normalizePayer(rules);
        int bomb = integer(rules, "bombScore", 10);
        rules.put("bombScoreMode", "FIXED_POINTS"); rules.put("bombScoreCap", 0);
        rules.put("bombFixedPoints", bomb);
        rules.put("initialHandPatternScoreUnit", integer(rules, "specialPatternScore", 5));
        rules.put("initialHandPatternLimit", Integer.MAX_VALUE);
        Set<String> play = strings(rules.get("playRule"));
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        if (play.contains("all_singles")) patterns.add("ALL_SINGLES");
        if (play.contains("all_pairs")) patterns.add("ALL_PAIRS");
        if (play.contains("all_black")) patterns.add("ALL_BLACK");
        if (play.contains("all_red")) patterns.add("ALL_RED");
        if (play.contains("all_big")) patterns.add("ALL_BIG");
        if (play.contains("all_small")) patterns.add("ALL_SMALL");
        rules.put("initialHandPatterns", List.copyOf(patterns));
        rules.put("specialTripleBombRanks", play.contains("triple_five_to_ace_bomb")
                ? List.of(5,6,7,8,9,10,11,12,13,14) : List.of());
        rules.put("hostingMissThreshold", MianningPdkRegionRules.threshold(rules,
                PANZHIHUA_HOSTING_MISS_THRESHOLD_FALLBACK));
        rules.put("settlementPresentation", MianningPdkRegionRules.selected(
                rules.get("rule_panzhihua2006_0001"), "option_0001") ? "POPUP" : "FLOATING");
        rules.putIfAbsent("firstLead", PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER.name());
        return Map.copyOf(rules);
    }

    public PokerRuleProfile profile(String version, Map<String,Object> rules, PaoDeKuaiConfig config) {
        return PdkPublishedRuleOptions.profile(version, rules, config,
                new PokerRuleProfile(version, 40, 2, 4,
                        PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER, 105, 5, 2,
                        false, false, true, true, 1, 16, 2, 10, DECK_5_TO_ACE));
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
