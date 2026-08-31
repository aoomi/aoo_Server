package com.aoo.bcg.gamespi;

public interface RuleComponent<C> {
    String ruleId();
    String componentVersion();
    RuleStage stage();
    int priority();
    RuleResult execute(C context);
}
