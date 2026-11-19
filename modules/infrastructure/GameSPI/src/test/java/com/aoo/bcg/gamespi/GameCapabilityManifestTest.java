package com.aoo.bcg.gamespi;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameCapabilityManifestTest {
    @Test void reportsMissingCapabilitiesAndRequiresEvidence() {
        var manifest = new GameCapabilityManifest(Map.of(
                GameCapability.LIFECYCLE, "RoomLifecycleExtension",
                GameCapability.RULES, "RuleComponent chain"));
        assertTrue(manifest.supports(GameCapability.LIFECYCLE));
        assertEquals(Set.of(GameCapability.SETTLEMENT), manifest.missing(Set.of(
                GameCapability.LIFECYCLE, GameCapability.SETTLEMENT)));
        assertThrows(IllegalStateException.class, () -> manifest.require(Set.of(GameCapability.SETTLEMENT)));
        assertThrows(IllegalArgumentException.class, () -> new GameCapabilityManifest(Map.of(
                GameCapability.REPLAY, " ")));
    }
}
