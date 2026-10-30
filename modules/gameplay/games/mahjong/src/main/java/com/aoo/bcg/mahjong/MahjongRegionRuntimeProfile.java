package com.aoo.bcg.mahjong;

import java.util.List;
import java.util.Set;

/** Immutable source-derived routing profile for one Mahjong catalog row. */
public record MahjongRegionRuntimeProfile(
        int gameId,
        String code,
        String family,
        String archetype,
        String region,
        Set<String> ruleComponents,
        Set<String> lifecycleComponents,
        String sourceStatus,
        String sourceEvidence,
        Set<String> allowedCreateFields,
        String configSchemaHash) {
    public MahjongRegionRuntimeProfile {
        if (gameId < 0 || code == null || code.isBlank() || family == null || family.isBlank()
                || archetype == null || archetype.isBlank() || sourceStatus == null || sourceStatus.isBlank())
            throw new IllegalArgumentException("invalid Mahjong region profile");
        ruleComponents = Set.copyOf(ruleComponents);
        lifecycleComponents = Set.copyOf(lifecycleComponents);
        allowedCreateFields = Set.copyOf(allowedCreateFields);
    }
    public boolean sourceAvailable() { return "SOURCE_SIGNATURE_AVAILABLE".equals(sourceStatus); }
    public boolean flowerExtended() { return archetype.contains("flower-extended") || ruleComponents.contains("flowers"); }
    public boolean reducedSuits() { return archetype.contains("reduced-suits") || family.equals("mahjong:xue-zhan") || family.equals("mahjong:xue-liu"); }
    public boolean multiWinner() { return lifecycleComponents.contains("multi-winner") || lifecycleComponents.contains("continuing-settlement"); }
    public boolean preSeatChoice() { return lifecycleComponents.contains("pre-seat-choice"); }
    public List<String> orderedRuleComponents() { return ruleComponents.stream().sorted().toList(); }
    public List<String> orderedLifecycleComponents() { return lifecycleComponents.stream().sorted().toList(); }
}
