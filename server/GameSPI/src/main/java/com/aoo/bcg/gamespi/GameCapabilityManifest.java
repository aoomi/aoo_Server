package com.aoo.bcg.gamespi;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Machine-readable capability matrix entry supplied by one game provider. */
public final class GameCapabilityManifest {
    private final Map<GameCapability, String> evidence;

    public GameCapabilityManifest(Map<GameCapability, String> evidence) {
        EnumMap<GameCapability, String> copy = new EnumMap<>(GameCapability.class);
        if (evidence != null) evidence.forEach((capability, value) -> {
            if (capability == null || value == null || value.isBlank())
                throw new IllegalArgumentException("capability evidence must be explicit");
            copy.put(capability, value.strip());
        });
        this.evidence = Map.copyOf(copy);
    }

    public static GameCapabilityManifest empty() { return new GameCapabilityManifest(Map.of()); }
    public boolean supports(GameCapability capability) { return evidence.containsKey(capability); }
    public String evidence(GameCapability capability) {
        String value = evidence.get(capability);
        if (value == null) throw new IllegalStateException("capability is not declared: " + capability);
        return value;
    }
    public Set<GameCapability> capabilities() { return Set.copyOf(evidence.keySet()); }
    public Set<GameCapability> missing(Set<GameCapability> required) {
        EnumSet<GameCapability> missing = required == null || required.isEmpty()
                ? EnumSet.noneOf(GameCapability.class) : EnumSet.copyOf(required);
        missing.removeAll(evidence.keySet());
        return Set.copyOf(missing);
    }
    public void require(Set<GameCapability> required) {
        Set<GameCapability> missing = missing(required);
        if (!missing.isEmpty()) throw new IllegalStateException("missing game capabilities: " + missing);
    }
}
