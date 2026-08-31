package com.aoo.bcg.poker;
public final class ComparePokerFamily implements PokerRuleFamily{public static final String CODE="poker:compare-hand";private final StandardPokerRuleSet<Void> rules=new StandardPokerRuleSet<>();public String familyCode(){return CODE;}public PokerRuleSet<?> ruleSet(){return rules;}public ComparePokerEngine engine(){return new ComparePokerEngine();}}
