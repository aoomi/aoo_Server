package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class NiuNiuHandEvaluatorTest {
  @Test void defaultsMatchCurrentXqpCreatePrefab() {
    var rules = NiuNiuRules.defaults();
    assertEquals("CN298", NiuNiuRules.GAME_CODE);
    assertEquals(10, rules.rounds()); assertEquals(8, rules.maxPlayers()); assertEquals(2, rules.startPlayers());
    assertEquals(NiuNiuRules.Mode.CLASSIC, rules.mode()); assertEquals(4, rules.maxRobMultiplier());
    assertEquals(10, rules.maxPushMultiplier()); assertTrue(rules.kanShunDouEnabled());
  }

  @Test void recognizesSpecialHandsAndXqpOrder() {
    var straightFlush = NiuNiuHandEvaluator.evaluate(List.of(101,102,103,104,105), true);
    var bomb = NiuNiuHandEvaluator.evaluate(List.of(107,207,307,407,113), true);
    var fiveSmall = NiuNiuHandEvaluator.evaluate(List.of(101,202,302,401,103), true);
    assertEquals(NiuNiuHandEvaluator.Type.STRAIGHT_FLUSH_BULL, straightFlush.type());
    assertEquals(NiuNiuHandEvaluator.Type.BOMB_BULL, bomb.type());
    assertEquals(NiuNiuHandEvaluator.Type.FIVE_SMALL_BULL, fiveSmall.type());
    assertTrue(straightFlush.compareTo(bomb) > 0); assertTrue(bomb.compareTo(fiveSmall) > 0);
  }

  @Test void fiveSmallMatchesXqpSumOnlyRule() {
    var includesFive = NiuNiuHandEvaluator.evaluate(List.of(101, 201, 301, 402, 105), true);
    assertEquals(NiuNiuHandEvaluator.Type.FIVE_SMALL_BULL, includesFive.type());
  }

  @Test void evaluatesOrdinaryBullAndKanShunDou() {
    assertEquals(NiuNiuHandEvaluator.Type.BULL_BULL,
        NiuNiuHandEvaluator.evaluate(List.of(101,109,210,303,407), false).type());
    var cards = List.of(103,204,305,109,211);
    assertEquals(NiuNiuHandEvaluator.Type.NO_BULL, NiuNiuHandEvaluator.evaluate(cards, false).type());
    assertNotEquals(NiuNiuHandEvaluator.Type.NO_BULL, NiuNiuHandEvaluator.evaluate(cards, true).type());
  }

  @Test void kanShunDouArrangementMatchesTheHighestEvaluatedBull() {
    var hand = NiuNiuHandEvaluator.evaluate(List.of(101, 202, 303, 104, 207), true);
    assertEquals(NiuNiuHandEvaluator.Type.BULL_EIGHT, hand.type());
    assertEquals(List.of(202, 303, 104), hand.arrangedCards().subList(0, 3));
    assertEquals(List.of(101, 207), hand.arrangedCards().subList(3, 5));
  }

  @Test void appliesThreeXqpRateModes() {
    assertEquals(4, NiuNiuRules.defaults().settlementMultiplier(NiuNiuHandEvaluator.Type.BULL_BULL));
    var passion = new NiuNiuRules(10,8,2,NiuNiuRules.Mode.PASSION,4,10,NiuNiuRules.StandPolicy.EVERYONE_MAY_STAND,true,true);
    var crazy = new NiuNiuRules(10,8,2,NiuNiuRules.Mode.CRAZY,4,10,NiuNiuRules.StandPolicy.EVERYONE_MAY_STAND,true,true);
    assertEquals(16, passion.settlementMultiplier(NiuNiuHandEvaluator.Type.STRAIGHT_FLUSH_BULL));
    assertEquals(20, crazy.settlementMultiplier(NiuNiuHandEvaluator.Type.STRAIGHT_FLUSH_BULL));
  }
}
