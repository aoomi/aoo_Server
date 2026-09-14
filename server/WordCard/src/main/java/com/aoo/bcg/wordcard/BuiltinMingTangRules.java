package com.aoo.bcg.wordcard;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BuiltinMingTangRules {
    private static final List<MingTangRule> RULES=List.of(
            rule(10,c->c.redCards()>=10,MingTang.RED_HU), rule(20,c->c.redCards()==0,MingTang.BLACK_HU),
            rule(30,c->c.totalCards()>0&&c.smallCards()==0,MingTang.ALL_BIG), rule(40,c->c.totalCards()>0&&c.bigCards()==0,MingTang.ALL_SMALL),
            rule(50,c->c.concealedTriplets()>=3,MingTang.KAN), rule(60,c->c.liftedQuads()>0,MingTang.TI_LONG));
    private BuiltinMingTangRules(){}
    public static Set<MingTang> evaluate(MingTangContext context){LinkedHashSet<MingTang> result=new LinkedHashSet<>();RULES.stream().sorted(java.util.Comparator.comparingInt(MingTangRule::priority)).map(r->r.evaluate(context)).flatMap(java.util.Optional::stream).forEach(result::add);return Set.copyOf(result);}
    private static MingTangRule rule(int priority,java.util.function.Predicate<MingTangContext> match,MingTang value){return new MingTangRule(){public int priority(){return priority;}public java.util.Optional<MingTang> evaluate(MingTangContext context){return match.test(context)?java.util.Optional.of(value):java.util.Optional.empty();}};}
}
