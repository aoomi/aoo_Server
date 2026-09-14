package com.aoo.bcg.poker;

public interface PokerRuleFamily {
    String familyCode();
    PokerRuleSet<?> ruleSet();
}
