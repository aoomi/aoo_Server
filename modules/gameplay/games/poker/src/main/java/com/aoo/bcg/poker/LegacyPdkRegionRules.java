package com.aoo.bcg.poker;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One runtime implementation for all 22 equivalent legacy workbook families. */
final class LegacyPdkRegionRules implements PdkRegionRules {
    /** Five misses matches the existing published PDK normalization default. */
    static final int LEGACY_HOSTING_MISS_THRESHOLD_FALLBACK = 5;

    private final LegacyPdkRegionCatalog.Region region;

    LegacyPdkRegionRules(String gameCode) { region = LegacyPdkRegionCatalog.require(gameCode); }

    public String gameCode() { return region.gameCode(); }
    public String providerKey() { return "native-pdk-" + gameCode(); }
    public String defaultDeckMode() { return "STANDARD_48"; }
    public int defaultPlayers() { return integer(region.defaults(), "playerCount", 2); }
    public int defaultCardsPerPlayer() { return integer(region.defaults(), "dealCardCount", 15); }
    public PaoDeKuaiConfig defaults() { return ChengduPdkRules.defaults().withCardsPerPlayer(defaultCardsPerPlayer()); }

    public Map<String,Object> authoritativeRules(Map<String,Object> published, int players) {
        validatePublishedVocabulary(published);
        if (!allowed("playerCount").contains(players))
            throw new IllegalArgumentException(gameCode() + " does not support " + players + " players");
        Map<String,Object> rules = new LinkedHashMap<>(region.defaults());
        rules.putAll(published); // published/unknown fields retain Neijiang's put semantics
        int cards = integer(rules, "dealCardCount", defaultCardsPerPlayer());
        rules.put("cardsPerPlayer", cards);
        rules.put("deckMode", "STANDARD_48");
        rules.put("deckCards", ChengduPdkRules.standardDeck());
        rules.put("operationTimeoutSeconds", unlimited(integer(rules,
                "operationTimeoutSeconds", -1)));
        rules.put("offlineDissolveSeconds", unlimited(integer(rules,
                "dissolveTimeoutSeconds", 30)));
        normalizePayment(rules);
        normalizeRestrictions(rules);
        normalizePlay(rules);
        normalizeBombs(rules);
        normalizeAttachments(rules);
        rules.putIfAbsent("firstLead", PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER.name());
        return Map.copyOf(rules);
    }

    public PokerRuleProfile profile(String version, Map<String,Object> rules,
            PaoDeKuaiConfig config) {
        PokerRuleProfile base = new PokerRuleProfile(version, 48, 2,
                allowed("playerCount").contains(4) ? 4 : 3,
                PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER, null, 5, 2,
                false, false, true, true, 1, 16, 2, 16,
                ChengduPdkRules.standardDeck());
        return PdkPublishedRuleOptions.profile(version, rules, config, base);
    }

    private void validatePublishedVocabulary(Map<String,Object> published) {
        for (Map.Entry<String,Set<Object>> entry : region.allowed().entrySet()) {
            Object raw = published.get(entry.getKey());
            if (raw == null) continue;
            if (raw instanceof Collection<?> values) {
                if (!entry.getValue().containsAll(values))
                    throw new IllegalArgumentException("invalid " + gameCode() + " " + entry.getKey());
            } else if (!entry.getValue().contains(raw))
                throw new IllegalArgumentException("invalid " + gameCode() + " " + entry.getKey());
        }
    }

    private void normalizePayment(Map<String,Object> rules) {
        String payment = String.valueOf(rules.getOrDefault("paymentMode", "owner_pays"));
        PdkAdvancedRules.PayerMode payer = switch (payment) {
            case "owner_pays" -> PdkAdvancedRules.PayerMode.OWNER;
            case "split_pays" -> PdkAdvancedRules.PayerMode.SPLIT;
            case "winner_pays" -> PdkAdvancedRules.PayerMode.WINNER_PAYS;
            default -> throw new IllegalArgumentException("invalid " + gameCode() + " paymentMode");
        };
        rules.put("payerMode", payer.name());
    }

    private void normalizeRestrictions(Map<String,Object> rules) {
        Set<String> selected = strings(rules.get("roomRestriction"));
        rules.put("uniqueIpRequired", selected.contains("same_ip_blocked"));
        rules.put("gpsAdmissionRequired", selected.contains("same_location_blocked")
                || selected.contains("within_100m_blocked") || selected.contains("within_200m_blocked"));
        rules.put("gpsMinimumDistanceMeters", selected.contains("within_200m_blocked") ? 200
                : selected.contains("within_100m_blocked") ? 100 : 0);
        rules.put("textChatEnabled", !selected.contains("chat_forbidden"));
        rules.put("interactionEnabled", !selected.contains("items_forbidden"));
        rules.put("distanceWarningEnabled", selected.contains("same_location_blocked")
                || selected.contains("within_100m_blocked") || selected.contains("within_200m_blocked"));
        // 22 个族 A 地区的「其他」里没有 timeout_auto_play 可选项（开房规则表逐表实测），
        // 故此处保留公共归一化的 -1（托管关闭）；具名兜底只在勾选托管却缺字段时生效。
        int threshold = selected.contains("timeout_auto_play")
                ? positive(integer(rules, "hostingMissThreshold",
                        LEGACY_HOSTING_MISS_THRESHOLD_FALLBACK))
                : PdkAdvancedRules.HOSTING_MISS_THRESHOLD_DISABLED;
        rules.put("hostingMissThreshold", threshold);
    }

    private void normalizePlay(Map<String,Object> rules) {
        Set<String> play = strings(rules.get("playRule"));
        if (play.contains("must_beat") || play.contains("must_beat_with_big"))
            rules.put("mustBeatWhenPossible", true);
        if (play.contains("may_pass") || play.contains("not_required_beat"))
            rules.put("mustBeatWhenPossible", false);
        if (play.contains("triple_ace_bomb")) rules.put("specialTripleBombRanks", List.of(14));
        if (play.contains("four_with_three")) rules.put("allowFourWithThree", true);
        String first = firstRule(rules, play);
        if (first != null) {
            boolean every = first.startsWith("every_round_");
            int card = first.contains("heart_three") ? 203 : 103;
            rules.put("bankerSelectionCard", card);
            rules.put("requiredFirstCard", card);
            rules.put("requiredFirstCardRounds", every ? Integer.MAX_VALUE : 1);
            rules.put("selectBankerEveryRound", every);
        }
    }

    private void normalizeBombs(Map<String,Object> rules) {
        Set<String> play = strings(rules.get("playRule"));
        Object settlement = rules.get("bombSettlementMode");
        Object score = rules.get("bombScore");
        if (play.contains("no_bomb") || Integer.valueOf(0).equals(settlement)
                || Integer.valueOf(0).equals(score)) {
            bomb(rules, PdkAdvancedRules.BombMode.DISABLED, 0, 0);
            return;
        }
        int points = score instanceof Number number ? number.intValue() : 1;
        if ("winner_only".equals(settlement)) {
            bomb(rules, PdkAdvancedRules.BombMode.WINNER_ONLY, 0, points);
        } else if (score instanceof Number) {
            // A4 has no settlement selector: every server-observed bomb settles pairwise.
            bomb(rules, PdkAdvancedRules.BombMode.FIXED_POINTS, 0, points);
        } else if (region.variant() == LegacyPdkVariant.A5_WITHOUT_BOMB_SCORE) {
            // A5 has a settlement selector but no points field; one published score unit applies.
            bomb(rules, PdkAdvancedRules.BombMode.PAIRWISE, 0, 1);
        } else if ("double".equals(score)) {
            bomb(rules, PdkAdvancedRules.BombMode.MULTIPLIER, 1, 0);
        } else {
            bomb(rules, PdkAdvancedRules.BombMode.PAIRWISE, 0, points);
        }
    }

    private void normalizeAttachments(Map<String,Object> rules) {
        Set<String> four = strings(rules.get("fourWithMode"));
        if (four.contains("four_with_three")) rules.put("allowFourWithThree", true);
        if (four.contains("four_with_two")) rules.put("fourAttachmentMode", "SINGLES");
        else if (four.contains("four_with_one")) rules.put("allowFourBombWithOne", true);
        Set<String> triple = strings(rules.get("threeWithMode"));
        if (triple.contains("triple_with_pair")) rules.put("tripleAttachmentMode", "PAIRS");
        else if (triple.contains("triple_with_two")) rules.put("tripleAttachmentMode", "EITHER");
        else if (triple.contains("triple_with_one")) rules.put("tripleAttachmentMode", "SINGLES");
    }

    private static void bomb(Map<String,Object> rules, PdkAdvancedRules.BombMode mode,
            int cap, int points) {
        rules.put("bombScoreMode", mode.name());
        rules.put("bombScoreCap", cap);
        rules.put("bombFixedPoints", points);
    }

    private Set<Object> allowed(String key) { return region.allowed().getOrDefault(key, Set.of()); }
    private static int unlimited(int value) { return value < 0 ? 0 : value; }
    private static int positive(int value) { return value > 0 ? value : LEGACY_HOSTING_MISS_THRESHOLD_FALLBACK; }
    private static int integer(Map<String,Object> source, String key, int fallback) {
        Object value = source.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }
    private static String firstRule(Map<String,Object> rules, Set<String> play) {
        Object explicit = rules.get("firstPlayRule");
        if (explicit != null) return String.valueOf(explicit);
        explicit = rules.get("firstPlayMode");
        if (explicit != null) return String.valueOf(explicit);
        return play.stream().filter(value -> value.contains("round_")
                && (value.contains("three_first") || value.contains("three_required"))).findFirst().orElse(null);
    }
    private static Set<String> strings(Object raw) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (raw instanceof Collection<?> values) values.forEach(value -> result.add(String.valueOf(value)));
        else if (raw != null) result.add(String.valueOf(raw));
        return Set.copyOf(result);
    }
}
