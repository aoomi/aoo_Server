package com.aoo.bcg.poker.nn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CN298 多人限额结算。调用方按“赢家由强到弱、输家由弱到强”提供席位顺序；
 * 玩法层只分配本局零和差额，账户锁定与最终落账由共享账本端口负责。
 */
final class NiuNiuSettlementAllocator {
  private NiuNiuSettlementAllocator() {}

  static Map<Integer, Long> allocate(int bankerSeat, List<Integer> winnerOrder,
      List<Integer> loserOrder, Map<Integer, Long> theoreticalAmounts,
      Map<Integer, Long> carryScores) {
    if (!carryScores.containsKey(bankerSeat) || carryScores.get(bankerSeat) < 0) {
      throw new IllegalArgumentException("CN298 banker carry score is required");
    }
    Map<Integer, Long> result = new LinkedHashMap<>();
    carryScores.keySet().forEach(seat -> result.put(seat, 0L));

    long collectible = loserOrder.stream().mapToLong(seat -> capped(seat, theoreticalAmounts, carryScores)).sum();
    long payable = winnerOrder.stream().mapToLong(seat -> capped(seat, theoreticalAmounts, carryScores)).sum();
    long bankerCarry = carryScores.get(bankerSeat);
    // XQP limits both directions with the banker's carry: the banker cannot
    // retain more net winnings than it brought, nor pay more than that carry.
    long loserBudget = Math.min(collectible, Math.addExact(payable, bankerCarry));
    long winnerBudget = Math.min(payable, Math.addExact(loserBudget, bankerCarry));

    long paid = 0;
    for (int seat : winnerOrder) {
      long amount = Math.min(capped(seat, theoreticalAmounts, carryScores), winnerBudget - paid);
      if (amount <= 0) break;
      result.put(seat, amount);
      paid += amount;
    }
    long collected = 0;
    for (int seat : loserOrder) {
      long amount = Math.min(capped(seat, theoreticalAmounts, carryScores), loserBudget - collected);
      if (amount <= 0) break;
      result.put(seat, -amount);
      collected += amount;
    }
    result.put(bankerSeat, collected - paid);
    long sum = result.values().stream().mapToLong(Long::longValue).sum();
    if (sum != 0) throw new IllegalStateException("CN298 settlement must be zero-sum");
    return Map.copyOf(result);
  }

  private static long capped(int seat, Map<Integer, Long> theoreticalAmounts,
      Map<Integer, Long> carryScores) {
    long theoretical = theoreticalAmounts.getOrDefault(seat, 0L);
    long carry = carryScores.getOrDefault(seat, -1L);
    if (theoretical < 0 || carry < 0) throw new IllegalArgumentException("CN298 invalid settlement input");
    return Math.min(theoretical, carry);
  }
}
