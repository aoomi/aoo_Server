package com.aoo.bcg.wordcard;
import java.util.List;import java.util.Set;
public final class PaoHuZiFamily implements WordCardRuleFamily{
    public static final String CODE="word-card-pao-hu-zi";private final PaoHuZiProfile profile;
    private final WordCardRuleSet<WordCardRuleContext> rules=new WordCardRuleSet<>(){
        public Set<WordCardOperation> allowedOperations(int seat,WordCardRuleContext context){if(seat<0)throw new IllegalArgumentException("seat must not be negative");return profile.allowedOperations();}
        public boolean canWin(int seat,List<Integer> cards,int incoming,WordCardRuleContext context){return context!=null&&context.huXi()>=profile.minimumHuXi();}
    };
    public PaoHuZiFamily(PaoHuZiProfile profile){this.profile=java.util.Objects.requireNonNull(profile);}
    public String familyCode(){return CODE;}public PaoHuZiProfile profile(){return profile;}public WordCardRuleSet<WordCardRuleContext> ruleSet(){return rules;}public WordCardCoreEngine<WordCardRuleContext> engine(){return new WordCardCoreEngine<>(rules);}
}
