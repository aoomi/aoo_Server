package com.aoo.bcg.gamespi;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Selects every provider affected by a common capability change. */
public final class GameCapabilityRegressionSelector {
    private GameCapabilityRegressionSelector() { }

    public static Set<String> select(Collection<? extends GameProvider> providers,
                                     Set<GameCapability> changedCapabilities) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(changedCapabilities, "changedCapabilities");
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (GameProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            if (provider.capabilityManifest().capabilities().stream().anyMatch(changedCapabilities::contains))
                selected.add(provider.descriptor().code());
        }
        return Set.copyOf(selected);
    }

    public static void requireProductionBaseline(Collection<? extends GameProvider> providers) {
        Set<GameCapability> baseline = Set.of(GameCapability.LIFECYCLE,
                GameCapability.AUTHORITATIVE_COMMANDS, GameCapability.COMMAND_SCHEMA,
                GameCapability.RULES, GameCapability.SCORING, GameCapability.SETTLEMENT,
                GameCapability.RECONNECT, GameCapability.REPLAY, GameCapability.PLAYER_PERSPECTIVE,
                GameCapability.RANDOMNESS, GameCapability.INVARIANTS,
                GameCapability.SERIAL_EXECUTION, GameCapability.VERSION_LOCK,
                GameCapability.EVENT_JOURNAL);
        for (GameProvider provider : providers) provider.capabilityManifest().require(baseline);
    }
}
