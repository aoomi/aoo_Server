package com.aoo.bcg.wordcard;
import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class ByzpRulesTest{
 @Test void exactLegacyDeckHasEightyNormalCardsAndOneFlower(){List<Integer>d=ByzpRules.deck();assertEquals(81,d.size());assertEquals(80,d.stream().filter(c->c!=5101).count());assertEquals(4,d.stream().filter(c->ByzpRules.type(c)==10).count());assertEquals(1,d.stream().filter(c->c==5101).count());assertEquals(81,new HashSet<>(d).size());}
 @Test void wakeModesWrapWithinSuitAndFourChainMultiplies(){List<Integer>cards=List.of(1001,1002,1901,2001,2901);assertEquals(10,ByzpRules.wakeType(1901,ByzpRules.WakeMode.UPPER));assertEquals(29,ByzpRules.wakeType(2001,ByzpRules.WakeMode.LOWER));assertEquals(8,ByzpRules.wakeCount(cards,1001,ByzpRules.WakeMode.FOUR_CHAIN));}
 @Test void huEvaluatorRequiresPairAndCompleteGroupsAndSupportsGhosts(){assertTrue(ByzpRules.canHu(List.of(1001,1002,1003,1101,1201,1301,1401,1402),0));List<Integer>missing=List.of(1001,1002,1003,1101,1301,1401,1402);assertFalse(ByzpRules.canHu(missing,0));assertTrue(ByzpRules.canHu(missing,1));}
 @Test void scoringUsesLegacyOilConversionWakeAndMultiplier(){var c=new ByzpRules.Config(ByzpRules.WakeMode.OWN,true,true,true,false,false,false,5,2);assertEquals(12,ByzpRules.winnerScore(20,1,c,true));assertThrows(IllegalArgumentException.class,()->new ByzpRules.Config(ByzpRules.WakeMode.OWN,false,false,false,false,false,false,4,1));}
 @Test void mandatoryHuSuppressesEveryCompetingOperation(){var rules=new ByzpRuleSet();var c=new ByzpRuleSet.Context(new ByzpRules.Config(ByzpRules.WakeMode.OWN,false,true,false,false,false,false,5,1),15,0,true);assertEquals(Set.of(WordCardOperation.HU),rules.allowedOperations(0,c));}
}
