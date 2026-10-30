package com.aoo.bcg.bootstrap;

import com.aoo.bcg.config.RoomConfigurationSnapshotMetadataProvider;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomLockAuthorityWiringTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void gatewayAuthorityRequiresTheCanonicalProjectionBeforeRuntimeCreation() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("roomMetadata.requireProjection(route.roomId())"));
        assertTrue(source.contains("requireCanonicalMatchesHall(canonical, hallRules)"));
        assertTrue(source.contains("requireCanonicalSourceHash(route.roomId(), canonical)"));
        assertTrue(source.contains("canonical.source()"));
        assertTrue(source.contains("ROOM_LOCK_IDENTITY_CONFLICT"));
        assertTrue(source.contains("ROOM_LOCK_INDEX_REGION_VERSION_CONFLICT"));
        assertTrue(source.contains("ROOM_LOCK_SOURCE_HASH_CONFLICT"));
        assertTrue(source.contains("requireCanonicalHashCrossChecks"));
        assertTrue(source.contains("ROOM_LOCK_RULE_HASH_CONFLICT"));
        assertTrue(source.contains("ROOM_LOCK_COMPONENT_HASH_CONFLICT"));
        assertTrue(source.contains("ROOM_LOCK_BUNDLE_HASH_CONFLICT"));
        assertTrue(source.contains("l.component_chain AS lock_component_chain"));
        assertTrue(source.contains("r.rule_hash AS release_rule_hash"));
        assertTrue(source.contains("i.bundle_hash AS index_bundle_hash"));
    }

    @Test
    void productionAssemblyCreatesOneCanonicalRoomMetadataAdapter() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertEquals(1, occurrences(source, "new RoomConfigurationSnapshotMetadataProvider("));
        assertTrue(source.contains("new RoomConfigurationLeaseStore(source, json)"));
        assertTrue(source.contains("hallLifecycle, roomMetadata"));
    }

    @Test
    void projectionRejectsAnIndexOrRegionConflict() {
        RoomSnapshotMetadataSource source = new RoomSnapshotMetadataSource(
                91L, HASH, HASH, "codec-7", "events-7", "protocol-7", "snapshot-7",
                17L, "CN-51-01", HASH);
        assertThrows(IllegalArgumentException.class, () ->
                new RoomConfigurationSnapshotMetadataProvider.CanonicalProjection(
                        1001, "chengdu1001-v1.0.0", "CN-51-02", 17, source));
        assertThrows(IllegalArgumentException.class, () ->
                new RoomConfigurationSnapshotMetadataProvider.CanonicalProjection(
                        1001, "chengdu1001-v1.0.0", "CN-51-01", 18, source));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }
}
