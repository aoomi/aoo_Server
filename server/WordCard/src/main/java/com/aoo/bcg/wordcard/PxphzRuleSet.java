package com.aoo.bcg.wordcard;
import java.util.*;
final class PxphzRuleSet implements WordCardRuleSet<PxphzRuleSet.Context>{
 record Context(PxphzRules.Config config,int huXi,boolean canHu,Set<Integer>passedChi,boolean fromOpposite){}
 public Set<WordCardOperation>allowedOperations(int seat,Context c){var x=EnumSet.of(WordCardOperation.DRAW,WordCardOperation.DISCARD,WordCardOperation.PASS,WordCardOperation.PENG,WordCardOperation.WEI,WordCardOperation.PAO,WordCardOperation.TI);if(!c.config.oppositeCannotChi()||!c.fromOpposite)x.add(WordCardOperation.CHI);if(c.canHu)x.add(WordCardOperation.HU);return x;}
 public boolean canWin(int seat,List<Integer>hand,int incoming,Context c){return c.canHu&&PxphzRules.canHu(hand,c.huXi);}
}
