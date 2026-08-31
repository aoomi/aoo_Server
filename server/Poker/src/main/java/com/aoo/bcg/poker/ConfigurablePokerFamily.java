package com.aoo.bcg.poker;

/** Shared family contract for card-set/action configurable poker games. */
public final class ConfigurablePokerFamily implements PokerRuleFamily {
    public static final String CODE = "poker:generic-card-round";
    private final StandardPokerRuleSet<Void> rules = new StandardPokerRuleSet<>();
    @Override public String familyCode() { return CODE; }
    @Override public PokerRuleSet<?> ruleSet() { return rules; }
}
