package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ComponentCombinationCompilerTest {
    @Test void compilesCompleteCompatibleCapabilitiesInDependencyOrder() {
        var rule = component("rules", "1.2.0", ComponentCapability.Kind.RULE,
                Set.of("playerCount"), Set.of("rule.accepted"), Map.of(), Set.of());
        var flow = component("flow", "2.0.0", ComponentCapability.Kind.FLOW_ACTION,
                Set.of("rule.accepted"), Set.of("flow.action"), Map.of("rules", range("1.0.0", "2.0.0")), Set.of());
        var score = component("score", "1.0.0", ComponentCapability.Kind.SCORING,
                Set.of("flow.action"), Set.of("score.total"), Map.of("flow", range("2.0.0", "3.0.0")), Set.of());
        var ui = component("ui", "3.0.0", ComponentCapability.Kind.UI,
                Set.of("score.total"), Set.of("ui.summary"), Map.of(), Set.of("score"));
        var result = new ComponentCombinationCompiler().compile("poker.test", schema(), List.of(ui, score, flow, rule));
        assertEquals(List.of("rules", "flow", "score", "ui"), result.ordered().stream().map(ComponentCapability::componentId).toList());
        assertEquals("score", result.outputOwners().get("score.total"));
    }

    @Test void rejectsMissingCapabilitiesCyclesConflictsVersionsAndUnreachableInputs() {
        var rule = component("rules", "1.0.0", ComponentCapability.Kind.RULE, Set.of("playerCount"), Set.of("rule.ok"), Map.of(), Set.of());
        assertThrows(IllegalArgumentException.class, () -> new ComponentCombinationCompiler().compile("poker.test", schema(), List.of(rule)));

        List<ComponentCapability> cyclic = valid();
        cyclic.set(0, component("rules", "1.0.0", ComponentCapability.Kind.RULE, Set.of("playerCount"), Set.of("rule.ok"),
                Map.of("ui", range("1.0.0", "2.0.0")), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ComponentCombinationCompiler().compile("poker.test", schema(), cyclic));

        List<ComponentCapability> conflict = valid();
        conflict.set(0, new ComponentCapability("rules", "1.0.0", ComponentCapability.Kind.RULE,
                Set.of("playerCount"), Set.of("rule.ok"), Map.of(), Set.of("flow"), Set.of(), Set.of("poker.test")));
        assertThrows(IllegalArgumentException.class, () -> new ComponentCombinationCompiler().compile("poker.test", schema(), conflict));

        List<ComponentCapability> wrongVersion = valid();
        wrongVersion.set(1, component("flow", "1.0.0", ComponentCapability.Kind.FLOW_ACTION, Set.of("rule.ok"), Set.of("flow.action"),
                Map.of("rules", range("2.0.0", "3.0.0")), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ComponentCombinationCompiler().compile("poker.test", schema(), wrongVersion));

        List<ComponentCapability> unreachable = valid();
        unreachable.set(0, component("rules", "1.0.0", ComponentCapability.Kind.RULE, Set.of("legacyUnknown"), Set.of("rule.ok"), Map.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ComponentCombinationCompiler().compile("poker.test", schema(), unreachable));
    }

    private static List<ComponentCapability> valid() {
        return new ArrayList<>(List.of(
                component("rules", "1.0.0", ComponentCapability.Kind.RULE, Set.of("playerCount"), Set.of("rule.ok"), Map.of(), Set.of()),
                component("flow", "1.0.0", ComponentCapability.Kind.FLOW_ACTION, Set.of("rule.ok"), Set.of("flow.action"), Map.of("rules", range("1.0.0", "2.0.0")), Set.of()),
                component("score", "1.0.0", ComponentCapability.Kind.SCORING, Set.of("flow.action"), Set.of("score.total"), Map.of("flow", range("1.0.0", "2.0.0")), Set.of()),
                component("ui", "1.0.0", ComponentCapability.Kind.UI, Set.of("score.total"), Set.of("ui.summary"), Map.of("score", range("1.0.0", "2.0.0")), Set.of())));
    }

    private static ComponentCapability component(String id, String version, ComponentCapability.Kind kind,
            Set<String> inputs, Set<String> outputs, Map<String, ComponentCapability.VersionRange> dependencies,
            Set<String> orderedAfter) {
        return new ComponentCapability(id, version, kind, inputs, outputs, dependencies, Set.of(), orderedAfter, Set.of("poker.test"));
    }

    private static ComponentCapability.VersionRange range(String min, String max) { return new ComponentCapability.VersionRange(min, max); }

    private static RuleSchema schema() {
        return new RuleSchema("poker.test", "1", List.of(new RuleFieldDefinition("playerCount", Set.of(),
                RuleFieldDefinition.ValueType.INTEGER, "人", true, null, BigDecimal.valueOf(2),
                BigDecimal.valueOf(4), Set.of(), "人数", "参与人数")), List.of(), List.of());
    }
}
