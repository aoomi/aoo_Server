package com.aoo.bcg.poker;
public final class ShengJiFamily implements PokerRuleFamily{public static final String CODE="poker:trick-taking";private final ShengJiRuleSet rules=new ShengJiRuleSet();public String familyCode(){return CODE;}public PokerRuleSet<?> ruleSet(){return rules;}public ShengJiEngine engine(){return new ShengJiEngine();}}
