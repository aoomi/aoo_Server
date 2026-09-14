package com.aoo.bcg.longcard;

import com.aoo.bcg.gamespi.*;
import java.util.*;

/** One catalog-bound provider entry for the long-card lifecycle family. */
public final class LongCardFamilyProvider implements GameProvider {
    private final GameDescriptor descriptor;
    private final GameProvider regional;

    LongCardFamilyProvider(GameDescriptor descriptor, GameProvider regional) {
        this.descriptor = Objects.requireNonNull(descriptor);
        this.regional = Objects.requireNonNull(regional);
    }
    @Override public GameDescriptor descriptor() { return descriptor; }
    @Override public GameRoomFactory roomFactory() { return regional.roomFactory(); }
    @Override public Optional<GameCommandHandler> commandHandler() { return regional.commandHandler(); }
    @Override public GameCommandCommitter commandCommitter() { return regional.commandCommitter(); }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider() { return regional.reconnectViewProvider(); }
    @Override public Optional<SettlementProvider> settlementProvider() { return regional.settlementProvider(); }
    @Override public Optional<EventReplayProvider> eventReplayProvider() { return regional.eventReplayProvider(); }
    @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) { return regional.createAuthoritativeSession(context); }
    @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String,Object> state) { return regional.restoreAuthoritativeSession(state); }
    @Override public List<RuleComponent<GameCommandRequest>> ruleComponents() { return regional.ruleComponents(); }
    @Override public Map<String,Object> defaultConfiguration() {
        var out = new LinkedHashMap<String,Object>(regional.defaultConfiguration());
        out.put("provider", "long-card-family-runtime");
        out.put("regionCode", descriptor.code());
        return Map.copyOf(out);
    }
    @Override public GameCapabilityManifest capabilityManifest() { return regional.capabilityManifest(); }
}
