package com.aoo.bcg.families;

import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.RuleResult;
import com.aoo.bcg.gamespi.RuleStage;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompiledRuleChainTest {
    @Test void sortsOnceAndStopsOnFirstRejection() {
        List<Integer> calls = new ArrayList<>();
        RuleComponent<List<Integer>> late = rule(20, true, calls);
        RuleComponent<List<Integer>> reject = rule(10, false, calls);
        RuleResult result = new CompiledRuleChain<>(List.of(late, reject)).execute(calls);
        assertFalse(result.accepted());
        assertEquals(List.of(10), calls);
    }
    private static RuleComponent<List<Integer>> rule(int priority, boolean accepted, List<Integer> calls) {
        return new RuleComponent<>() {
            public String ruleId() { return "r" + priority; }
            public String componentVersion() { return "1"; }
            public RuleStage stage() { return RuleStage.FLOW; }
            public int priority() { return priority; }
            public RuleResult execute(List<Integer> ignored) { calls.add(priority); return accepted ? RuleResult.accept() : RuleResult.reject("NO", "rejected"); }
        };
    }
}
