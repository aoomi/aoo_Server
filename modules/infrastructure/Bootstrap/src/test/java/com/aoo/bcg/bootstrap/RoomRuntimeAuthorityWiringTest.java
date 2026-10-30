package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomRuntimeAuthorityWiringTest {
    @Test
    void createAndRecoverUseTheSameDurableRuntimeIdentitySource() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("roomIdentityStore.create(route.roomId(), identity)"));
        assertTrue(source.contains("rooms.create(identityBoundProvider(provider, identity)"));
        assertTrue(source.contains("roomIdentityStore.require(route.roomId())"));
        assertTrue(source.contains("route.playVersion(), authority, null, canonical.source(), identity"));
        assertTrue(source.contains("RoomRuntimeIdentity issueIdentity(Route route, long stateVersion)"));
        assertTrue(source.contains("UUID.randomUUID().toString()"));
        assertTrue(source.contains("identityRandom.nextLong()"));
        assertTrue(!source.contains("String.valueOf(route.roomId())"));
    }

    @Test
    void destroyExposesTypedTombstoneAndCasStatuses() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("AuthorityDestroyResult destroy(long roomId"));
        assertTrue(source.contains("roomIdentityStore.destroy(roomId, stored.identity())"));
        assertTrue(source.contains("\"DESTROYED\""));
        assertTrue(source.contains("\"CAS_CONFLICT\""));
        assertTrue(source.contains("\"NOT_FOUND\""));
        assertTrue(source.contains("\"ALREADY_DESTROYED\""));
        assertTrue(source.contains("roomIdentityStore.require(roomId)"));
    }

    @Test
    void productionAssemblyPassesTheV207IdentityStore() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertEquals(1, occurrences(source, "new RoomRuntimeIdentityStore(source)"));
        assertTrue(source.contains("hallLifecycle, roomMetadata, roomIdentityStore"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }
}
