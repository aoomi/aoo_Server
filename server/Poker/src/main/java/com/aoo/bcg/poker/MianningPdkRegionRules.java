package com.aoo.bcg.poker;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** MN201 published differences over the common authoritative PDK engine. */
final class MianningPdkRegionRules implements PdkRegionRules {
    private static final int MIANNING_HOSTING_MISS_THRESHOLD_FALLBACK = 3;
    public String gameCode() { return PdkBusinessCodes.MIANNING; }
    public String providerKey() { return "native-pdk-MN201"; }
    public String defaultDeckMode() { return "STANDARD_48"; }
    public int defaultPlayers() { return 2; }
    public int defaultCardsPerPlayer() { return 13; }
    public PaoDeKuaiConfig defaults() { return ChengduPdkRules.defaults().withCardsPerPlayer(13); }

    public Map<String,Object> authoritativeRules(Map<String,Object> published, int players) {
        if (!Set.of(2,3).contains(players)) throw new IllegalArgumentException("MN201 supports 2/3 players");
        Map<String,Object> rules = new LinkedHashMap<>(published);
        rules.put("cardsPerPlayer", 13); rules.put("deckMode", "STANDARD_48");
        rules.put("deckCards", ChengduPdkRules.standardDeck());
        rules.put("operationTimeoutSeconds", 0);
        normalizePayer(rules);
        String dealer = String.valueOf(rules.getOrDefault("robDealerRule", "rob_dealer"));
        rules.put("competeDealerEnabled", true);
        rules.put("skipCompeteDealerFirstRound", "first_round_no_compete".equals(dealer));
        rules.put("bankerCannotCompeteAfterAllPass",
                "dealer_cannot_rob_when_idle_pass".equals(dealer));
        Object score = rules.getOrDefault("bombScore", "no_multiplier");
        if ("no_multiplier".equals(score)) bomb(rules, PdkAdvancedRules.BombMode.DISABLED, 0);
        else if ("unlimited_multiplier".equals(score)) bomb(rules, PdkAdvancedRules.BombMode.MULTIPLIER, 30);
        else if (score instanceof Number number && number.intValue() == 1)
            bomb(rules, PdkAdvancedRules.BombMode.MULTIPLIER, 1);
        else throw new IllegalArgumentException("invalid MN201 bombScore");
        rules.put("hostingMissThreshold", threshold(rules, MIANNING_HOSTING_MISS_THRESHOLD_FALLBACK));
        rules.put("settlementPresentation", selected(rules.get("rule_mn201_0001"), "option_0001")
                ? "POPUP" : "FLOATING");
        rules.putIfAbsent("firstLead", PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER.name());
        return Map.copyOf(rules);
    }

    public PokerRuleProfile profile(String version, Map<String,Object> rules, PaoDeKuaiConfig config) {
        return PdkPublishedRuleOptions.profile(version, rules, config,
                new PokerRuleProfile(version, 48, 2, 3,
                        PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER, null, 5, 2,
                        false, false, true, true, 1, 16, 2, 13,
                        ChengduPdkRules.standardDeck()));
    }
    private static void bomb(Map<String,Object> rules, PdkAdvancedRules.BombMode mode, int cap) {
        rules.put("bombScoreMode", mode.name()); rules.put("bombScoreCap", cap);
        rules.put("bombFixedPoints", 0);
    }
    static int threshold(Map<String,Object> rules, int fallback) {
        // 真门控：未勾选 timeout_auto_play ⇒ 托管关闭（-1）；勾选但表内无该字段时用族兜底常量。
        if (!selected(rules.get("roomRestriction"), "timeout_auto_play"))
            return PdkAdvancedRules.HOSTING_MISS_THRESHOLD_DISABLED;
        Object raw = rules.get("hostingMissThreshold");
        return raw instanceof Number number && number.intValue() > 0
                ? number.intValue() : fallback;
    }
    static void normalizePayer(Map<String,Object> rules) {
        if (!rules.containsKey("paymentMode")) return;
        String payer = switch (String.valueOf(rules.get("paymentMode"))) {
            case "owner_pays" -> "OWNER"; case "split_pays" -> "SPLIT";
            case "winner_pays" -> "WINNER_PAYS";
            default -> throw new IllegalArgumentException("invalid paymentMode");
        };
        rules.put("payerMode", payer);
    }
    static boolean selected(Object raw, String value) {
        return raw instanceof Collection<?> values ? values.contains(value) : value.equals(raw);
    }
}
