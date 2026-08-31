package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AtomicConfigurationReleaseStoreTest {
    @Test void publishesAndRollsBackEveryDomainAsOneVersionSet() {
        var store = new AtomicConfigurationReleaseStore();
        var first = ConfigurationRelease.create("release-1", Map.of(
            "runtime", "v4", "play.mahjong", "v18", "play.poker", "v12", "protocol", "v2"), Instant.EPOCH);
        var second = ConfigurationRelease.create("release-2", Map.of(
            "runtime", "v5", "play.mahjong", "v19", "play.poker", "v13", "protocol", "v2"), Instant.EPOCH.plusSeconds(1));
        store.publish(first, "");
        store.publish(second, "release-1");
        assertEquals(first.domainVersions(), store.rollback("release-1", "release-2").domainVersions());
        assertThrows(IllegalStateException.class, () -> store.rollback("release-2", "release-2"));
    }
}
