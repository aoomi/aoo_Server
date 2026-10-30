package com.aoo.bcg.gamespi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameCapabilityRegressionSelectorTest {
    @Test void selectsAllAndOnlyProvidersAffectedByChangedCommonCapability() {
        GameProvider replay = provider("replay", Map.of(GameCapability.REPLAY, "ReplayAdapter"));
        GameProvider rules = provider("rules", Map.of(GameCapability.RULES, "RuleChain"));
        assertEquals(Set.of("replay"), GameCapabilityRegressionSelector.select(
                List.of(replay, rules), Set.of(GameCapability.REPLAY)));
        assertThrows(IllegalStateException.class,
                () -> GameCapabilityRegressionSelector.requireProductionBaseline(List.of(replay)));
    }

    private static GameProvider provider(String code, Map<GameCapability, String> capabilities) {
        return new GameProvider() {
            @Override public GameDescriptor descriptor() {
                return new GameDescriptor(code.hashCode() & Integer.MAX_VALUE, code, code,
                        GameCategory.POKER, "test", RegionScope.NATIONAL, "", "", "v1");
            }
            @Override public GameRoomFactory roomFactory() { return context -> { throw new UnsupportedOperationException(); }; }
            @Override public GameCapabilityManifest capabilityManifest() {
                return new GameCapabilityManifest(capabilities);
            }
        };
    }
}
