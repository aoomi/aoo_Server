package com.aoo.bcg.poker;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Hall-validated immutable rule map into the common PDK policy model.
 * Display labels never enter this boundary; only versioned field keys and values are accepted.
 */
public final class PdkPublishedRuleOptions {
    private static final Set<String> SMALL_SETTLEMENT_FIELDS = Set.of(
            "rule_cd201_0001", "rule_nj201_0004", "rule_ls201_0001");
    private static final String SMALL_SETTLEMENT_POPUP_OPTION = "option_0001";
    private static final Set<String> PRESENTATION_ONLY_PLAY_RULE_OPTIONS = Set.of(
            SMALL_SETTLEMENT_POPUP_OPTION);
    private PdkPublishedRuleOptions() { }

    public static PaoDeKuaiConfig apply(Map<String,Object> rules, PaoDeKuaiConfig base) {
        Objects.requireNonNull(rules);
        Objects.requireNonNull(base);
        rules = normalizePublishedRoomFields(rules);
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
        Integer requiredFirstCard = nullableInteger(rules, "requiredFirstCard",
                base.requiredFirstCard());
        Set<Integer> specialBombRanks = integers(rules, "specialTripleBombRanks",
                base.specialTripleBombRanks());
        Integer compatibilityBombRank = specialBombRanks.size() == 1
                ? specialBombRanks.iterator().next() : null;
        return new PaoDeKuaiConfig(straight, allowsPairs(triple),
                four != PaoDeKuaiConfig.AttachmentMode.DISABLED,
                bool(rules, "allowFourWithThree", base.allowFourWithThree()),
                requiredFirstCard, bool(rules, "forceHighestSingleAgainstReportedSingle",
                        base.forceHighestSingleAgainstReportedSingle()),
                compatibilityBombRank, bool(rules, "allowSpecialTripleBombWithOne",
                        base.allowSpecialTripleBombWithOne()),
                bool(rules, "allowFourBombWithOne", base.allowFourBombWithOne()),
                base.standardBombTier(), base.specialBombTier(), base.fourBombWithOneTier(),
                bool(rules, "allowTerminalAttachmentShortage",
                        base.allowTerminalAttachmentShortage()), pairs, triple,
                airplane, four, tripleTiming, airplaneTiming,
                bool(rules, "allowAirplaneWithTwo", base.allowAirplaneWithTwo()),
                bool(rules, "compareTripleAttachments", base.compareTripleAttachments()),
                bool(rules, "forceHighestPairAgainstReportedPair",
                        base.forceHighestPairAgainstReportedPair()),
                bool(rules, "allowSingle", base.allowSingle()),
                bool(rules, "allowPair", base.allowPair()),
                integer(rules, "cardsPerPlayer", base.cardsPerPlayer()),
                bool(rules, "allowConsecutiveBomb", base.allowConsecutiveBomb()),
                specialBombRanks,
                enumeration(rules, "playedCardVisibility", base.playedCardVisibility(),
                        PaoDeKuaiConfig.PlayedCardVisibility.class),
                advanced(rules, base.advancedRules()));
    }

    /**
     * Expands compact Hall fields and removes schema-generated presentation values before
     * regional validation consumes a room payload. Rule capabilities retain their published
     * stable values throughout.
     */
    public static Map<String,Object> normalizePublishedRoomFields(Map<String,Object> source) {
        Map<String,Object> rules = new LinkedHashMap<>(source);
        normalizeSettlementPresentation(source, rules);
        normalizePresentationOnlyPlayOptions(rules);
        if (source.containsKey("bombScore") && !source.containsKey("bombFixedPoints")) {
            rules.put("bombScoreMode", PdkAdvancedRules.BombMode.FIXED_POINTS.name());
            rules.put("bombFixedPoints", source.get("bombScore"));
        }
        if (source.containsKey("firstPlayRule") && !source.containsKey("bankerSelectionCard")) {
            if ("spade_three_first".equals(String.valueOf(source.get("firstPlayRule"))))
                rules.put("bankerSelectionCard", 103);
        }
        if (source.containsKey("playRule")) {
            Set<String> selected = strings(source.get("playRule"));
            boolean anytime = selected.contains("three_no_attachment");
            rules.putIfAbsent("tripleWithoutAttachmentTiming", (anytime
                    ? PaoDeKuaiConfig.PlayTiming.ANYTIME
                    : PaoDeKuaiConfig.PlayTiming.FINAL_ONLY).name());
            rules.putIfAbsent("airplaneWithoutAttachmentTiming", (anytime
                    ? PaoDeKuaiConfig.PlayTiming.ANYTIME
                    : PaoDeKuaiConfig.PlayTiming.FINAL_ONLY).name());
            rules.putIfAbsent("fourAttachmentMode", (selected.contains("four_with_two")
                    ? PaoDeKuaiConfig.AttachmentMode.SINGLES
                    : PaoDeKuaiConfig.AttachmentMode.DISABLED).name());
            rules.putIfAbsent("specialTripleBombRanks",
                    selected.contains("triple_ace_bomb") ? List.of(14) : List.of());
            if (selected.contains("require_spade_three")) {
                rules.putIfAbsent("requiredFirstCard", 103);
                rules.putIfAbsent("requiredFirstCardRounds", 99999);
            }
            rules.putIfAbsent("directWinPatterns", selected.contains("four_threes_direct_win")
                    ? List.of(List.of(103, 203, 303, 403)) : List.of());
            rules.putIfAbsent("compareTripleAttachments",
                    selected.contains("compare_attachments"));
        }
        if (source.containsKey("attachmentComparison"))
            rules.put("compareTripleAttachments",
                    "compare".equals(String.valueOf(source.get("attachmentComparison"))));
        if (source.containsKey("roomRestriction")) {
            Set<String> selected = strings(source.get("roomRestriction"));
            rules.putIfAbsent("uniqueIpRequired", selected.contains("ip_limit"));
            rules.putIfAbsent("gpsAdmissionRequired", selected.contains("gps_limit"));
            rules.putIfAbsent("hostingMissThreshold",
                    selected.contains("timeout_auto_play") ? 5 : -1);
            rules.putIfAbsent("distanceWarningEnabled", selected.contains("distance_warning"));
            rules.putIfAbsent("interactionEnabled", !selected.contains("interaction_forbidden"));
            rules.putIfAbsent("textChatEnabled", !selected.contains("chat_muted"));
        }
        return Map.copyOf(rules);
    }

    private static void normalizePresentationOnlyPlayOptions(Map<String,Object> rules) {
        if (!rules.containsKey("playRule")) return;
        LinkedHashSet<String> selected = new LinkedHashSet<>(strings(rules.get("playRule")));
        selected.removeAll(PRESENTATION_ONLY_PLAY_RULE_OPTIONS);
        rules.put("playRule", List.copyOf(selected));
    }

    /**
     * The workbook's one-option checkbox is the source of truth for small-settlement
     * presentation.  The Hall keeps its generated field ids so the mapping lives once at
     * the immutable PDK boundary; downstream rules only receive the stable enum contract.
     */
    private static void normalizeSettlementPresentation(
            Map<String,Object> source, Map<String,Object> rules) {
        if (source.containsKey("settlementPresentation")) return;
        for (String field : SMALL_SETTLEMENT_FIELDS)
            if (source.containsKey(field)) {
                rules.put(
                        "settlementPresentation",
                        strings(source.get(field)).contains(SMALL_SETTLEMENT_POPUP_OPTION)
                                ? PdkAdvancedRules.SettlementPresentation.POPUP.name()
                                : PdkAdvancedRules.SettlementPresentation.FLOATING.name());
                return;
            }
    }

    private static Set<String> strings(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection)
            collection.forEach(value -> values.add(String.valueOf(value)));
        else if (raw != null) values.add(String.valueOf(raw));
        return Collections.unmodifiableSet(values);
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
        if (config.requiredFirstCard() != null)
            values.put("requiredFirstCard", config.requiredFirstCard());
        values.put("allowFourWithThree", config.allowFourWithThree());
        values.put("allowSpecialTripleBombWithOne", config.allowSpecialTripleBombWithOne());
        values.put("allowFourBombWithOne", config.allowFourBombWithOne());
        values.put("allowTerminalAttachmentShortage", config.allowTerminalAttachmentShortage());
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
        Integer requiredFirstCard = config.requiredFirstCard() != null
                && deck.contains(config.requiredFirstCard()) ? config.requiredFirstCard() : null;
        PokerRuleProfile.FirstLead firstLead = rules.containsKey("firstLead")
                ? enumeration(rules, "firstLead", base.firstLead(),
                        PokerRuleProfile.FirstLead.class)
                : requiredFirstCard == null ? base.firstLead()
                        : PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER;
        if (firstLead == PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER
                && requiredFirstCard == null)
            firstLead = base.firstLead();
        return new PokerRuleProfile(playVersion, deck.size(), base.minimumPlayers(),
                base.maximumPlayers(), firstLead, requiredFirstCard,
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
        values.put("allowTwoInRuns", profile.allowTwoInRuns());
        values.put("allowJokersInRuns", profile.allowJokersInRuns());
        values.put("firstLead", profile.firstLead().name());
        return Map.copyOf(values);
    }

    public static Map<String,Object> snapshot(PaoDeKuaiConfig config, PokerRuleProfile profile,
            boolean allowPassByRoomRule) {
        Map<String,Object> values = new LinkedHashMap<>(snapshot(config, profile));
        values.put("allowPassByRoomRule", allowPassByRoomRule);
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

    /**
     * Converts a legacy key only after the old record-based hash can be recomputed from the
     * persisted profile/config semantics. Unknown or semantically different keys stay rejected.
     */
    public static Map<String,Object> migrateVerifiedLegacySnapshotIdentity(
            Map<String,Object> state, PaoDeKuaiFamily family) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(family);
        String persisted = String.valueOf(state.get("ruleSnapshotKey"));
        if (family.ruleSnapshotKey().equals(persisted)) return state;
        if (!family.profile().version().equals(String.valueOf(state.get("ruleVersion")))
                || !family.matchesLegacyRuleSnapshotKey(persisted))
            return state;
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
                        base.dealerRule().skipFirstRound()),
                bool(rules, "competeDealerMustSpringToWin",
                        base.dealerRule().mustSpringToWin()));
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
        values.put("competeDealerMustSpringToWin", advanced.dealerRule().mustSpringToWin());
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
