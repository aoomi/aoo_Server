package com.aoo.bcg.poker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 内江跑得快：XQP ScNj 的地区配置，复用公共 PDK 状态机。 */
final class NeijiangPdkRules implements PdkRegionRules {
    private static final Set<String> PLAY_RULES = Set.of("three_no_attachment",
            "four_with_two", "triple_ace_bomb", "remove_three_four",
            "require_spade_three");
    private static final Set<String> ROOM_RESTRICTIONS = Set.of("ip_limit", "gps_limit",
            "timeout_auto_play", "distance_warning", "interaction_forbidden", "chat_muted");
    public String gameCode() { return PdkBusinessCodes.NEIJIANG; }
    public String providerKey() { return "native-pdk-NJ201"; }
    public String defaultDeckMode() { return "CUT_40"; }
    public int defaultPlayers() { return 2; }
    public int defaultCardsPerPlayer() { return 16; }
    public PaoDeKuaiConfig defaults() { return ChengduPdkRules.defaults(); }

    public Map<String,Object> authoritativeRules(Map<String,Object> published, int players) {
        if (players < 2 || players > 3)
            throw new IllegalArgumentException("Neijiang PDK supports two or three players");
        Map<String,Object> rules = new LinkedHashMap<>(published);
        PdkRegionalRuleValidation.integerChoice(published, "roundCount", 8,
                Set.of(8, 12, 16));
        int timeout = PdkRegionalRuleValidation.integerRange(published, "operationTime", 10,
                1, 3600);
        int bombScore = PdkRegionalRuleValidation.integerChoice(published, "bombScore", 5,
                Set.of(5, 10, 20));
        String first = PdkRegionalRuleValidation.stringChoice(published, "firstPlayRule",
                "winner_first", Set.of("winner_first", "spade_three_first"));
        Set<String> playRules = PdkRegionalRuleValidation.stringChoices(published, "playRule",
                Set.of("three_no_attachment", "four_with_two", "triple_ace_bomb",
                        "remove_three_four"), PLAY_RULES);
        PdkRegionalRuleValidation.stringChoices(published, "roomRestriction", Set.of(),
                ROOM_RESTRICTIONS);
        boolean cut = players == 2 && playRules.contains("remove_three_four");
        rules.put("cardsPerPlayer", 16);
        rules.put("deckMode", cut ? "CUT_40" : "STANDARD_48");
        rules.put("deckCards", cut ? ChengduPdkRules.cutDeck() : ChengduPdkRules.standardDeck());
        rules.put("operationTimeoutSeconds", timeout);
        rules.put("bombScore", bombScore);
        rules.put("selectBankerEveryRound", "spade_three_first".equals(first));
        rules.putIfAbsent("firstLead", PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER.name());
        return Map.copyOf(rules);
    }

    public PokerRuleProfile profile(String version, Map<String,Object> rules,
            PaoDeKuaiConfig config) {
        boolean cut = "CUT_40".equals(String.valueOf(rules.get("deckMode")));
        PokerRuleProfile base = new PokerRuleProfile(version, cut ? 40 : 48, 2, 3,
                PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER, null, 5, 2,
                false, false, true, true, 1, 16, 2, 16,
                cut ? ChengduPdkRules.cutDeck() : ChengduPdkRules.standardDeck());
        return PdkPublishedRuleOptions.profile(version, rules, config, base);
    }
}
