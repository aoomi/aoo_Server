package com.aoo.bcg.wordcard;import java.util.*;
final class YzchzRuleSet implements WordCardRuleSet<YzchzRuleSet.Context>{
 record Context(YzchzRules.Config config,int huXi,int lockedHuXi,boolean canHu,Set<Integer>passedChi){}
 public Set<WordCardOperation>allowedOperations(int seat,Context c){var x=EnumSet.of(WordCardOperation.DRAW,WordCardOperation.DISCARD,WordCardOperation.PASS,WordCardOperation.PENG,WordCardOperation.WEI,WordCardOperation.PAO,WordCardOperation.TI,WordCardOperation.CHI);if(c.canHu)x.add(WordCardOperation.HU);return Set.copyOf(x);}
 public boolean canWin(int seat,List<Integer>hand,int incoming,Context c){return c.canHu&&YzchzRules.canHu(c.huXi,c.lockedHuXi);}
}
