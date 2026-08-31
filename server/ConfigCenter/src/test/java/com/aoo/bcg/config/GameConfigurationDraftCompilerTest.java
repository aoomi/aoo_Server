package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.common.operations.OperationSwitches;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameConfigurationDraftCompilerTest {
    private static final String HASH = "a".repeat(64);

    @Test void validatesWholeDraftAndGeneratesEveryExplanationFromOneMetadataSource() {
        RuleSchemaRegistry registry = registry();
        GameConfigurationDraft draft = draft(Set.of(Map.of("renshu", 3)), Set.of(Map.of("playerCount", 3)));
        var validated = new GameConfigurationDraftCompiler(registry).compile(draft);
        assertEquals(8, validated.regionalRules().global().get("roundCount"));
        assertEquals(16, validated.regionalRules().require("SC-CD").get("roundCount"));
        assertSame(validated.explanations().creationForm(), validated.explanations().roomRuleView());
        assertSame(validated.explanations().creationForm(), validated.explanations().recordLabels());
        assertSame(validated.explanations().creationForm(), validated.explanations().supportExplanation());
    }

    @Test void rejectsUiServerDriftAndNonAtomicClientOrSwitchPublication() {
        GameConfigurationDraftCompiler compiler = new GameConfigurationDraftCompiler(registry());
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(draft(
                Set.of(Map.of("playerCount", 3)), Set.of(Map.of("playerCount", 4)))));
        GameConfigurationDraft valid = draft(Set.of(Map.of("playerCount", 3)), Set.of(Map.of("playerCount", 3)));
        GameConfigurationDraft mismatchedClient = copy(valid,
                new OperationSwitches(true, true, false, "client-v1", "all", "play-v2"));
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(mismatchedClient));
        GameConfigurationDraft missingChecksum = new GameConfigurationDraft(valid.draftId(), valid.gameId(), valid.playType(),
                valid.playVersion(), valid.schemaVersion(), valid.globalRules(), valid.regionalLayers(), valid.components(),
                new ReleaseManifest(62, "play-v2", "protocol-v2", "components-v2", "client-v2", Instant.EPOCH, Map.of()),
                valid.switches(), valid.uiSelectableCombinations(), valid.serverRunnableCombinations(), valid.feeExplanation(),
                valid.requestedActivationAt());
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(missingChecksum));
    }

    @Test void availabilityBindsListingMaintenancePercentageChannelAndKnownRegion() {
        GameConfigurationDraft valid = draft(Set.of(Map.of("playerCount", 3)), Set.of(Map.of("playerCount", 3)));
        var compiled = new GameConfigurationDraftCompiler(registry()).compile(valid);
        PlayAvailabilityPolicy policy = new PlayAvailabilityPolicy(true, false, 10,
                Set.of("APP", "MINI_PROGRAM"), Set.of("SC-CD"), "all");
        assertDoesNotThrow(() -> policy.verify(valid.switches(), compiled.regionalRules()));
        PlayAvailabilityPolicy unknownRegion = new PlayAvailabilityPolicy(true, false, 10,
                Set.of("APP"), Set.of("UNKNOWN"), "all");
        assertThrows(IllegalArgumentException.class, () -> unknownRegion.verify(valid.switches(), compiled.regionalRules()));
        assertThrows(IllegalArgumentException.class, () -> new PlayAvailabilityPolicy(true, true, 10,
                Set.of("APP"), Set.of(), "all"));
    }

    private static GameConfigurationDraft copy(GameConfigurationDraft source, OperationSwitches switches) {
        return new GameConfigurationDraft(source.draftId(), source.gameId(), source.playType(), source.playVersion(),
                source.schemaVersion(), source.globalRules(), source.regionalLayers(), source.components(), source.manifest(),
                switches, source.uiSelectableCombinations(), source.serverRunnableCombinations(), source.feeExplanation(),
                source.requestedActivationAt());
    }

    private static GameConfigurationDraft draft(Set<Map<String, ?>> ui, Set<Map<String, ?>> server) {
        List<ComponentCapability> components = List.of(
                component("rules", ComponentCapability.Kind.RULE, Set.of("playerCount"), Set.of("rule.ok"), Map.of()),
                component("flow", ComponentCapability.Kind.FLOW_ACTION, Set.of("rule.ok"), Set.of("flow.action"), Map.of("rules", range())),
                component("score", ComponentCapability.Kind.SCORING, Set.of("flow.action"), Set.of("score.total"), Map.of("flow", range())),
                component("ui", ComponentCapability.Kind.UI, Set.of("score.total"), Set.of("ui.summary"), Map.of("score", range())));
        ReleaseManifest manifest = new ReleaseManifest(62, "play-v2", "protocol-v2", "components-v2", "client-v2",
                Instant.EPOCH, Map.of("rules", HASH, "components", HASH, "ui", HASH));
        return new GameConfigurationDraft("draft-1", 62, "poker.test", "play-v2", "schema-v1",
                Map.of("playerCount", 3),
                List.of(new RegionalRuleResolver.Layer("SC", "", RegionalRuleResolver.Level.PROVINCE, Map.of()),
                        new RegionalRuleResolver.Layer("SC-CD", "SC", RegionalRuleResolver.Level.CITY, Map.of("roundCount", 16))),
                components, manifest, new OperationSwitches(true, true, false, "client-v2", "all", "play-v2"),
                ui, server, "房主支付2钻石", Instant.parse("2026-08-25T00:00:00Z"));
    }

    private static ComponentCapability component(String id, ComponentCapability.Kind kind, Set<String> in,
            Set<String> out, Map<String, ComponentCapability.VersionRange> dependencies) {
        return new ComponentCapability(id, "1.0.0", kind, in, out, dependencies, Set.of(), Set.of(), Set.of("poker.test"));
    }
    private static ComponentCapability.VersionRange range() { return new ComponentCapability.VersionRange("1.0.0", "2.0.0"); }

    private static RuleSchemaRegistry registry() {
        RuleSchemaRegistry registry = new RuleSchemaRegistry();
        registry.register(new RuleSchema("poker.test", "schema-v1", List.of(
                new RuleFieldDefinition("playerCount", Set.of("renshu"), RuleFieldDefinition.ValueType.INTEGER, "人", true,
                        null, BigDecimal.valueOf(2), BigDecimal.valueOf(4), Set.of(), "人数", "参与人数"),
                new RuleFieldDefinition("roundCount", Set.of(), RuleFieldDefinition.ValueType.INTEGER, "局", true,
                        8, BigDecimal.ONE, BigDecimal.valueOf(32), Set.of(), "局数", "牌局总局数")), List.of(), List.of()));
        return registry;
    }
}
