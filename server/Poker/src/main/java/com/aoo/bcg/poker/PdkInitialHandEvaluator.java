package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-only evaluation of XQP initial-hand patterns and direct wins. */
final class PdkInitialHandEvaluator {
    private PdkInitialHandEvaluator() { }

    static Set<PdkAdvancedRules.InitialPattern> patterns(List<Integer> hand,
            PaoDeKuaiConfig config) {
        Map<Integer,Integer> counts = new HashMap<>();
        hand.forEach(card -> counts.merge(StandardPokerRuleSet.rank(card), 1, Integer::sum));
        EnumSet<PdkAdvancedRules.InitialPattern> found = EnumSet.noneOf(
                PdkAdvancedRules.InitialPattern.class);
        Set<PdkAdvancedRules.InitialPattern> enabled = config.advancedRules().initialPatterns();
        if (enabled.contains(PdkAdvancedRules.InitialPattern.FOUR_ACES)
                && counts.getOrDefault(14, 0) == 4)
            found.add(PdkAdvancedRules.InitialPattern.FOUR_ACES);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.FOUR_CONFIGURED_RANK)
                && config.advancedRules().fourOfKindPatternRanks().stream()
                        .anyMatch(rank -> counts.getOrDefault(rank, 0) == 4))
            found.add(PdkAdvancedRules.InitialPattern.FOUR_CONFIGURED_RANK);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.ALL_SINGLES)
                && counts.values().stream().allMatch(count -> count == 1))
            found.add(PdkAdvancedRules.InitialPattern.ALL_SINGLES);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.FULL_STRAIGHT)
                && isFullStraight(counts, config.minimumStraightLength()))
            found.add(PdkAdvancedRules.InitialPattern.FULL_STRAIGHT);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.FULL_CONSECUTIVE_PAIRS)
                && isFullConsecutivePairs(counts, config.minimumPairRunLength()))
            found.add(PdkAdvancedRules.InitialPattern.FULL_CONSECUTIVE_PAIRS);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.ALL_PAIRS)
                && counts.values().stream().allMatch(count -> count % 2 == 0))
            found.add(PdkAdvancedRules.InitialPattern.ALL_PAIRS);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.ALL_BLACK)
                && hand.stream().allMatch(card -> suit(card) == 1 || suit(card) == 3))
            found.add(PdkAdvancedRules.InitialPattern.ALL_BLACK);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.ALL_RED)
                && hand.stream().allMatch(card -> suit(card) == 2 || suit(card) == 4))
            found.add(PdkAdvancedRules.InitialPattern.ALL_RED);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.ALL_BIG)
                && counts.keySet().stream().filter(rank -> rank != 10 && rank != 14)
                        .allMatch(rank -> rank >= 10))
            found.add(PdkAdvancedRules.InitialPattern.ALL_BIG);
        if (enabled.contains(PdkAdvancedRules.InitialPattern.ALL_SMALL)
                && counts.keySet().stream().filter(rank -> rank != 10 && rank != 14)
                        .allMatch(rank -> rank < 10))
            found.add(PdkAdvancedRules.InitialPattern.ALL_SMALL);
        return Set.copyOf(found);
    }

    static boolean directWin(List<Integer> hand, PdkAdvancedRules rules) {
        Set<Integer> cards = new HashSet<>(hand);
        return rules.directWinPatterns().stream().anyMatch(cards::containsAll);
    }

    static JinHuaStrength bestJinHua(List<Integer> hand) {
        if (hand.size() < 3) throw new IllegalArgumentException("JinHua requires three cards");
        JinHuaStrength best = null;
        for (int first = 0; first < hand.size() - 2; first++)
            for (int second = first + 1; second < hand.size() - 1; second++)
                for (int third = second + 1; third < hand.size(); third++) {
                    JinHuaStrength candidate = classifyJinHua(List.of(hand.get(first),
                            hand.get(second), hand.get(third)));
                    if (best == null || candidate.compareTo(best) > 0) best = candidate;
                }
        return best;
    }

    private static JinHuaStrength classifyJinHua(List<Integer> cards) {
        List<Integer> ranks = cards.stream().map(StandardPokerRuleSet::rank)
                .sorted(Comparator.reverseOrder()).toList();
        Map<Integer,Long> counts = new HashMap<>();
        ranks.forEach(rank -> counts.merge(rank, 1L, Long::sum));
        boolean flush = cards.stream().map(PdkInitialHandEvaluator::suit).distinct().count() == 1;
        int straightHigh = straightHigh(ranks);
        int type;
        List<Integer> orderedRanks;
        if (counts.containsValue(3L)) {
            type = 5;
            orderedRanks = List.of(ranks.getFirst(), ranks.getFirst(), ranks.getFirst());
        } else if (flush && straightHigh > 0) {
            type = 4;
            orderedRanks = List.of(straightHigh);
        } else if (flush) {
            type = 3;
            orderedRanks = ranks;
        } else if (straightHigh > 0) {
            type = 2;
            orderedRanks = List.of(straightHigh);
        } else if (counts.containsValue(2L)) {
            type = 1;
            int pair = counts.entrySet().stream().filter(entry -> entry.getValue() == 2)
                    .mapToInt(Map.Entry::getKey).findFirst().orElseThrow();
            int kicker = counts.entrySet().stream().filter(entry -> entry.getValue() == 1)
                    .mapToInt(Map.Entry::getKey).findFirst().orElseThrow();
            orderedRanks = List.of(pair, kicker);
        } else {
            type = 0;
            orderedRanks = ranks;
        }
        List<Integer> suitTie = cards.stream().sorted(Comparator
                .comparingInt((Integer card) -> StandardPokerRuleSet.rank(card)).reversed()
                .thenComparingInt(PdkInitialHandEvaluator::suit))
                .map(card -> 5 - suit(card)).toList();
        return new JinHuaStrength(type, orderedRanks, suitTie);
    }

    private static boolean isFullStraight(Map<Integer,Integer> counts, int minimum) {
        if (counts.size() < minimum || counts.values().stream().anyMatch(count -> count != 1))
            return false;
        return consecutive(counts.keySet());
    }

    private static boolean isFullConsecutivePairs(Map<Integer,Integer> counts, int minimum) {
        if (counts.size() < minimum || counts.values().stream().anyMatch(count -> count != 2))
            return false;
        return consecutive(counts.keySet());
    }

    private static boolean consecutive(Set<Integer> ranks) {
        List<Integer> ordered = ranks.stream().sorted().toList();
        if (ordered.getLast() >= 15) return false;
        for (int index = 1; index < ordered.size(); index++)
            if (ordered.get(index) != ordered.get(index - 1) + 1) return false;
        return true;
    }

    private static int straightHigh(List<Integer> descending) {
        List<Integer> unique = new ArrayList<>(new HashSet<>(descending));
        unique.sort(Integer::compareTo);
        if (unique.equals(List.of(2, 3, 14))) return 3;
        if (unique.size() != 3) return 0;
        return unique.get(1) == unique.get(0) + 1 && unique.get(2) == unique.get(1) + 1
                ? unique.get(2) : 0;
    }

    private static int suit(int card) {
        return card / 100;
    }

    record JinHuaStrength(int type, List<Integer> ranks, List<Integer> suits)
            implements Comparable<JinHuaStrength> {
        JinHuaStrength {
            ranks = List.copyOf(ranks);
            suits = List.copyOf(suits);
        }

        @Override public int compareTo(JinHuaStrength other) {
            int result = Integer.compare(type, other.type);
            if (result != 0) return result;
            result = compareLexicographically(ranks, other.ranks);
            return result != 0 ? result : compareLexicographically(suits, other.suits);
        }

        private static int compareLexicographically(List<Integer> left, List<Integer> right) {
            for (int index = 0; index < Math.min(left.size(), right.size()); index++) {
                int result = Integer.compare(left.get(index), right.get(index));
                if (result != 0) return result;
            }
            return Integer.compare(left.size(), right.size());
        }
    }
}
