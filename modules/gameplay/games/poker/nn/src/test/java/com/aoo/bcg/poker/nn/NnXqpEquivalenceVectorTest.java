package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.common.settlement.InMemoryRoomScoreLedger;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NnXqpEquivalenceVectorTest {
  private static final List<NnHandEvaluator.Type> XQP_TYPES = List.of(
      NnHandEvaluator.Type.BULL_ONE, NnHandEvaluator.Type.BULL_TWO,
      NnHandEvaluator.Type.BULL_THREE, NnHandEvaluator.Type.BULL_FOUR,
      NnHandEvaluator.Type.BULL_FIVE, NnHandEvaluator.Type.BULL_SIX,
      NnHandEvaluator.Type.BULL_SEVEN, NnHandEvaluator.Type.BULL_EIGHT,
      NnHandEvaluator.Type.BULL_NINE, NnHandEvaluator.Type.BULL_BULL,
      NnHandEvaluator.Type.KAN_SHUN_DOU, NnHandEvaluator.Type.STRAIGHT_BULL,
      NnHandEvaluator.Type.SILVER_FIVE, NnHandEvaluator.Type.GOLD_FIVE,
      NnHandEvaluator.Type.FLUSH_BULL, NnHandEvaluator.Type.FULL_HOUSE_BULL,
      NnHandEvaluator.Type.FIVE_SMALL_BULL, NnHandEvaluator.Type.BOMB_BULL,
      NnHandEvaluator.Type.STRAIGHT_FLUSH_BULL);

  @Test void matchesAll57XqpXmlMultiplierCells() {
    // chessRule_53.xml sid 530028/530029/530030; values are deliberately embedded vectors.
    Map<NnRules.Mode, int[]> expected = new EnumMap<>(NnRules.Mode.class);
    expected.put(NnRules.Mode.CLASSIC,
        new int[] {1,1,1,1,1,1,2,2,3,4,4,5,5,6,6,7,8,9,10});
    expected.put(NnRules.Mode.PASSION,
        new int[] {1,2,3,4,5,6,7,8,9,10,10,11,11,12,12,13,14,15,16});
    expected.put(NnRules.Mode.CRAZY,
        new int[] {1,2,3,4,5,6,7,8,9,10,10,15,15,16,16,17,18,19,20});
    expected.forEach((mode, multipliers) -> {
      NnRules rules = rules(mode, 10);
      for (int index = 0; index < XQP_TYPES.size(); index++) {
        assertEquals(multipliers[index], rules.settlementMultiplier(XQP_TYPES.get(index)),
            mode + " legacyCode=" + XQP_TYPES.get(index).legacyCode());
      }
    });
  }

  @Test void matchesAll20XqpTypeCodeAndOrderPairs() {
    // chessRule_53.xml sid 530040 order vector, plus NO_BULL=0 as the natural lower boundary.
    int[] codes = {0,1,2,3,4,5,6,7,8,9,10,11,101,111,121,131,141,151,161,171};
    var types = NnHandEvaluator.Type.values();
    assertEquals(20, types.length);
    for (int index = 0; index < codes.length; index++) {
      assertEquals(codes[index], types[index].legacyCode());
      assertEquals(index, types[index].strength());
    }
  }

  @Test void specialTypeDetectionUsesXqpPriorityBoundaries() {
    assertEquals(NnHandEvaluator.Type.FIVE_SMALL_BULL,
        hand(101,201,301,102,402).type());
    assertEquals(NnHandEvaluator.Type.BOMB_BULL,
        hand(101,201,301,401,113).type());
    // Four aces plus a two also satisfies the XQP sum-only five-small predicate; bomb wins by priority.
    assertEquals(NnHandEvaluator.Type.BOMB_BULL,
        hand(101,201,301,401,102).type());
    assertTrue(hand(101,102,103,104,105).compareTo(hand(101,201,301,401,113)) > 0);
  }

  @Test void equalHandsUseStableSeatOrderAroundBanker() {
    // F-1 修补轮（独立验证第3轮）：真实发牌座位间无重复牌 ⇒ compareTo==0 不会自然发生；
    // 以真实 deal/rob/bet 驱动到 SPLITTING 后，经 authoritativeState→restore 将两座位置为同一手牌
    // 并改选庄家，驱动生产 settle() 的 seat<bankerSeat 平局分支两侧方向（取代先前常量永真占位）。
    NnSession session = startedSession(298105);
    List<Integer> seats = session.players().keySet().stream().sorted().toList();
    session.rob(seats.get(0), 4, "rob-" + seats.get(0));
    session.rob(seats.get(1), 0, "rob-" + seats.get(1));
    int realBanker = (int) session.snapshotFor(1001).get("bankerSeat");
    for (int seat : seats) if (seat != realBanker) session.bet(seat, 1, "bet-" + seat);
    assertEquals(NnSession.Phase.SPLITTING, session.phase());
    Map<String, Object> base = new LinkedHashMap<>(session.authoritativeState());
    @SuppressWarnings("unchecked")
    Map<Integer, List<Integer>> dealt = (Map<Integer, List<Integer>>) base.get("hands");
    List<Integer> shared = dealt.get(seats.get(0));
    Map<Integer, List<Integer>> equal = new LinkedHashMap<>();
    seats.forEach(seat -> equal.put(seat, shared));
    int lower = seats.get(0);
    int upper = seats.get(1);

    // 场景一：庄家取后座 ⇒ 前座挑战者平局判胜。
    Map<String, Object> stateA = new LinkedHashMap<>(base);
    stateA.put("hands", Map.copyOf(equal));
    stateA.put("bankerSeat", upper);
    NnSession restoredA = NnSession.restore(stateA);
    splitAll(restoredA, seats, "tie-a");
    NnSession.Result resultA = restoredA.lastResult();
    assertEquals(upper, resultA.bankerSeat());
    assertEquals(0, resultA.hands().get(lower).compareTo(resultA.hands().get(upper)));
    assertTrue(resultA.scoreDelta().get(lower) > 0);
    assertTrue(resultA.scoreDelta().get(upper) < 0);
    assertEquals(0L, resultA.scoreDelta().values().stream().mapToLong(Long::longValue).sum());

    // 场景二：庄家取前座 ⇒ 后座挑战者平局判负，方向翻转。
    Map<String, Object> stateB = new LinkedHashMap<>(base);
    stateB.put("hands", Map.copyOf(equal));
    stateB.put("bankerSeat", lower);
    NnSession restoredB = NnSession.restore(stateB);
    splitAll(restoredB, seats, "tie-b");
    NnSession.Result resultB = restoredB.lastResult();
    assertEquals(lower, resultB.bankerSeat());
    assertEquals(0, resultB.hands().get(upper).compareTo(resultB.hands().get(lower)));
    assertTrue(resultB.scoreDelta().get(upper) < 0);
    assertTrue(resultB.scoreDelta().get(lower) > 0);
    assertEquals(0L, resultB.scoreDelta().values().stream().mapToLong(Long::longValue).sum());
  }

  private static void splitAll(NnSession session, List<Integer> seats, String prefix) {
    for (int seat : seats) {
      long playerId = session.players().get(seat);
      @SuppressWarnings("unchecked")
      Map<Integer, List<Integer>> hands = (Map<Integer, List<Integer>>) session.snapshotFor(playerId).get("hands");
      session.split(seat, hands.get(seat).subList(0, 3), prefix + "-split-" + seat);
    }
    assertEquals(NnSession.Phase.SETTLEMENT, session.phase());
  }

  @Test void suitTieBreakMatchesSpadeHeartClubDiamondEncoding() {
    var spade = hand(101,202,303,404,113);
    var heart = hand(201,102,303,404,113);
    var club = hand(301,102,203,404,113);
    var diamond = hand(401,102,203,304,113);
    assertTrue(spade.compareTo(heart) > 0);
    assertTrue(heart.compareTo(club) > 0);
    assertTrue(club.compareTo(diamond) > 0);
  }

  @Test void pushEncodingNeverIntroducesZeroOrNegativeMultipliers() {
    NnSession noPush = session(rules(NnRules.Mode.CLASSIC, 0), 298100);
    Map<Integer, List<Integer>> noPushOptions = reachBetting(noPush);
    assertEquals(List.of(1, 2), noPushOptions.values().stream().filter(options -> !options.isEmpty()).findFirst().orElseThrow());

    NnSession push = session(rules(NnRules.Mode.CLASSIC, 10), 298101);
    Map<Integer, List<Integer>> pushOptions = reachBetting(push);
    List<Integer> options = pushOptions.values().stream().filter(values -> !values.isEmpty()).findFirst().orElseThrow();
    assertTrue(options.contains(5));
    assertTrue(options.stream().allMatch(value -> value > 0));
  }

  @Test void allocatorCapsByCarryAndStaysZeroSumLikeXqpCarryNumBudget() {
    Map<Integer, Long> allocated = NnSettlementAllocator.allocate(0, List.of(1), List.of(2),
        Map.of(1, 100L, 2, 100L), Map.of(0, 10L, 1, 100L, 2, 7L));
    assertEquals(Map.of(0, -10L, 1, 17L, 2, -7L), allocated);
    assertEquals(0L, allocated.values().stream().mapToLong(Long::longValue).sum());
    assertEquals(7L, Math.abs(allocated.get(2)));
  }

  @Test void robThresholdRejectsInsufficientAndMissingCarryData() {
    NnSession insufficient = startedSession(298102);
    int seat = insufficient.players().keySet().stream().sorted().findFirst().orElseThrow();
    insufficient.setRobMinCarryScore(300);
    insufficient.setCarryQuery(ignored -> 250L);
    assertThrows(IllegalArgumentException.class, () -> insufficient.rob(seat, 3, "rob-low"));
    insufficient.setCarryQuery(ignored -> 300L);
    assertDoesNotThrow(() -> insufficient.rob(seat, 3, "rob-enough"));

    NnSession missing = startedSession(298103);
    int missingSeat = missing.players().keySet().stream().sorted().findFirst().orElseThrow();
    missing.setRobMinCarryScore(300);
    assertEquals("NN rob threshold requires carry score data",
        assertThrows(IllegalStateException.class, () -> missing.rob(missingSeat, 3, "rob-missing")).getMessage());
  }

  @Test void leaveAfterSettlementControlsNextRoundAdmissionAndFinishesEmptyRoom() {
    NnSession continuing = settledSession(298104);
    List<Integer> seats = continuing.players().keySet().stream().sorted().toList();
    continuing.leave(seats.get(0), "leave-one");
    assertFalse(continuing.players().containsKey(seats.get(0)));
    continuing.continueNextRound(seats.get(1), "continue-one");
    assertEquals(NnSession.Phase.ROBBING, continuing.phase());
    assertEquals(1, continuing.players().size());

    NnSession empty = settledSession(298105);
    List<Integer> emptySeats = empty.players().keySet().stream().sorted().toList();
    empty.leave(emptySeats.get(0), "leave-a");
    empty.leave(emptySeats.get(1), "leave-b");
    assertEquals(NnSession.Phase.FINISHED, empty.phase());
    assertTrue(empty.players().isEmpty());

    NnSession active = startedSession(298106);
    int activeSeat = active.players().keySet().stream().findFirst().orElseThrow();
    assertEquals("NN leave only allowed after settlement",
        assertThrows(IllegalStateException.class, () -> active.leave(activeSeat, "leave-active")).getMessage());
  }

  @Test void providerSettlementHasLedgerCappedAndLegacyUncappedBranches() {
    NnSession cappedSession = settledSession(298107);
    var cappedAuthority = new NnAuthority(cappedSession, NnRules.PLAY_VERSION);
    var ledger = new InMemoryRoomScoreLedger();
    Map<Long, Long> zeroCarry = new LinkedHashMap<>();
    cappedSession.players().values().forEach(playerId -> zeroCarry.put(playerId, 0L));
    ledger.seed(cappedSession.roomId(), zeroCarry);
    cappedAuthority.attachRoomScoreLedger(ledger);
    var capped = cappedAuthority.settlement(1, NnRules.PLAY_VERSION);
    assertTrue(capped.scoreDelta().values().stream().allMatch(score -> score == 0));

    NnSession directSession = settledSession(298108);
    var direct = new NnAuthority(directSession, NnRules.PLAY_VERSION)
        .settlement(1, NnRules.PLAY_VERSION);
    Map<Long, Long> expected = new LinkedHashMap<>();
    directSession.lastResult().scoreDelta().forEach(
        (seat, score) -> expected.put(directSession.players().get(seat), score));
    assertEquals(expected, direct.scoreDelta());
    assertTrue(direct.scoreDelta().values().stream().anyMatch(score -> score != 0));
  }

    @Test
    void roundCountOptionsUseRuleTableAuthorityWithXqpDiffRegistered() {
      // 冲突登记③（AOO-0042 证据09）：XQP chessRule_53.xml 530001/02/03/04 = 10/15/20/-1(无限局)；
      // 开房规则表（档位权威）= 10/20/30 → 以规则表为准，XQP 15 与 -1 档不采纳，仅登记。
      for (int rounds : new int[] {10, 20, 30}) {
        assertDoesNotThrow(() -> new NnRules(rounds, 8, 2, NnRules.Mode.CLASSIC,
            4, 10, NnRules.StandPolicy.EVERYONE_MAY_STAND, true, true));
      }
      assertThrows(IllegalArgumentException.class, () -> new NnRules(15, 8, 2,
          NnRules.Mode.CLASSIC, 4, 10, NnRules.StandPolicy.EVERYONE_MAY_STAND, true, true));
      assertThrows(IllegalArgumentException.class, () -> new NnRules(-1, 8, 2,
          NnRules.Mode.CLASSIC, 4, 10, NnRules.StandPolicy.EVERYONE_MAY_STAND, true, true));
    }

  private static NnHandEvaluator.Hand hand(Integer... cards) {
    return NnHandEvaluator.evaluate(List.of(cards), true);
  }

  private static NnRules rules(NnRules.Mode mode, int maxPush) {
    return new NnRules(10, 8, 2, mode, 4, maxPush,
        NnRules.StandPolicy.EVERYONE_MAY_STAND, true, true);
  }

  private static NnSession session(NnRules rules, long roomId) {
    return new NnSession(roomId, 1001, 77, rules);
  }

  private static NnSession startedSession(long roomId) {
    NnSession session = session(NnRules.defaults(), roomId);
    sit(session, 1001, "sit-owner");
    sit(session, 1002, "sit-guest");
    session.start(1001, "start");
    return session;
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, List<Integer>> reachBetting(NnSession session) {
    sit(session, 1001, "sit-owner");
    sit(session, 1002, "sit-guest");
    session.start(1001, "start");
    List<Integer> seats = session.players().keySet().stream().sorted().toList();
    seats.forEach(seat -> session.rob(seat, 4, "rob-" + seat));
    Map<Integer, List<Integer>> options = new LinkedHashMap<>();
    session.players().forEach((seat, playerId) ->
        options.put(seat, (List<Integer>) session.snapshotFor(playerId).get("betOptions")));
    return options;
  }

  @SuppressWarnings("unchecked")
  private static NnSession settledSession(long roomId) {
    NnSession session = startedSession(roomId);
    List<Integer> seats = session.players().keySet().stream().sorted().toList();
    session.rob(seats.get(0), 4, "rob-" + seats.get(0));
    session.rob(seats.get(1), 0, "rob-" + seats.get(1));
    int banker = (int) session.snapshotFor(1001).get("bankerSeat");
    for (int seat : seats) if (seat != banker) session.bet(seat, 1, "bet-" + seat);
    for (int seat : seats) {
      long playerId = session.players().get(seat);
      Map<Integer, List<Integer>> hands = (Map<Integer, List<Integer>>) session.snapshotFor(playerId).get("hands");
      session.split(seat, hands.get(seat).subList(0, 3), "split-" + seat);
    }
    assertEquals(NnSession.Phase.SETTLEMENT, session.phase());
    return session;
  }

  private static int sit(NnSession session, long playerId, String operationId) {
    session.sit(playerId, operationId);
    return (int) session.snapshotFor(playerId).get("viewerSeat");
  }
}
