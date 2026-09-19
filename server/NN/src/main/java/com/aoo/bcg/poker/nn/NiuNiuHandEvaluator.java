package com.aoo.bcg.poker.nn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** CN298 五张牌牌型判定。牌 ID 延续项目扑克牌编码：花色 * 100 + 点数（A=1，J=11，Q=12，K=13）。 */
public final class NiuNiuHandEvaluator {
  public enum Type {
    NO_BULL(0, 0), BULL_ONE(1, 1), BULL_TWO(2, 2), BULL_THREE(3, 3),
    BULL_FOUR(4, 4), BULL_FIVE(5, 5), BULL_SIX(6, 6), BULL_SEVEN(7, 7),
    BULL_EIGHT(8, 8), BULL_NINE(9, 9), BULL_BULL(10, 10), KAN_SHUN_DOU(11, 11),
    STRAIGHT_BULL(101, 12), SILVER_FIVE(111, 13), GOLD_FIVE(121, 14),
    FLUSH_BULL(131, 15), FULL_HOUSE_BULL(141, 16), FIVE_SMALL_BULL(151, 17),
    BOMB_BULL(161, 18), STRAIGHT_FLUSH_BULL(171, 19);

    private final int legacyCode;
    private final int strength;

    Type(int legacyCode, int strength) {
      this.legacyCode = legacyCode;
      this.strength = strength;
    }

    public int legacyCode() { return legacyCode; }
    int strength() { return strength; }
  }

  public record Hand(Type type, List<Integer> arrangedCards, int primaryRank, int highCardRank, int highCardSuit)
      implements Comparable<Hand> {
    public Hand {
      arrangedCards = List.copyOf(arrangedCards);
    }

    @Override public int compareTo(Hand other) {
      int result = Integer.compare(type.strength(), other.type.strength());
      if (result == 0) result = Integer.compare(primaryRank, other.primaryRank);
      if (result == 0) result = Integer.compare(highCardRank, other.highCardRank);
      // XQP 花色值越小越大：黑桃(1) > 红桃(2) > 梅花(3) > 方片(4)。
      if (result == 0) result = Integer.compare(other.highCardSuit, highCardSuit);
      return result;
    }
  }

  private NiuNiuHandEvaluator() {}

  public static Hand evaluate(List<Integer> cards, boolean kanShunDouEnabled) {
    if (cards == null || cards.size() != 5 || new HashSet<>(cards).size() != 5)
      throw new IllegalArgumentException("exactly five distinct cards are required");
    List<Integer> sorted = new ArrayList<>(cards);
    sorted.sort(Comparator.comparingInt(NiuNiuHandEvaluator::rank).thenComparingInt(NiuNiuHandEvaluator::suit));
    for (int card : sorted) validateCard(card);

    boolean flush = sorted.stream().map(NiuNiuHandEvaluator::suit).distinct().count() == 1;
    boolean straight = isStraight(sorted);
    Map<Integer, Integer> counts = rankCounts(sorted);
    int primary = counts.entrySet().stream().filter(e -> e.getValue() >= 3)
        .map(Map.Entry::getKey).max(Integer::compareTo).orElse(0);
    Type type;
    if (flush && straight) type = Type.STRAIGHT_FLUSH_BULL;
    else if (counts.containsValue(4)) {
      type = Type.BOMB_BULL;
      primary = counts.entrySet().stream().filter(e -> e.getValue() == 4).findFirst().orElseThrow().getKey();
    } else if (sorted.stream().mapToInt(NiuNiuHandEvaluator::rank).sum() <= 10)
      type = Type.FIVE_SMALL_BULL;
    else if (counts.size() == 2 && counts.containsValue(3) && counts.containsValue(2)) type = Type.FULL_HOUSE_BULL;
    else if (flush) type = Type.FLUSH_BULL;
    else if (sorted.stream().allMatch(c -> rank(c) >= 11)) type = Type.GOLD_FIVE;
    else if (sorted.stream().allMatch(c -> rank(c) >= 10)) type = Type.SILVER_FIVE;
    else if (straight) type = Type.STRAIGHT_BULL;
    else type = ordinaryType(sorted, kanShunDouEnabled);

    int high = sorted.stream().mapToInt(NiuNiuHandEvaluator::comparisonRank).max().orElseThrow();
    int highSuit = sorted.stream().filter(c -> comparisonRank(c) == high)
        .mapToInt(NiuNiuHandEvaluator::suit).min().orElseThrow();
    return new Hand(type, arrangeBullCards(sorted, kanShunDouEnabled), primary, high, highSuit);
  }

  private static Type ordinaryType(List<Integer> cards, boolean kanShunDouEnabled) {
    int best = -1;
    for (int i = 0; i < 3; i++) for (int j = i + 1; j < 4; j++) for (int k = j + 1; k < 5; k++) {
      boolean valid = (point(cards.get(i)) + point(cards.get(j)) + point(cards.get(k))) % 10 == 0;
      if (!valid && kanShunDouEnabled) {
        List<Integer> ranks = List.of(rank(cards.get(i)), rank(cards.get(j)), rank(cards.get(k)));
        valid = new HashSet<>(ranks).size() == 1 || isConsecutive(ranks);
      }
      if (valid) {
        int remainder = 0;
        for (int n = 0; n < 5; n++) if (n != i && n != j && n != k) remainder += point(cards.get(n));
        best = Math.max(best, remainder % 10 == 0 ? 10 : remainder % 10);
      }
    }
    if (best < 0) return Type.NO_BULL;
    return Type.values()[best];
  }

  private static List<Integer> arrangeBullCards(List<Integer> cards, boolean ksd) {
    List<Integer> best = null;
    int bestBull = -1;
    for (int i = 0; i < 3; i++) for (int j = i + 1; j < 4; j++) for (int k = j + 1; k < 5; k++) {
      boolean valid = (point(cards.get(i)) + point(cards.get(j)) + point(cards.get(k))) % 10 == 0;
      if (!valid && ksd) {
        List<Integer> ranks = List.of(rank(cards.get(i)), rank(cards.get(j)), rank(cards.get(k)));
        valid = new HashSet<>(ranks).size() == 1 || isConsecutive(ranks);
      }
      if (valid) {
        List<Integer> result = new ArrayList<>(5);
        result.add(cards.get(i)); result.add(cards.get(j)); result.add(cards.get(k));
        for (int n = 0; n < 5; n++) if (n != i && n != j && n != k) result.add(cards.get(n));
        int remainder = point(result.get(3)) + point(result.get(4));
        int bull = remainder % 10 == 0 ? 10 : remainder % 10;
        if (bull > bestBull) {
          bestBull = bull;
          best = result;
        }
      }
    }
    return best == null ? cards : best;
  }

  private static boolean isStraight(List<Integer> cards) {
    return isConsecutive(cards.stream().map(NiuNiuHandEvaluator::rank).toList());
  }

  private static boolean isConsecutive(List<Integer> input) {
    List<Integer> ranks = new ArrayList<>(new HashSet<>(input));
    if (ranks.size() != input.size()) return false;
    ranks.sort(Integer::compareTo);
    if (consecutive(ranks)) return true;
    if (ranks.get(0) == 1) {
      ranks.remove(0); ranks.add(14); ranks.sort(Integer::compareTo);
      return consecutive(ranks);
    }
    return false;
  }

  private static boolean consecutive(List<Integer> ranks) {
    for (int i = 1; i < ranks.size(); i++) if (ranks.get(i) != ranks.get(0) + i) return false;
    return true;
  }

  private static Map<Integer, Integer> rankCounts(List<Integer> cards) {
    Map<Integer, Integer> counts = new HashMap<>();
    cards.forEach(c -> counts.merge(rank(c), 1, Integer::sum));
    return counts;
  }

  private static int point(int card) { return Math.min(rank(card), 10); }
  private static int rank(int card) { return card % 100; }
  private static int suit(int card) { return card / 100; }
  private static int comparisonRank(int card) { return rank(card) == 1 ? 14 : rank(card); }
  private static void validateCard(int card) {
    if (suit(card) < 1 || suit(card) > 4 || rank(card) < 1 || rank(card) > 13)
      throw new IllegalArgumentException("invalid card id: " + card);
  }
}
