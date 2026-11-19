package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Configurable Paodekuai combinations shared by all regional variants. */
public final class PaoDeKuaiRuleSet implements PokerRuleSet<PaoDeKuaiContext> {
    private final PaoDeKuaiConfig config;
    private final PokerRuleProfile profile;
    private final StandardPokerRuleSet<PaoDeKuaiContext> basic = new StandardPokerRuleSet<>();

    public PaoDeKuaiRuleSet(PaoDeKuaiConfig config) {
        this(config, PokerRuleProfile.fromConfig("pdk-v1", 52, config));
    }

    public PaoDeKuaiRuleSet(PaoDeKuaiConfig config, PokerRuleProfile profile) {
        this.config = Objects.requireNonNull(config);
        this.profile = Objects.requireNonNull(profile);
    }

    public PaoDeKuaiConfig config() { return config; }

    public CardCombination recognize(List<Integer> cards, PaoDeKuaiContext context) {
        Map<Integer, Long> counts = new TreeMap<>();
        cards.forEach(card -> counts.merge(StandardPokerRuleSet.rank(card), 1L, Long::sum));
        int size = cards.size();
        Integer special = counts.entrySet().stream()
                .filter(entry -> config.specialTripleBombRanks().contains(entry.getKey())
                        && entry.getValue() == 3)
                .map(Map.Entry::getKey).findFirst().orElse(null);
        if (special != null) {
            if (size == 3) return new CardCombination("SPECIAL_TRIPLE_BOMB", special, cards);
            if (size == 4 && config.allowSpecialTripleBombWithOne()) {
                return new CardCombination("SPECIAL_TRIPLE_BOMB_WITH_ONE", special, cards);
            }
        }
        if (config.allowConsecutiveBomb() && size >= 8 && size % 4 == 0
                && counts.values().stream().allMatch(value -> value == 4)
                && legalRun(counts.keySet(), 2)) {
            return new CardCombination("CONSECUTIVE_BOMB",
                    counts.keySet().stream().max(Integer::compareTo).orElseThrow(), cards);
        }
        if (size == 5 && config.allowFourBombWithOne() && counts.containsValue(4L)) {
            int rank = counts.entrySet().stream().filter(entry -> entry.getValue() == 4)
                    .findFirst().orElseThrow().getKey();
            return new CardCombination("FOUR_BOMB_WITH_ONE", rank, cards);
        }
        if (size >= config.minimumStraightLength() && counts.size() == size
                && legalRun(counts.keySet(), config.minimumStraightLength())) {
            return new CardCombination("STRAIGHT",
                    counts.keySet().stream().max(Integer::compareTo).orElseThrow(), cards);
        }
        try {
            CardCombination basicCombination = basic.recognize(cards, context);
            if ("SINGLE".equals(basicCombination.type()) && !config.allowSingle()) {
                throw new IllegalArgumentException("single is disabled");
            }
            if ("PAIR".equals(basicCombination.type()) && !config.allowPair()) {
                throw new IllegalArgumentException("pair is disabled");
            }
            if ("TRIPLE".equals(basicCombination.type())
                    && !timingAllows(config.tripleWithoutAttachmentTiming(), cards, context)) {
                throw new IllegalArgumentException("triple without attachment is disabled for this turn");
            }
            if ("STRAIGHT".equals(basicCombination.type())) {
                profile.validateRun(basicCombination.cards().stream()
                        .map(StandardPokerRuleSet::rank).distinct().sorted().toList(), false);
            }
            return basicCombination;
        } catch (IllegalArgumentException ignored) {
            // Basic parser rejection intentionally falls through to regional combinations.
        }
        String type = null;
        int primary = counts.keySet().stream().max(Integer::compareTo).orElseThrow();
        if (size == 4 && counts.containsValue(3L) && allowsSingles(config.tripleAttachmentMode())) {
            type = "TRIPLE_WITH_ONE";
            primary = rankWithCount(counts, 3);
        } else if (size == 5 && frequency(counts, 3) == 1
                && allowsPairs(config.tripleAttachmentMode()) && frequency(counts, 2) == 1) {
            type = "TRIPLE_WITH_PAIR";
            primary = rankWithCount(counts, 3);
        } else if (size == 5 && counts.values().stream().anyMatch(value -> value >= 3)
                && allowsTwoSingles(config.tripleAttachmentMode())) {
            type = "TRIPLE_WITH_TWO";
            primary = rankWithAtLeastCount(counts, 3);
        } else if (size >= 4 && size % 2 == 0 && counts.values().stream().allMatch(value -> value == 2)
                && validProfileRun(counts.keySet(), true)) {
            type = "CONSECUTIVE_PAIRS";
        } else {
            int bodyLength = size % 3 == 0 ? size / 3 : 0;
            List<Integer> body = consecutiveTripleBody(counts, bodyLength);
            if (!body.isEmpty() && timingAllows(config.airplaneWithoutAttachmentTiming(), cards, context)) {
                type = "AIRPLANE";
                primary = body.get(body.size() - 1);
            }
            if (type == null && size % 4 == 0 && allowsSingles(config.airplaneAttachmentMode())) {
                bodyLength = size / 4;
                body = consecutiveTripleBody(counts, bodyLength);
                if (!body.isEmpty()) {
                    type = "AIRPLANE_WITH_SINGLES";
                    primary = body.get(body.size() - 1);
                }
            }
            if (type == null && size % 5 == 0) {
                bodyLength = size / 5;
                body = consecutiveTripleBody(counts, bodyLength);
                if (!body.isEmpty()) {
                    Map<Integer, Long> attachments = airplaneAttachments(counts, body);
                    if (allowsPairs(config.airplaneAttachmentMode())
                            && attachments.values().stream().allMatch(value -> value == 2)) {
                        type = "AIRPLANE_WITH_PAIRS";
                    } else if (config.allowAirplaneWithTwo()) {
                        type = "AIRPLANE_WITH_TWO";
                    }
                    if (type != null) primary = body.get(body.size() - 1);
                }
            }
        }
        if (type == null && frequency(counts, 4) == 1) {
            if (size == 6 && allowsSingles(config.fourAttachmentMode())) type = "FOUR_WITH_TWO";
            else if (size == 8 && allowsPairs(config.fourAttachmentMode())
                    && counts.values().stream().filter(value -> value != 4).allMatch(value -> value == 2)) {
                type = "FOUR_WITH_TWO_PAIRS";
            } else if (size == 7 && config.allowFourWithThree()) type = "FOUR_WITH_THREE";
            if (type != null) primary = rankWithCount(counts, 4);
        }
        if (type == null) throw new IllegalArgumentException("unsupported paodekuai combination");
        return new CardCombination(type, primary, cards);
    }

    public boolean canBeat(CardCombination candidate, CardCombination previous, PaoDeKuaiContext context) {
        if (previous == null) return true;
        if (isBomb(candidate)) {
            if (!isBomb(previous)) return true;
            int candidateTier = bombTier(candidate);
            int previousTier = bombTier(previous);
            return candidateTier > previousTier
                    || candidateTier == previousTier && candidate.primaryRank() > previous.primaryRank();
        }
        if (config.allowTerminalAttachmentShortage() && context != null
                && context.handBeforePlay().size() == candidate.cards().size()) {
            if ("TRIPLE".equals(candidate.type()) && previous.type().startsWith("TRIPLE_WITH")) {
                return candidate.primaryRank() > previous.primaryRank();
            }
            if ("AIRPLANE".equals(candidate.type()) && previous.type().startsWith("AIRPLANE_WITH")) {
                return tripleGroups(candidate) == tripleGroups(previous)
                        && candidate.primaryRank() > previous.primaryRank();
            }
        }
        if (candidate.cards().size() != previous.cards().size()
                || candidate.primaryRank() <= previous.primaryRank()) return false;
        if (!sameComparisonShape(candidate, previous)) return false;
        return !config.compareTripleAttachments()
                || !candidate.type().startsWith("TRIPLE_WITH")
                || maximumAttachmentRank(candidate) > maximumAttachmentRank(previous);
    }

    public boolean isBomb(CardCombination combination) {
        return "BOMB".equals(combination.type()) || combination.type().contains("_BOMB");
    }

    private int bombTier(CardCombination combination) {
        return switch (combination.type()) {
            case "SPECIAL_TRIPLE_BOMB", "SPECIAL_TRIPLE_BOMB_WITH_ONE" -> config.specialBombTier();
            case "FOUR_BOMB_WITH_ONE" -> config.fourBombWithOneTier();
            case "CONSECUTIVE_BOMB" -> (int) (tripleGroups(combination) * 2 - 1);
            default -> config.standardBombTier();
        };
    }

    private static long tripleGroups(CardCombination combination) {
        if ("AIRPLANE".equals(combination.type())) return combination.cards().size() / 3;
        if ("AIRPLANE_WITH_SINGLES".equals(combination.type())) return combination.cards().size() / 4;
        if ("AIRPLANE_WITH_PAIRS".equals(combination.type())
                || "AIRPLANE_WITH_TWO".equals(combination.type())) return combination.cards().size() / 5;
        Map<Integer, Long> counts = new HashMap<>();
        combination.cards().forEach(card -> counts.merge(StandardPokerRuleSet.rank(card), 1L, Long::sum));
        return counts.values().stream().filter(value -> value >= 3).count();
    }

    public int cardsPerPlayer(int players) {
        profile.validatePlayerCount(players, config.cardsPerPlayer());
        return config.cardsPerPlayer() == 0 ? profile.deckSize() / players : config.cardsPerPlayer();
    }

    public void validatePlay(List<Integer> cards, PaoDeKuaiContext context) {
        Integer required = context.authoritativeContext()
                ? context.authoritativeRequiredFirstCard() : config.requiredFirstCard();
        if (context.firstPlay() && required != null && !cards.contains(required)) {
            throw new IllegalStateException("first play must contain required card");
        }
        if (context.nextPlayerCardCount() == 1 && cards.size() == 1) {
            int highest = context.handBeforePlay().stream()
                    .mapToInt(StandardPokerRuleSet::rank).max().orElseThrow();
            if (StandardPokerRuleSet.rank(cards.getFirst()) != highest) {
                throw new IllegalStateException("下家报单,必须打最大单张");
            }
        }
        if (config.forceHighestPairAgainstReportedPair() && context.nextPlayerCardCount() == 2
                && cards.size() == 2
                && StandardPokerRuleSet.rank(cards.get(0)) == StandardPokerRuleSet.rank(cards.get(1))) {
            int highestPair = highestPairRank(context.handBeforePlay());
            if (highestPair >= 0 && StandardPokerRuleSet.rank(cards.get(0)) != highestPair) {
                throw new IllegalStateException("must play highest pair against reported pair");
            }
        }
    }

    private static int rankWithAtLeastCount(Map<Integer, Long> counts, long value) {
        return counts.entrySet().stream().filter(entry -> entry.getValue() >= value)
                .mapToInt(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private boolean sameComparisonShape(CardCombination candidate, CardCombination previous) {
        if (candidate.type().equals(previous.type())) return true;
        if (config.compareTripleAttachments()) return false;
        if (candidate.type().startsWith("TRIPLE_WITH") && previous.type().startsWith("TRIPLE_WITH")) {
            return !config.tripleAttachmentMode().requiresMatchingResponseShape();
        }
        return candidate.type().startsWith("AIRPLANE_WITH")
                && previous.type().startsWith("AIRPLANE_WITH")
                && tripleGroups(candidate) == tripleGroups(previous)
                && !config.airplaneAttachmentMode().requiresMatchingResponseShape();
    }

    private List<Integer> consecutiveTripleBody(Map<Integer, Long> counts, int length) {
        if (length < profile.minimumPlaneLength()) return List.of();
        int maximum = profile.allowTwoInRuns() ? 15 : 14;
        for (int start = 3; start + length - 1 <= maximum; start++) {
            List<Integer> ranks = new ArrayList<>(length);
            boolean complete = true;
            for (int rank = start; rank < start + length; rank++) {
                if (counts.getOrDefault(rank, 0L) < 3) {
                    complete = false;
                    break;
                }
                ranks.add(rank);
            }
            if (complete && validProfileRun(ranks, true)) return List.copyOf(ranks);
        }
        return List.of();
    }

    private static Map<Integer, Long> airplaneAttachments(Map<Integer, Long> counts,
            List<Integer> body) {
        Map<Integer, Long> attachments = new TreeMap<>(counts);
        for (int rank : body) {
            long remaining = attachments.getOrDefault(rank, 0L) - 3;
            if (remaining == 0) attachments.remove(rank);
            else attachments.put(rank, remaining);
        }
        return attachments;
    }

    private boolean validProfileRun(java.util.Collection<Integer> ranks, boolean pairs) {
        try {
            profile.validateRun(ranks.stream().sorted().toList(), pairs);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static long frequency(Map<Integer, Long> counts, long value) {
        return counts.values().stream().filter(current -> current == value).count();
    }

    private static int rankWithCount(Map<Integer, Long> counts, long value) {
        return counts.entrySet().stream().filter(entry -> entry.getValue() == value)
                .mapToInt(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private static boolean timingAllows(PaoDeKuaiConfig.PlayTiming timing, List<Integer> cards,
            PaoDeKuaiContext context) {
        if (timing == PaoDeKuaiConfig.PlayTiming.ANYTIME) return true;
        if (context == null) return false;
        if (context.handBeforePlay().size() == cards.size()) {
            return timing == PaoDeKuaiConfig.PlayTiming.FINAL_ONLY
                    || timing == PaoDeKuaiConfig.PlayTiming.DEALER_RESPONSE_OR_FINAL;
        }
        return timing == PaoDeKuaiConfig.PlayTiming.DEALER_RESPONSE_OR_FINAL
                && context.attachmentlessPlayPrivilege();
    }

    private static boolean allowsSingles(PaoDeKuaiConfig.AttachmentMode mode) { return mode.allowsSingles(); }
    private static boolean allowsPairs(PaoDeKuaiConfig.AttachmentMode mode) { return mode.allowsPairs(); }
    private static boolean allowsTwoSingles(PaoDeKuaiConfig.AttachmentMode mode) { return mode.allowsTwoSingles(); }

    private static int highestPairRank(List<Integer> cards) {
        Map<Integer, Long> counts = new HashMap<>();
        cards.forEach(card -> counts.merge(StandardPokerRuleSet.rank(card), 1L, Long::sum));
        return counts.entrySet().stream().filter(entry -> entry.getValue() >= 2)
                .mapToInt(Map.Entry::getKey).max().orElse(-1);
    }

    private static int maximumAttachmentRank(CardCombination combination) {
        Map<Integer, Long> counts = new HashMap<>();
        combination.cards().forEach(card -> counts.merge(StandardPokerRuleSet.rank(card), 1L, Long::sum));
        return counts.entrySet().stream().filter(entry -> entry.getKey() != combination.primaryRank())
                .mapToInt(Map.Entry::getKey).max().orElse(-1);
    }

    private static boolean legalRun(java.util.Collection<Integer> ranks, int minimum) {
        if (ranks.size() < minimum) return false;
        int previous = -1;
        for (int rank : ranks) {
            if (rank >= 15 || previous >= 0 && rank != previous + 1) return false;
            previous = rank;
        }
        return true;
    }
}
