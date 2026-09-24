package com.aoo.bcg.poker.cd299;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class Cd299HandEvaluatorTest {
 private final Cd299HandEvaluator evaluator=new Cd299HandEvaluator();
 @Test void sourceSpecialTypesKeep510042Order(){var top=evaluator.pair(List.of(203,506),true);var second=evaluator.pair(List.of(212,412),true);assertEquals(1,top.type());assertEquals(2,second.type());assertTrue(top.compareTo(second)<0);}
 @Test void earthNineCanBeFiltered(){assertEquals(10,evaluator.pair(List.of(202,109),true).type());assertEquals(100,evaluator.pair(List.of(202,109),false).type());}
 @Test void zeroPointTieDoesNotUseSingleOrder(){var a=new Cd299HandEvaluator.PairRank(100,0,1,List.of(212,108),0);var b=new Cd299HandEvaluator.PairRank(100,0,7,List.of(105,305),0);assertEquals(0,a.compareTo(b));}
 @Test void nonZeroTieUses510043SingleOrder(){var a=new Cd299HandEvaluator.PairRank(100,6,1,List.of(212,204),0);var b=new Cd299HandEvaluator.PairRank(100,6,4,List.of(404,202),0);assertTrue(a.compareTo(b)<0);}
 @Test void tailBreaksTieBeforeHead(){var strong=evaluator.pair(List.of(203,506),true);var weak=evaluator.pair(List.of(212,412),true);var ordinary=evaluator.pair(List.of(202,204),true);var a=new Cd299HandEvaluator.SplitRank(ordinary,strong);var b=new Cd299HandEvaluator.SplitRank(ordinary,weak);assertTrue(a.compareTo(b)<0);}
 @Test void threeFlowerAndFourCardExclusionsMatch510047(){assertTrue(evaluator.isThreeFlower(List.of(110,210,111)));assertFalse(evaluator.isThreeFlower(List.of(110,310,210,111)));assertFalse(evaluator.isThreeFlower(List.of(202,402,203)));}
 @Test void splitRejectsDuplicateOrReversedAuthority(){assertThrows(IllegalArgumentException.class,()->evaluator.split(List.of(203,506,203,212),true));}
 @Test void canonicalSplitAlwaysPlacesBigPairFirstAndKeepsExactTiesStable(){
  assertEquals(List.of(203,506,212,412),evaluator.bigLeftOrder(List.of(212,412,203,506),true));
  assertEquals(List.of(107,203,104,106),evaluator.bigLeftOrder(List.of(107,203,104,106),true));
 }
}
