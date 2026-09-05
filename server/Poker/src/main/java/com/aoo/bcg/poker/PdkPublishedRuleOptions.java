package com.aoo.bcg.poker;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Hall-validated immutable rule map into the common PDK policy model.
 * Display labels never enter this boundary; only versioned field keys and values are accepted.
 */
public final class PdkPublishedRuleOptions {
    private PdkPublishedRuleOptions() { }

    public static PaoDeKuaiConfig apply(Map<String,Object> rules, PaoDeKuaiConfig base) {
        Objects.requireNonNull(rules);
        Objects.requireNonNull(base);
        int straight = integer(rules, "minimumStraightLength", base.minimumStraightLength());
        int pairs = integer(rules, "minimumPairRunLength", base.minimumPairRunLength());
        PaoDeKuaiConfig.AttachmentMode triple = attachment(rules, "tripleAttachmentMode",
                base.tripleAttachmentMode());
        PaoDeKuaiConfig.AttachmentMode airplane = attachment(rules, "airplaneAttachmentMode",
                base.airplaneAttachmentMode());
        PaoDeKuaiConfig.AttachmentMode four = attachment(rules, "fourAttachmentMode",
                base.fourAttachmentMode());
        PaoDeKuaiConfig.PlayTiming tripleTiming = timing(rules, "tripleWithoutAttachmentTiming",
                base.tripleWithoutAttachmentTiming());
        PaoDeKuaiConfig.PlayTiming airplaneTiming = timing(rules,
                "airplaneWithoutAttachmentTiming", base.airplaneWithoutAttachmentTiming());
        return new PaoDeKuaiConfig(straight, allowsPairs(triple),
                four != PaoDeKuaiConfig.AttachmentMode.DISABLED, base.allowFourWithThree(),
                base.requiredFirstCard(), bool(rules, "forceHighestSingleAgainstReportedSingle",
                        base.forceHighestSingleAgainstReportedSingle()),
                base.specialTripleBombRank(), base.allowSpecialTripleBombWithOne(),
                base.allowFourBombWithOne(), base.standardBombTier(), base.specialBombTier(),
                base.fourBombWithOneTier(), base.allowTerminalAttachmentShortage(), pairs, triple,
                airplane, four, tripleTiming, airplaneTiming,
                bool(rules, "allowAirplaneWithTwo", base.allowAirplaneWithTwo()),
                bool(rules, "compareTripleAttachments", base.compareTripleAttachments()),
                bool(rules, "forceHighestPairAgainstReportedPair",
                        base.forceHighestPairAgainstReportedPair()),
                bool(rules, "allowSingle", base.allowSingle()),
                bool(rules, "allowPair", base.allowPair()),
                integer(rules, "cardsPerPlayer", base.cardsPerPlayer()),
                bool(rules, "allowConsecutiveBomb", base.allowConsecutiveBomb()),
                integers(rules, "specialTripleBombRanks", base.specialTripleBombRanks()),
                enumeration(rules, "playedCardVisibility", base.playedCardVisibility(),
                        PaoDeKuaiConfig.PlayedCardVisibility.class),
                advanced(rules, base.advancedRules()));
    }

    public static Map<String,Object> snapshot(PaoDeKuaiConfig config) {
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("minimumStraightLength", config.minimumStraightLength());
        values.put("minimumPairRunLength", config.minimumPairRunLength());
        values.put("tripleAttachmentMode", config.tripleAttachmentMode().name());
        values.put("airplaneAttachmentMode", config.airplaneAttachmentMode().name());
        values.put("fourAttachmentMode", config.fourAttachmentMode().name());
        values.put("tripleWithoutAttachmentTiming", config.tripleWithoutAttachmentTiming().name());
        values.put("airplaneWithoutAttachmentTiming", config.airplaneWithoutAttachmentTiming().name());
        values.put("allowAirplaneWithTwo", config.allowAirplaneWithTwo());
        values.put("compareTripleAttachments", config.compareTripleAttachments());
        values.put("forceHighestPairAgainstReportedPair",
                config.forceHighestPairAgainstReportedPair());
        values.put("forceHighestSingleAgainstReportedSingle",
                config.forceHighestSingleAgainstReportedSingle());
        values.put("allowSingle", config.allowSingle());
        values.put("allowPair", config.allowPair());
        values.put("cardsPerPlayer", config.cardsPerPlayer());
        values.put("allowConsecutiveBomb", config.allowConsecutiveBomb());
        values.put("specialTripleBombRanks", config.specialTripleBombRanks().stream().sorted().toList());
        values.put("playedCardVisibility", config.playedCardVisibility().name());
        snapshotAdvanced(values, config.advancedRules());
        return Map.copyOf(values);
    }

    public static PokerRuleProfile profile(String playVersion, Map<String,Object> rules,
            PaoDeKuaiConfig config, PokerRuleProfile base) {
        Object rawDeck = rules.get("deckCards");
        List<Integer> deck = base.deck();
        if (rawDeck != null) {
            if (!(rawDeck instanceof Collection<?> values))
                throw new IllegalArgumentException("deckCards must be an array");
            ArrayList<Integer> parsed = new ArrayList<>();
            for (Object value : values) {
                if (!(value instanceof Number number))
                    throw new IllegalArgumentException("deckCards must contain numbers");
                parsed.add(number.intValue());
            }
            deck = List.copyOf(parsed);
        }
        PokerRuleProfile.FirstLead firstLead = config.requiredFirstCard() == null
                ? base.firstLead() : PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER;
        return new PokerRuleProfile(playVersion, deck.size(), base.minimumPlayers(),
                base.maximumPlayers(), firstLead, config.requiredFirstCard(),
                config.minimumStraightLength(), config.minimumPairRunLength(),
                base.allowTwoInRuns(), base.allowJokersInRuns(),
                bool(rules, "mustBeatWhenPossible", base.mustBeatWhenPossible()),
                config.forceHighestSingleAgainstReportedSingle(), base.bombUnit(),
                base.multiplierCap(), base.minimumPlaneLength(), base.maximumHandSize(), deck);
    }

    public static Map<String,Object> snapshot(PaoDeKuaiConfig config, PokerRuleProfile profile) {
        Map<String,Object> values = new LinkedHashMap<>(snapshot(config));
        values.put("deckCards", profile.deck());
        values.put("mustBeatWhenPossible", profile.mustBeatWhenPossible());
        return Map.copyOf(values);
    }

    /**
     * 同一发布版本扩展人数范围后，旧 WAITING 快照仍保存旧 profile 指纹。
     * 只允许 Provider 明确登记的历史指纹迁移；已开局快照或未知指纹继续拒绝，
     * 避免把规则能力变化伪装成可恢复的配置升级。
     */
    public static Map<String,Object> migrateWaitingSnapshotIdentity(Map<String,Object> state,
            PaoDeKuaiFamily family, Set<String> knownLegacyKeys) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(family);
        Objects.requireNonNull(knownLegacyKeys);
        String current = String.valueOf(state.get("ruleSnapshotKey"));
        if (family.ruleSnapshotKey().equals(current)) return state;
        if (!knownLegacyKeys.contains(current) || !"WAITING".equals(state.get("state")))
            throw new IllegalStateException("rule snapshot mismatch");
        Map<String,Object> migrated = new LinkedHashMap<>(state);
        migrated.put("ruleSnapshotKey", family.ruleSnapshotKey());
        return Map.copyOf(migrated);
    }

    private static boolean allowsPairs(PaoDeKuaiConfig.AttachmentMode mode) {
        return mode == PaoDeKuaiConfig.AttachmentMode.PAIRS
                || mode == PaoDeKuaiConfig.AttachmentMode.EITHER;
    }

    private static PdkAdvancedRules advanced(Map<String,Object> rules,
            PdkAdvancedRules base) {
        PdkAdvancedRules.ScoreRule spring = scoreRule(rules, "spring", base.spring());
        PdkAdvancedRules.ScoreRule reverse = scoreRule(rules, "reverseSpring",
                base.reverseSpring());
        PdkAdvancedRules.BombMode bombMode = enumeration(rules, "bombScoreMode",
                base.bombScore().mode(), PdkAdvancedRules.BombMode.class);
        int bombCap = integer(rules, "bombScoreCap", base.bombScore().cap());
        int bombPoints = integer(rules, "bombFixedPoints", base.bombScore().points());
        if (bombMode == PdkAdvancedRules.BombMode.MULTIPLIER && bombCap == 0)
            bombCap = 1;
        if (bombMode == PdkAdvancedRules.BombMode.FIXED_POINTS && bombPoints == 0)
            throw new IllegalArgumentException("bombFixedPoints required for fixed bomb scoring");

        PdkAdvancedRules.DealerRule dealer = new PdkAdvancedRules.DealerRule(
                bool(rules, "competeDealerEnabled", base.dealerRule().enabled()),
                bool(rules, "competeDealerStartAfterBanker",
                        base.dealerRule().startAfterBanker()),
                bool(rules, "bankerCannotCompeteAfterAllPass",
                        base.dealerRule().bankerCannotCompeteAfterAllPass()),
                bool(rules, "skipCompeteDealerFirstRound",
                        base.dealerRule().skipFirstRound()));
        return new PdkAdvancedRules(
                integer(rules, "baseScore", base.baseScore()),
                integer(rules, "requiredFirstCardRounds", base.requiredFirstCardRounds()),
                nullableInteger(rules, "bankerSelectionCard", base.bankerSelectionCard()),
                bool(rules, "selectBankerEveryRound", base.selectBankerEveryRound()),
                spring, reverse, handScoreTable(rules, base.handScoreTable()),
                new PdkAdvancedRules.BombScore(bombMode, bombCap, bombPoints), dealer,
                enumerations(rules, "initialHandPatterns", base.initialPatterns(),
                        PdkAdvancedRules.InitialPattern.class),
                integers(rules, "fourOfKindPatternRanks", base.fourOfKindPatternRanks()),
                integer(rules, "initialHandPatternLimit", base.initialPatternLimit()),
                integer(rules, "initialHandPatternScoreUnit",
                        base.initialPatternScoreUnit()),
                integer(rules, "jinHuaScoreUnit", base.jinHuaScoreUnit()),
                cardPatterns(rules, "directWinPatterns", base.directWinPatterns()),
                integer(rules, "operationTimeoutSeconds", base.operationTimeoutSeconds()),
                integer(rules, "hostingMissThreshold", base.hostingMissThreshold()),
                governance(rules, base.governance()));
    }

    private static PdkAdvancedRules.RoomGovernance governance(Map<String,Object> rules,
            PdkAdvancedRules.RoomGovernance base) {
        return new PdkAdvancedRules.RoomGovernance(
                enumeration(rules, "payerMode", base.payerMode(),
                        PdkAdvancedRules.PayerMode.class),
                integer(rules, "offlineDissolveSeconds", base.offlineDissolveSeconds()),
                bool(rules, "autoReady", base.autoReady()),
                integer(rules, "declaredRoundTimeoutSeconds",
                        base.declaredRoundTimeoutSeconds()),
                bool(rules, "gpsAdmissionRequired", base.gpsAdmissionRequired()),
                integer(rules, "gpsMinimumDistanceMeters",
                        base.gpsMinimumDistanceMeters()),
                bool(rules, "uniqueIpRequired", base.uniqueIpRequired()),
                bool(rules, "textChatEnabled", base.textChatEnabled()),
                enumeration(rules, "settlementPresentation", base.settlementPresentation(),
                        PdkAdvancedRules.SettlementPresentation.class),
                enumeration(rules, "entryMode", base.entryMode(),
                        PdkAdvancedRules.EntryMode.class),
                new ArrayList<>(integers(rules, "carryInMultipliers",
                        new LinkedHashSet<>(base.carryInMultipliers()))),
                bool(rules, "distanceWarningEnabled", base.distanceWarningEnabled()),
                bool(rules, "interactionEnabled", base.interactionEnabled()));
    }

    private static PdkAdvancedRules.ScoreRule scoreRule(Map<String,Object> rules,
            String prefix, PdkAdvancedRules.ScoreRule base) {
        PdkAdvancedRules.ScoreMode mode = enumeration(rules, prefix + "Mode", base.mode(),
                PdkAdvancedRules.ScoreMode.class);
        int value = integer(rules, prefix + "Value", base.value());
        if (mode == PdkAdvancedRules.ScoreMode.DISABLED) value = 0;
        return new PdkAdvancedRules.ScoreRule(mode, value,
                integer(rules, prefix + "MaxPlayedCards", base.maxPlayedCards()),
                bool(rules, prefix + "AppliesToAllLosers", base.appliesToAllLosers()));
    }

    private static List<PdkAdvancedRules.HandScoreBand> handScoreTable(
            Map<String,Object> rules, List<PdkAdvancedRules.HandScoreBand> fallback) {
        Object raw = rules.get("handScoreTable");
        if (raw == null) return fallback;
        if (!(raw instanceof Collection<?> values) || values.size() % 2 != 0)
            throw new IllegalArgumentException("handScoreTable must contain threshold/score pairs");
        ArrayList<Integer> flat = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Number number))
                throw new IllegalArgumentException("handScoreTable must contain numbers");
            flat.add(number.intValue());
        }
        ArrayList<PdkAdvancedRules.HandScoreBand> bands = new ArrayList<>();
        for (int index = 0; index < flat.size(); index += 2)
            bands.add(new PdkAdvancedRules.HandScoreBand(flat.get(index), flat.get(index + 1)));
        return List.copyOf(bands);
    }

    private static List<Set<Integer>> cardPatterns(Map<String,Object> rules, String key,
            List<Set<Integer>> fallback) {
        Object raw = rules.get(key);
        if (raw == null) return fallback;
        if (!(raw instanceof Collection<?> patterns))
            throw new IllegalArgumentException(key + " must be an array of arrays");
        ArrayList<Set<Integer>> result = new ArrayList<>();
        for (Object pattern : patterns) {
            if (!(pattern instanceof Collection<?> cards))
                throw new IllegalArgumentException(key + " must be an array of arrays");
            LinkedHashSet<Integer> parsed = new LinkedHashSet<>();
            for (Object card : cards) {
                if (!(card instanceof Number number))
                    throw new IllegalArgumentException(key + " must contain numeric cards");
                if (!parsed.add(number.intValue()))
                    throw new IllegalArgumentException(key + " contains duplicate cards");
            }
            result.add(Set.copyOf(parsed));
        }
        return List.copyOf(result);
    }

    private static <T extends Enum<T>> Set<T> enumerations(Map<String,Object> rules, String key,
            Set<T> fallback, Class<T> type) {
        Object raw = rules.get(key);
        if (raw == null) return fallback;
        if (!(raw instanceof Collection<?> values))
            throw new IllegalArgumentException(key + " must be an array");
        LinkedHashSet<T> result = new LinkedHashSet<>();
        for (Object value : values) {
            T parsed;
            try {
                parsed = Enum.valueOf(type, String.valueOf(value).trim()
                        .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException badValue) {
                throw new IllegalArgumentException("invalid " + key, badValue);
            }
            if (!result.add(parsed)) throw new IllegalArgumentException(key + " has duplicates");
        }
        return Set.copyOf(result);
    }

    private static void snapshotAdvanced(Map<String,Object> values, PdkAdvancedRules advanced) {
        values.put("baseScore", advanced.baseScore());
        values.put("requiredFirstCardRounds", advanced.requiredFirstCardRounds());
        if (advanced.bankerSelectionCard() != null)
            values.put("bankerSelectionCard", advanced.bankerSelectionCard());
        values.put("selectBankerEveryRound", advanced.selectBankerEveryRound());
        snapshotScore(values, "spring", advanced.spring());
        snapshotScore(values, "reverseSpring", advanced.reverseSpring());
        ArrayList<Integer> table = new ArrayList<>();
        advanced.handScoreTable().forEach(band -> {
            table.add(band.minimumCards());
            table.add(band.score());
        });
        values.put("handScoreTable", List.copyOf(table));
        values.put("bombScoreMode", advanced.bombScore().mode().name());
        values.put("bombScoreCap", advanced.bombScore().cap());
        values.put("bombFixedPoints", advanced.bombScore().points());
        values.put("competeDealerEnabled", advanced.dealerRule().enabled());
        values.put("competeDealerStartAfterBanker", advanced.dealerRule().startAfterBanker());
        values.put("bankerCannotCompeteAfterAllPass",
                advanced.dealerRule().bankerCannotCompeteAfterAllPass());
        values.put("skipCompeteDealerFirstRound", advanced.dealerRule().skipFirstRound());
        values.put("initialHandPatterns", advanced.initialPatterns().stream()
                .map(Enum::name).sorted().toList());
        values.put("fourOfKindPatternRanks", advanced.fourOfKindPatternRanks().stream()
                .sorted().toList());
        values.put("initialHandPatternLimit", advanced.initialPatternLimit());
        values.put("initialHandPatternScoreUnit", advanced.initialPatternScoreUnit());
        values.put("jinHuaScoreUnit", advanced.jinHuaScoreUnit());
        values.put("directWinPatterns", advanced.directWinPatterns().stream()
                .map(pattern -> pattern.stream().sorted().toList()).toList());
        values.put("operationTimeoutSeconds", advanced.operationTimeoutSeconds());
        values.put("hostingMissThreshold", advanced.hostingMissThreshold());
        PdkAdvancedRules.RoomGovernance governance = advanced.governance();
        values.put("payerMode", governance.payerMode().name());
        values.put("offlineDissolveSeconds", governance.offlineDissolveSeconds());
        values.put("autoReady", governance.autoReady());
        values.put("declaredRoundTimeoutSeconds", governance.declaredRoundTimeoutSeconds());
        values.put("gpsAdmissionRequired", governance.gpsAdmissionRequired());
        values.put("gpsMinimumDistanceMeters", governance.gpsMinimumDistanceMeters());
        values.put("uniqueIpRequired", governance.uniqueIpRequired());
        values.put("textChatEnabled", governance.textChatEnabled());
        values.put("settlementPresentation", governance.settlementPresentation().name());
        values.put("entryMode", governance.entryMode().name());
        values.put("carryInMultipliers", governance.carryInMultipliers());
        values.put("distanceWarningEnabled", governance.distanceWarningEnabled());
        values.put("interactionEnabled", governance.interactionEnabled());
    }

    private static void snapshotScore(Map<String,Object> values, String prefix,
            PdkAdvancedRules.ScoreRule score) {
        values.put(prefix + "Mode", score.mode().name());
        values.put(prefix + "Value", score.value());
        values.put(prefix + "MaxPlayedCards", score.maxPlayedCards());
        values.put(prefix + "AppliesToAllLosers", score.appliesToAllLosers());
    }

    private static int integer(Map<String,Object> rules, String key, int fallback) {
        Object value = rules.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number))
            throw new IllegalArgumentException(key + " must be numeric");
        return number.intValue();
    }

    private static Integer nullableInteger(Map<String,Object> rules, String key,
            Integer fallback) {
        if (!rules.containsKey(key)) return fallback;
        Object value = rules.get(key);
        if (value == null) return null;
        if (!(value instanceof Number number))
            throw new IllegalArgumentException(key + " must be numeric or null");
        return number.intValue();
    }

    private static boolean bool(Map<String,Object> rules, String key, boolean fallback) {
        Object value = rules.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Boolean result))
            throw new IllegalArgumentException(key + " must be boolean");
        return result;
    }

    private static Set<Integer> integers(Map<String,Object> rules, String key, Set<Integer> fallback) {
        Object value = rules.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Collection<?> values))
            throw new IllegalArgumentException(key + " must be an array");
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        for (Object item : values) {
            if (!(item instanceof Number number))
                throw new IllegalArgumentException(key + " must contain numbers");
            if (!result.add(number.intValue()))
                throw new IllegalArgumentException(key + " contains duplicate values");
        }
        return Set.copyOf(result);
    }

    private static PaoDeKuaiConfig.AttachmentMode attachment(Map<String,Object> rules,
            String key, PaoDeKuaiConfig.AttachmentMode fallback) {
        return enumeration(rules, key, fallback, PaoDeKuaiConfig.AttachmentMode.class);
    }

    private static PaoDeKuaiConfig.PlayTiming timing(Map<String,Object> rules, String key,
            PaoDeKuaiConfig.PlayTiming fallback) {
        return enumeration(rules, key, fallback, PaoDeKuaiConfig.PlayTiming.class);
    }

    private static <T extends Enum<T>> T enumeration(Map<String,Object> rules, String key,
            T fallback, Class<T> type) {
        Object value = rules.get(key);
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, String.valueOf(value).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException badValue) {
            throw new IllegalArgumentException("invalid " + key, badValue);
        }
    }
}
