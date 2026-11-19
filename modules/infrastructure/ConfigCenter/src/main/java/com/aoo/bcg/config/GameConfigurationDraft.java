package com.aoo.bcg.config;

import com.aoo.bcg.common.operations.OperationSwitches;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Closed publication draft: no caller, database or client is allowed to supply an independent default. */
public record GameConfigurationDraft(
        String draftId,
        int gameId,
        String playType,
        String playVersion,
        String schemaVersion,
        Map<String, ?> globalRules,
        List<RegionalRuleResolver.Layer> regionalLayers,
        List<ComponentCapability> components,
        ReleaseManifest manifest,
        OperationSwitches switches,
        Set<Map<String, ?>> uiSelectableCombinations,
        Set<Map<String, ?>> serverRunnableCombinations,
        String feeExplanation,
        Instant requestedActivationAt) {
    public GameConfigurationDraft {
        if (draftId == null || draftId.isBlank() || gameId <= 0 || playType == null || playType.isBlank()
                || playVersion == null || playVersion.isBlank() || schemaVersion == null || schemaVersion.isBlank()
                || manifest == null || switches == null || feeExplanation == null || feeExplanation.isBlank()
                || requestedActivationAt == null) throw new IllegalArgumentException("incomplete configuration draft");
        globalRules = Map.copyOf(globalRules == null ? Map.of() : globalRules);
        regionalLayers = List.copyOf(regionalLayers == null ? List.of() : regionalLayers);
        components = List.copyOf(components == null ? List.of() : components);
        uiSelectableCombinations = copyCombinations(uiSelectableCombinations);
        serverRunnableCombinations = copyCombinations(serverRunnableCombinations);
    }

    private static Set<Map<String, ?>> copyCombinations(Set<Map<String, ?>> source) {
        if (source == null) return Set.of();
        return source.stream().map(Map::copyOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
