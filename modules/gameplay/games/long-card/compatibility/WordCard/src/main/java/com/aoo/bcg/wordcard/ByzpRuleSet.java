package com.aoo.bcg.wordcard;
import java.util.*;
/** BYZP operation and Hu authority; generic engine supplies transitions, this class owns regional legality. */
public final class ByzpRuleSet implements WordCardRuleSet<ByzpRuleSet.Context>{
 public record Context(ByzpRules.Config config,int huXi,int ghosts,boolean huAvailable){public Context{Objects.requireNonNull(config);if(huXi<0||ghosts<0)throw new IllegalArgumentException("negative BYZP context");}}
 private static final Set<WordCardOperation> ALL=Set.of(WordCardOperation.values());
 public Set<WordCardOperation>allowedOperations(int seat,Context context){if(seat<0)throw new IllegalArgumentException("invalid seat");return context!=null&&context.config().mandatoryHu()&&context.huAvailable()?Set.of(WordCardOperation.HU):ALL;}
 public boolean canWin(int seat,List<Integer>cards,int incoming,Context context){if(context==null||context.huXi()<15)return false;List<Integer>combined=new ArrayList<>(cards);if(incoming>0)combined.add(incoming);return ByzpRules.canHu(combined,context.ghosts());}
}
