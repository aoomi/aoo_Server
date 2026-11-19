package com.aoo.bcg.config;

import com.aoo.bcg.common.operations.OperationSwitches;
import java.util.Set;

/** One dependent switch set for listing, maintenance, percentage, channel and region rollout. */
public record PlayAvailabilityPolicy(boolean listed, boolean maintenance, int rolloutPercent,
                                     Set<String> channels, Set<String> regionCodes, String rolloutKey) {
    public PlayAvailabilityPolicy {
        channels = Set.copyOf(channels == null ? Set.of() : channels);
        regionCodes = Set.copyOf(regionCodes == null ? Set.of() : regionCodes);
        if (rolloutPercent < 0 || rolloutPercent > 100 || rolloutKey == null || rolloutKey.isBlank())
            throw new IllegalArgumentException("invalid availability rollout");
        if (maintenance && (listed || rolloutPercent != 0))
            throw new IllegalArgumentException("maintenance must unlist and stop rollout");
        if (listed && (rolloutPercent == 0 || channels.isEmpty()))
            throw new IllegalArgumentException("listed play requires rollout percentage and channels");
        if (!listed && rolloutPercent != 0) throw new IllegalArgumentException("unlisted play cannot have rollout traffic");
    }

    public void verify(OperationSwitches switches, RegionalRuleResolver.RegionalRuleSet regionalRules) {
        if (switches == null || regionalRules == null) throw new IllegalArgumentException("switches and regional rules are required");
        if (maintenance != switches.maintenance() || listed != switches.allowNewRooms()
                || !rolloutKey.equals(switches.rolloutKey()))
            throw new IllegalArgumentException("availability policy and operation switches must publish atomically");
        Set<String> knownRegions = regionalRules.rulesByRegion().keySet();
        if (!knownRegions.containsAll(regionCodes)) throw new IllegalArgumentException("rollout references unknown regions");
    }

    public String auditScope() {
        return "percent=" + rolloutPercent + ",channels=" + new java.util.TreeSet<>(channels)
                + ",regions=" + new java.util.TreeSet<>(regionCodes);
    }
}
