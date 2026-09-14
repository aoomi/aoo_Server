package com.aoo.bcg.config;

import com.aoo.bcg.common.config.RoomRuleSnapshot;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryGameConfigurationRepositoryTest {
    @Test void publishedVersionCannotBeOverwritten() {
        InMemoryGameConfigurationRepository repository = new InMemoryGameConfigurationRepository();
        Instant now = Instant.now();
        RoomRuleSnapshot snapshot = new RoomRuleSnapshot(
                1, 516, "v1", "c1", "room-v1", "flow-v1", "score-v1", "ui-v1", now, Map.of());
        ReleaseManifest manifest = new ReleaseManifest(516, "v1", "p1", "c1", "b1", now, Map.of());
        PublishedGameConfiguration<Object> configuration = new PublishedGameConfiguration<>(snapshot, manifest, null);
        repository.publish(configuration);
        assertThrows(IllegalStateException.class, () -> repository.publish(configuration));
        assertTrue(repository.find(516, "v1").isPresent());
    }
}
