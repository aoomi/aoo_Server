package com.aoo.bcg.families;

import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.RuleResult;
import java.util.Comparator;
import java.util.List;

public final class CompiledRuleChain<C> {
    private final List<RuleComponent<C>> components;
    public CompiledRuleChain(List<RuleComponent<C>> components) {
        this.components = components.stream().sorted(Comparator.comparingInt(RuleComponent<C>::priority)).toList();
    }
    public RuleResult execute(C context) {
        for (RuleComponent<C> component : components) {
            RuleResult result = component.execute(context);
            if (!result.accepted()) return result;
        }
        return RuleResult.accept();
    }
}
