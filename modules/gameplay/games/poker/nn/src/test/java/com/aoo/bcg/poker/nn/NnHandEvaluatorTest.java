package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class NnHandEvaluatorTest {
  @Test void defaultsMatchCurrentXqpCreatePrefab() {
    var rules = NnRules.defaults();
    assertEquals("quanguo2998", NnRules.GAME_CODE);
    assertEquals(10, rules.rounds()); assertEquals(8, rules.maxPlayers()); assertEquals(2, rules.startPlayers());
    assertEquals(NnRules.Mode.CLASSIC, rules.mode()); assertEquals(4, rules.maxRobMultiplier());
    assertEquals(10, rules.maxPushMultiplier()); assertTrue(rules.kanShunDouEnabled());
  }

  @Test void recognizesSpecialHandsAndXqpOrder() {
    var straightFlush = NnHandEvaluator.evaluate(List.of(101,102,103,104,105), true);
    var bomb = NnHandEvaluator.evaluate(List.of(107,207,307,407,113), true);
    var fiveSmall = NnHandEvaluator.evaluate(List.of(101,202,302,401,103), true);
    assertEquals(NnHandEvaluator.Type.STRAIGHT_FLUSH_BULL, straightFlush.type());
    assertEquals(NnHandEvaluator.Type.BOMB_BULL, bomb.type());
    assertEquals(NnHandEvaluator.Type.FIVE_SMALL_BULL, fiveSmall.type());
    assertTrue(straightFlush.compareTo(bomb) > 0); assertTrue(bomb.compareTo(fiveSmall) > 0);
  }

  @Test void fiveSmallMatchesXqpSumOnlyRule() {
    var includesFive = NnHandEvaluator.evaluate(List.of(101, 201, 301, 402, 105), true);
    assertEquals(NnHandEvaluator.Type.FIVE_SMALL_BULL, includesFive.type());
  }

  @Test void evaluatesOrdinaryBullAndKanShunDou() {
    assertEquals(NnHandEvaluator.Type.BULL_BULL,
        NnHandEvaluator.evaluate(List.of(101,109,210,303,407), false).type());
    var cards = List.of(103,204,305,109,211);
    assertEquals(NnHandEvaluator.Type.NO_BULL, NnHandEvaluator.evaluate(cards, false).type());
    assertNotEquals(NnHandEvaluator.Type.NO_BULL, NnHandEvaluator.evaluate(cards, true).type());
  }

  @Test void kanShunDouArrangementMatchesTheHighestEvaluatedBull() {
    var hand = NnHandEvaluator.evaluate(List.of(101, 202, 303, 104, 207), true);
    assertEquals(NnHandEvaluator.Type.BULL_EIGHT, hand.type());
    assertEquals(List.of(202, 303, 104), hand.arrangedCards().subList(0, 3));
    assertEquals(List.of(101, 207), hand.arrangedCards().subList(3, 5));
  }

  @Test void appliesThreeXqpRateModes() {
    assertEquals(4, NnRules.defaults().settlementMultiplier(NnHandEvaluator.Type.BULL_BULL));
    var passion = new NnRules(10,8,2,NnRules.Mode.PASSION,4,10,NnRules.StandPolicy.EVERYONE_MAY_STAND,true,true);
    var crazy = new NnRules(10,8,2,NnRules.Mode.CRAZY,4,10,NnRules.StandPolicy.EVERYONE_MAY_STAND,true,true);
    assertEquals(16, passion.settlementMultiplier(NnHandEvaluator.Type.STRAIGHT_FLUSH_BULL));
    assertEquals(20, crazy.settlementMultiplier(NnHandEvaluator.Type.STRAIGHT_FLUSH_BULL));
  }
}
