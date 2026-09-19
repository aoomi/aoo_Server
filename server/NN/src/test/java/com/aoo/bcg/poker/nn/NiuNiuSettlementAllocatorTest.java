package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NiuNiuSettlementAllocatorTest {
  @Test void capsLosersAndPaysStrongestWinnerFirstWithoutBreakingZeroSum() {
    Map<Integer, Long> delta = NiuNiuSettlementAllocator.allocate(0,
        List.of(2, 1), List.of(3, 4),
        Map.of(1, 90L, 2, 120L, 3, 80L, 4, 100L),
        Map.of(0, 20L, 1, 200L, 2, 200L, 3, 30L, 4, 40L));
    assertEquals(90L, delta.get(2));
    assertEquals(-30L, delta.get(3));
    assertEquals(-40L, delta.get(4));
    assertEquals(0L, delta.get(1));
    assertEquals(-20L, delta.get(0));
    assertEquals(0L, delta.values().stream().mapToLong(Long::longValue).sum());
  }

  @Test void capsEachWinnerAndLoserByOwnCarryScore() {
    Map<Integer, Long> delta = NiuNiuSettlementAllocator.allocate(0,
        List.of(1), List.of(2), Map.of(1, 100L, 2, 100L), Map.of(0, 10L, 1, 25L, 2, 7L));
    assertEquals(Map.of(0, -10L, 1, 17L, 2, -7L), delta);
  }

  @Test void zeroBalancesNeverCreateScoreAndMultipleSidesRemainZeroSum() {
    assertEquals(Map.of(0, 0L, 1, 0L, 2, 0L), NiuNiuSettlementAllocator.allocate(0,
        List.of(1), List.of(2), Map.of(1, 100L, 2, 100L), Map.of(0, 0L, 1, 0L, 2, 0L)));
    Map<Integer, Long> delta = NiuNiuSettlementAllocator.allocate(0,
        List.of(3, 1), List.of(2, 4), Map.of(1, 30L, 2, 20L, 3, 40L, 4, 10L),
        Map.of(0, 15L, 1, 30L, 2, 20L, 3, 40L, 4, 10L));
    assertEquals(40L, delta.get(3));
    assertEquals(5L, delta.get(1));
    assertEquals(-20L, delta.get(2));
    assertEquals(-10L, delta.get(4));
    assertEquals(-15L, delta.get(0));
    assertEquals(0L, delta.values().stream().mapToLong(Long::longValue).sum());
  }

  @Test void capsBankerNetWinAndCollectsWeakestLoserFirstLikeXqp() {
    Map<Integer, Long> delta = NiuNiuSettlementAllocator.allocate(0,
        List.of(1), List.of(3, 2), Map.of(1, 20L, 2, 100L, 3, 100L),
        Map.of(0, 10L, 1, 100L, 2, 100L, 3, 100L));
    assertEquals(Map.of(0, 10L, 1, 20L, 2, 0L, 3, -30L), delta);
  }

  @Test void capsBankerNetLossAndPaysStrongestWinnerFirstLikeXqp() {
    Map<Integer, Long> delta = NiuNiuSettlementAllocator.allocate(0,
        List.of(2, 1), List.of(3), Map.of(1, 100L, 2, 100L, 3, 20L),
        Map.of(0, 10L, 1, 100L, 2, 100L, 3, 100L));
    assertEquals(Map.of(0, -10L, 1, 0L, 2, 30L, 3, -20L), delta);
  }
}
