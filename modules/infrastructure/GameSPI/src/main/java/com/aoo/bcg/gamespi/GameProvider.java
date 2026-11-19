package com.aoo.bcg.gamespi;

import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface GameProvider {
    GameDescriptor descriptor();

    GameRoomFactory roomFactory();

    default Optional<GameCommandHandler> commandHandler() {
        return Optional.empty();
    }

    default GameCommandCommitter commandCommitter() {
        return GameCommandCommitter.noOp();
    }

    default Optional<ReconnectViewProvider<?>> reconnectViewProvider() { return Optional.empty(); }

    default Optional<SettlementProvider> settlementProvider() { return Optional.empty(); }

    default Optional<EventReplayProvider> eventReplayProvider() { return Optional.empty(); }

    /** Creates the authority bound to an already-created legacy room without creating a shadow room. */
    default Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
        return Optional.empty();
    }

    /** Rebuilds the sole authority after a fenced node takeover. */
    default Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String, Object> state) {
        return Optional.empty();
    }

    /** Strong state boundary used by framework recovery; Map overload remains only for legacy providers. */
    default Optional<AuthoritativeGameSession> restoreAuthoritativeSession(StatePayload state) {
        return restoreAuthoritativeSession(state == null ? Map.of() : state.asMap());
    }

    default List<RuleComponent<GameCommandRequest>> ruleComponents() { return List.of(); }

    /** Immutable defaults used when the management platform has not published an override. */
    default Map<String, Object> defaultConfiguration() {
        return Map.of("enabled", false, "playVersion", descriptor().version());
    }

    default RulePayload defaultConfigurationPayload() {
        return RulePayload.copyOf(defaultConfiguration());
    }

    default Optional<GameServiceLauncher> serviceLauncher() { return Optional.empty(); }

    /** Machine-readable matrix used by bootstrap and regression selection. */
    default GameCapabilityManifest capabilityManifest() { return GameCapabilityManifest.empty(); }
}
