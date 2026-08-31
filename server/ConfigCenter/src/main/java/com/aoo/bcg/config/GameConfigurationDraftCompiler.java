package com.aoo.bcg.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One publication gate for field, component, region, UI, fee and explanation completeness. */
public final class GameConfigurationDraftCompiler {
    public record RuleExplanationBundle(List<String> creationForm, List<String> roomRuleView,
                                        List<String> recordLabels, List<String> supportExplanation) { }
    public record ValidatedDraft(GameConfigurationDraft source, RuleSchema schema,
                                 RegionalRuleResolver.RegionalRuleSet regionalRules,
                                 ComponentCombinationCompiler.CompiledComponents components,
                                 RuleExplanationBundle explanations) { }

    private final RuleSchemaRegistry schemas;
    private final ComponentCombinationCompiler componentCompiler;
    private final RegionalRuleResolver regionalResolver;

    public GameConfigurationDraftCompiler(RuleSchemaRegistry schemas) {
        this(schemas, new ComponentCombinationCompiler(), new RegionalRuleResolver());
    }

    public GameConfigurationDraftCompiler(RuleSchemaRegistry schemas, ComponentCombinationCompiler componentCompiler,
                                          RegionalRuleResolver regionalResolver) {
        this.schemas = java.util.Objects.requireNonNull(schemas);
        this.componentCompiler = java.util.Objects.requireNonNull(componentCompiler);
        this.regionalResolver = java.util.Objects.requireNonNull(regionalResolver);
    }

    public ValidatedDraft compile(GameConfigurationDraft draft) {
        if (draft == null) throw new IllegalArgumentException("draft is required");
        RuleSchema schema = schemas.require(draft.playType(), draft.schemaVersion());
        validateManifest(draft);
        validateSwitchDependencies(draft);
        RegionalRuleResolver.RegionalRuleSet regions = regionalResolver.compile(schema, draft.globalRules(), draft.regionalLayers());
        ComponentCombinationCompiler.CompiledComponents components = componentCompiler.compile(
                draft.playType(), schema, draft.components());
        validateReachability(schema, draft.uiSelectableCombinations(), draft.serverRunnableCombinations());
        List<String> descriptions = schema.descriptions(regions.global());
        if (descriptions.isEmpty()) throw new IllegalArgumentException("rule explanations are required");
        RuleExplanationBundle explanationBundle = new RuleExplanationBundle(
                descriptions, descriptions, descriptions, descriptions);
        return new ValidatedDraft(draft, schema, regions, components, explanationBundle);
    }

    private void validateManifest(GameConfigurationDraft draft) {
        ReleaseManifest manifest = draft.manifest();
        if (manifest.gameId() != draft.gameId() || !manifest.playVersion().equals(draft.playVersion()))
            throw new IllegalArgumentException("release manifest identity mismatch");
        if (manifest.protocolVersion() == null || manifest.protocolVersion().isBlank()
                || manifest.componentVersion() == null || manifest.componentVersion().isBlank()
                || manifest.clientBundleVersion() == null || manifest.clientBundleVersion().isBlank())
            throw new IllegalArgumentException("manifest protocol/component/client versions are required");
        Set<String> requiredChecksums = Set.of("rules", "components", "ui");
        if (!manifest.checksums().keySet().containsAll(requiredChecksums)
                || manifest.checksums().values().stream().anyMatch(value -> value == null || !value.matches("[0-9a-f]{64}")))
            throw new IllegalArgumentException("rules/components/ui checksums are required");
    }

    private void validateSwitchDependencies(GameConfigurationDraft draft) {
        var switches = draft.switches();
        if (switches.allowNewRooms() && !switches.allowExistingRoomsToFinish())
            throw new IllegalArgumentException("new rooms require existing-room completion support");
        if (switches.allowNewRooms() && (switches.rolloutKey() == null || switches.rolloutKey().isBlank()))
            throw new IllegalArgumentException("new-room rollout requires an atomic rollout key");
        if (!draft.manifest().clientBundleVersion().equals(switches.minimumClientVersion()))
            throw new IllegalArgumentException("client bundle and minimum client version must publish together");
        if (!draft.manifest().playVersion().equals(switches.serverRouteVersion()))
            throw new IllegalArgumentException("play version and server route must publish together");
    }

    private void validateReachability(RuleSchema schema, Set<Map<String, ?>> ui, Set<Map<String, ?>> server) {
        if (ui.isEmpty() || server.isEmpty()) throw new IllegalArgumentException("UI and server rule combinations are required");
        Set<Map<String, Object>> normalizedUi = normalizeAll(schema, ui);
        Set<Map<String, Object>> normalizedServer = normalizeAll(schema, server);
        if (!normalizedUi.equals(normalizedServer)) {
            Set<Map<String, Object>> uiOnly = new LinkedHashSet<>(normalizedUi); uiOnly.removeAll(normalizedServer);
            Set<Map<String, Object>> serverOnly = new LinkedHashSet<>(normalizedServer); serverOnly.removeAll(normalizedUi);
            throw new IllegalArgumentException("UI/server rule reachability mismatch: uiOnly=" + uiOnly + ", serverOnly=" + serverOnly);
        }
    }

    private Set<Map<String, Object>> normalizeAll(RuleSchema schema, Set<Map<String, ?>> values) {
        return values.stream().map(schema::normalize).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
