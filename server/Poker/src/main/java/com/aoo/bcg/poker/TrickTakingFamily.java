package com.aoo.bcg.poker;
public final class TrickTakingFamily implements PokerRuleFamily{public static final String CODE="poker:trick-taking";private final ShengJiRuleSet singleRules=new ShengJiRuleSet();public String familyCode(){return CODE;}public PokerRuleSet<?> ruleSet(){return singleRules;}public TrickTakingEngine engine(){return new TrickTakingEngine();}}
