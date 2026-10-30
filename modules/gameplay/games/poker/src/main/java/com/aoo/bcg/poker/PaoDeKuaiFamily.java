package com.aoo.bcg.poker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PaoDeKuaiFamily implements PokerRuleFamily {
    public static final String CODE = "poker:pao-de-kuai";
    private final PaoDeKuaiRuleSet rules;
    private final PokerRuleProfile profile;
    private final boolean allowPassByRoomRule;

    public PaoDeKuaiFamily(PaoDeKuaiConfig config) {
        this(config, PokerRuleProfile.fromConfig("pdk-v1", 52, config), false);
    }

    public PaoDeKuaiFamily(PaoDeKuaiConfig config, PokerRuleProfile profile) {
        this(config, profile, false);
    }

    public PaoDeKuaiFamily(PaoDeKuaiConfig config, PokerRuleProfile profile,
            boolean allowPassByRoomRule) {
        this.profile = Objects.requireNonNull(profile);
        rules = new PaoDeKuaiRuleSet(config, profile);
        this.allowPassByRoomRule = allowPassByRoomRule;
    }

    public String familyCode() { return CODE; }
    public PokerRuleSet<?> ruleSet() { return rules; }
    public PaoDeKuaiRuleSet rules() { return rules; }
    public PokerRuleProfile profile() { return profile; }
    public boolean allowPassByRoomRule() { return allowPassByRoomRule; }

    /**
     * Durable recovery must not hash record {@code toString()}: nested set order is JVM-local.
     * This serializes every rule field with sorted maps and sets before hashing.
     */
    public String ruleSnapshotKey() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    canonical(snapshotIdentity()).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Verifies a pre-canonical fingerprint against the exact reconstructed profile and config.
     * The old identity omitted {@code allowPassByRoomRule}, so that unbound legacy value is never
     * migrated. Set order is exhaustively considered within a bounded candidate budget.
     */
    public boolean matchesLegacyRuleSnapshotKey(String persistedKey) {
        if (persistedKey == null || persistedKey.isBlank()) return false;
        if (persistedKey.equals(beforeDealerSpringCanonicalSnapshotKey())) return true;
        if (allowPassByRoomRule) return false;
        String profilePrefix = profile + "|";
        // DealerRule gained mustSpringToWin after persisted states already existed.  Rebuild
        // precisely the former record rendering only for the centralized legacy fingerprint
        // check; the restored state is then upgraded to the current immutable snapshot key.
        String baseline = beforeDealerSpringRule(rules.config().toString());
        List<List<String>> choices = List.of(
                setRenderings(rules.config().specialTripleBombRanks()),
                setRenderings(rules.config().advancedRules().initialPatterns()),
                setRenderings(rules.config().advancedRules().fourOfKindPatternRanks()),
                directPatternRenderings(rules.config().advancedRules().directWinPatterns()));
        long candidates = 1;
        for (List<String> choice : choices) {
            if (choice.isEmpty() || candidates > 100_000L / choice.size()) return false;
            candidates *= choice.size();
        }
        return matchesLegacyCandidate(persistedKey, profilePrefix, baseline, choices, 0);
    }

    private String beforeDealerSpringCanonicalSnapshotKey() {
        Map<String,Object> identity = snapshotIdentity();
        @SuppressWarnings("unchecked") Map<String,Object> config =
                (Map<String,Object>) identity.get("config");
        @SuppressWarnings("unchecked") Map<String,Object> advanced =
                (Map<String,Object>) config.get("advancedRules");
        PdkAdvancedRules.DealerRule dealer = rules.config().advancedRules().dealerRule();
        advanced.put("dealerRule", List.of(dealer.enabled(), dealer.startAfterBanker(),
                dealer.bankerCannotCompeteAfterAllPass(), dealer.skipFirstRound()));
        return sha256(canonical(identity));
    }

    private static boolean matchesLegacyCandidate(String persistedKey, String profilePrefix,
            String candidate, List<List<String>> choices, int index) {
        if (index == choices.size()) return persistedKey.equals(sha256(profilePrefix + candidate));
        for (String rendering : choices.get(index)) {
            String next = switch (index) {
                case 0 -> replaceField(candidate, "specialTripleBombRanks=",
                        ", playedCardVisibility=", rendering);
                case 1 -> replaceField(candidate, "initialPatterns=",
                        ", fourOfKindPatternRanks=", rendering);
                case 2 -> replaceField(candidate, "fourOfKindPatternRanks=",
                        ", initialPatternLimit=", rendering);
                case 3 -> replaceField(candidate, "directWinPatterns=",
                        ", operationTimeoutSeconds=", rendering);
                default -> throw new IllegalStateException("unknown legacy snapshot field");
            };
            if (matchesLegacyCandidate(persistedKey, profilePrefix, next, choices, index + 1))
                return true;
        }
        return false;
    }

    private static String replaceField(String value, String prefix, String suffix,
            String replacement) {
        int start = value.indexOf(prefix);
        if (start < 0) throw new IllegalStateException("legacy snapshot field missing: " + prefix);
        start += prefix.length();
        int end = value.indexOf(suffix, start);
        if (end < 0) throw new IllegalStateException("legacy snapshot field boundary missing: "
                + suffix);
        return value.substring(0, start) + replacement + value.substring(end);
    }

    private static String beforeDealerSpringRule(String value) {
        String marker = ", mustSpringToWin=";
        int start = value.indexOf(marker);
        if (start < 0) return value;
        int end = value.indexOf(']', start);
        if (end < 0) throw new IllegalStateException("dealer rule snapshot field boundary missing");
        return value.substring(0, start) + value.substring(end);
    }

    private static List<String> directPatternRenderings(List<Set<Integer>> patterns) {
        List<List<String>> choices = patterns.stream().map(PaoDeKuaiFamily::setRenderings).toList();
        long candidates = 1;
        for (List<String> choice : choices) {
            if (choice.isEmpty() || candidates > 100_000L / choice.size()) return List.of();
            candidates *= choice.size();
        }
        List<String> renderings = new ArrayList<>();
        renderDirectPatterns(choices, 0, new ArrayList<>(), renderings);
        return renderings;
    }

    private static void renderDirectPatterns(List<List<String>> choices, int index,
            List<String> selected, List<String> renderings) {
        if (index == choices.size()) {
            renderings.add('[' + String.join(", ", selected) + ']');
            return;
        }
        for (String choice : choices.get(index)) {
            selected.add(choice);
            renderDirectPatterns(choices, index + 1, selected, renderings);
            selected.removeLast();
        }
    }

    private static List<String> setRenderings(Set<?> values) {
        long permutations = 1;
        for (int size = 2; size <= values.size(); size++) {
            if (permutations > 100_000L / size) return List.of();
            permutations *= size;
        }
        List<String> members = values.stream().map(String::valueOf).toList();
        List<String> renderings = new ArrayList<>();
        permuteSetMembers(members, 0, renderings);
        return renderings;
    }

    private static void permuteSetMembers(List<String> members, int index,
            List<String> renderings) {
        if (index == members.size()) {
            renderings.add('[' + String.join(", ", members) + ']');
            return;
        }
        ArrayList<String> mutable = new ArrayList<>(members);
        for (int position = index; position < mutable.size(); position++) {
            java.util.Collections.swap(mutable, index, position);
            permuteSetMembers(mutable, index + 1, renderings);
            java.util.Collections.swap(mutable, index, position);
        }
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Map<String, Object> snapshotIdentity() {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("allowPassByRoomRule", allowPassByRoomRule);
        identity.put("profile", profileIdentity(profile));
        identity.put("config", configIdentity(rules.config()));
        return identity;
    }

    private static Map<String, Object> profileIdentity(PokerRuleProfile value) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("version", value.version());
        identity.put("deckSize", value.deckSize());
        identity.put("minimumPlayers", value.minimumPlayers());
        identity.put("maximumPlayers", value.maximumPlayers());
        identity.put("firstLead", value.firstLead().name());
        identity.put("requiredFirstCard", value.requiredFirstCard());
        identity.put("minimumStraightLength", value.minimumStraightLength());
        identity.put("minimumPairRunLength", value.minimumPairRunLength());
        identity.put("allowTwoInRuns", value.allowTwoInRuns());
        identity.put("allowJokersInRuns", value.allowJokersInRuns());
        identity.put("mustBeatWhenPossible", value.mustBeatWhenPossible());
        identity.put("forceHighestSingleAgainstReportedSingle",
                value.forceHighestSingleAgainstReportedSingle());
        identity.put("bombUnit", value.bombUnit());
        identity.put("multiplierCap", value.multiplierCap());
        identity.put("minimumPlaneLength", value.minimumPlaneLength());
        identity.put("maximumHandSize", value.maximumHandSize());
        identity.put("canonicalDeck", value.canonicalDeck());
        return identity;
    }

    private static Map<String, Object> configIdentity(PaoDeKuaiConfig value) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("minimumStraightLength", value.minimumStraightLength());
        identity.put("allowTripleWithPair", value.allowTripleWithPair());
        identity.put("allowFourWithTwo", value.allowFourWithTwo());
        identity.put("allowFourWithThree", value.allowFourWithThree());
        identity.put("requiredFirstCard", value.requiredFirstCard());
        identity.put("forceHighestSingleAgainstReportedSingle",
                value.forceHighestSingleAgainstReportedSingle());
        identity.put("specialTripleBombRank", value.specialTripleBombRank());
        identity.put("allowSpecialTripleBombWithOne", value.allowSpecialTripleBombWithOne());
        identity.put("allowFourBombWithOne", value.allowFourBombWithOne());
        identity.put("standardBombTier", value.standardBombTier());
        identity.put("specialBombTier", value.specialBombTier());
        identity.put("fourBombWithOneTier", value.fourBombWithOneTier());
        identity.put("allowTerminalAttachmentShortage", value.allowTerminalAttachmentShortage());
        identity.put("minimumPairRunLength", value.minimumPairRunLength());
        identity.put("tripleAttachmentMode", value.tripleAttachmentMode().name());
        identity.put("airplaneAttachmentMode", value.airplaneAttachmentMode().name());
        identity.put("fourAttachmentMode", value.fourAttachmentMode().name());
        identity.put("tripleWithoutAttachmentTiming", value.tripleWithoutAttachmentTiming().name());
        identity.put("airplaneWithoutAttachmentTiming", value.airplaneWithoutAttachmentTiming().name());
        identity.put("allowAirplaneWithTwo", value.allowAirplaneWithTwo());
        identity.put("compareTripleAttachments", value.compareTripleAttachments());
        identity.put("forceHighestPairAgainstReportedPair",
                value.forceHighestPairAgainstReportedPair());
        identity.put("allowSingle", value.allowSingle());
        identity.put("allowPair", value.allowPair());
        identity.put("cardsPerPlayer", value.cardsPerPlayer());
        identity.put("allowConsecutiveBomb", value.allowConsecutiveBomb());
        identity.put("specialTripleBombRanks", value.specialTripleBombRanks());
        identity.put("playedCardVisibility", value.playedCardVisibility().name());
        identity.put("advancedRules", advancedIdentity(value.advancedRules()));
        return identity;
    }

    private static Map<String, Object> advancedIdentity(PdkAdvancedRules value) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("baseScore", value.baseScore());
        identity.put("requiredFirstCardRounds", value.requiredFirstCardRounds());
        identity.put("bankerSelectionCard", value.bankerSelectionCard());
        identity.put("selectBankerEveryRound", value.selectBankerEveryRound());
        identity.put("spring", List.of(value.spring().mode().name(), value.spring().value(),
                value.spring().maxPlayedCards(), value.spring().appliesToAllLosers()));
        identity.put("reverseSpring", List.of(value.reverseSpring().mode().name(),
                value.reverseSpring().value(), value.reverseSpring().maxPlayedCards(),
                value.reverseSpring().appliesToAllLosers()));
        identity.put("handScoreTable", value.handScoreTable().stream()
                .map(band -> List.of(band.minimumCards(), band.score())).toList());
        identity.put("bombScore", List.of(value.bombScore().mode().name(),
                value.bombScore().cap(), value.bombScore().points()));
        identity.put("dealerRule", List.of(value.dealerRule().enabled(),
                value.dealerRule().startAfterBanker(),
                value.dealerRule().bankerCannotCompeteAfterAllPass(),
                value.dealerRule().skipFirstRound(),
                value.dealerRule().mustSpringToWin()));
        identity.put("initialPatterns", value.initialPatterns());
        identity.put("fourOfKindPatternRanks", value.fourOfKindPatternRanks());
        identity.put("initialPatternLimit", value.initialPatternLimit());
        identity.put("initialPatternScoreUnit", value.initialPatternScoreUnit());
        identity.put("jinHuaScoreUnit", value.jinHuaScoreUnit());
        identity.put("directWinPatterns", value.directWinPatterns());
        identity.put("operationTimeoutSeconds", value.operationTimeoutSeconds());
        identity.put("hostingMissThreshold", value.hostingMissThreshold());
        identity.put("governance", governanceIdentity(value.governance()));
        return identity;
    }

    private static Map<String, Object> governanceIdentity(PdkAdvancedRules.RoomGovernance value) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("payerMode", value.payerMode().name());
        identity.put("offlineDissolveSeconds", value.offlineDissolveSeconds());
        identity.put("autoReady", value.autoReady());
        identity.put("declaredRoundTimeoutSeconds", value.declaredRoundTimeoutSeconds());
        identity.put("gpsAdmissionRequired", value.gpsAdmissionRequired());
        identity.put("gpsMinimumDistanceMeters", value.gpsMinimumDistanceMeters());
        identity.put("uniqueIpRequired", value.uniqueIpRequired());
        identity.put("textChatEnabled", value.textChatEnabled());
        identity.put("settlementPresentation", value.settlementPresentation().name());
        identity.put("entryMode", value.entryMode().name());
        identity.put("carryInMultipliers", value.carryInMultipliers());
        identity.put("distanceWarningEnabled", value.distanceWarningEnabled());
        identity.put("interactionEnabled", value.interactionEnabled());
        return identity;
    }

    private static String canonical(Object value) {
        if (value == null) return "null";
        if (value instanceof Enum<?> enumValue) return "enum:" + enumValue.name();
        if (value instanceof String string) return "string:" + string.length() + ':' + string;
        if (value instanceof Number || value instanceof Boolean)
            return value.getClass().getName() + ':' + value;
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            map.forEach((key, nested) -> entries.add(canonical(key) + '=' + canonical(nested)));
            entries.sort(Comparator.naturalOrder());
            return "map:[" + String.join(",", entries) + ']';
        }
        if (value instanceof Set<?> set) {
            List<String> members = set.stream().map(PaoDeKuaiFamily::canonical).sorted().toList();
            return "set:[" + String.join(",", members) + ']';
        }
        if (value instanceof Collection<?> collection) {
            List<String> members = collection.stream().map(PaoDeKuaiFamily::canonical).toList();
            return "list:[" + String.join(",", members) + ']';
        }
        throw new IllegalArgumentException("unsupported rule snapshot identity value: "
                + value.getClass().getName());
    }
}
