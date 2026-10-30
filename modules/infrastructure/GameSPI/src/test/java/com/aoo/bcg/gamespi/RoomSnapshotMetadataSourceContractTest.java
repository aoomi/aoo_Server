package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomSnapshotMetadataSourceContractTest {
    private static final String RULE_HASH = "a".repeat(64);
    private static final String COMPONENT_HASH = "b".repeat(64);
    private static final String SOURCE_HASH = "c".repeat(64);

    @Test
    void neutralSourceHasExactlyTheCanonicalTenFields() {
        assertArrayEquals(new String[]{
                        "releaseId", "ruleContentHash", "componentContentHash",
                        "cardCodecVersion", "eventInterpreterVersion", "protocolVersion",
                        "snapshotSchemaVersion", "indexGeneration", "regionCode", "sourceHash"
                },
                java.util.Arrays.stream(RoomSnapshotMetadataSource.class.getRecordComponents())
                        .map(component -> component.getName()).toArray(String[]::new));
    }

    @Test
    void neutralSourceValidatesAllRequiredFieldsWithoutDefaults() {
        RoomSnapshotMetadataSource source = source();
        assertEquals(7L, source.releaseId());
        assertEquals(11L, source.indexGeneration());
        assertEquals("CN-SC", source.regionCode());
        assertThrows(IllegalArgumentException.class, () -> new RoomSnapshotMetadataSource(
                null, RULE_HASH, COMPONENT_HASH, "card-v1", "events-v1", "protocol-v1",
                "room-snapshot-v1", 11L, "CN-SC", SOURCE_HASH));
        assertThrows(IllegalArgumentException.class, () -> new RoomSnapshotMetadataSource(
                7L, "not-a-hash", COMPONENT_HASH, "card-v1", "events-v1", "protocol-v1",
                "room-snapshot-v1", 11L, "CN-SC", SOURCE_HASH));
    }

    @Test
    void providerRequiresExactlyOneCandidate() {
        RoomSnapshotMetadataSourceProvider provider = (roomId, identity) ->
                new RoomSnapshotMetadataSourceProvider.Resolution(identity, source());
        assertSame(provider, RoomSnapshotMetadataSourceProvider.requireSingle(List.of(provider)));
        assertThrows(IllegalStateException.class,
                () -> RoomSnapshotMetadataSourceProvider.requireSingle(List.of()));
        assertThrows(IllegalStateException.class,
                () -> RoomSnapshotMetadataSourceProvider.requireSingle(List.of(provider, provider)));
        assertThrows(IllegalStateException.class,
                () -> RoomSnapshotMetadataSourceProvider.requireSingle(Arrays.asList(provider, null)));
    }

    @Test
    void providerRejectsMissingOrConflictingResolution() {
        GameDescriptor identity = identity();
        RoomSnapshotMetadataSourceProvider missing = (roomId, requested) -> null;
        assertThrows(IllegalStateException.class, () -> missing.requireSource(1001L, identity));

        RoomSnapshotMetadataSourceProvider conflict = (roomId, requested) ->
                new RoomSnapshotMetadataSourceProvider.Resolution(otherIdentity(), source());
        assertThrows(IllegalStateException.class, () -> conflict.requireSource(1001L, identity));
        assertThrows(IllegalArgumentException.class, () -> conflict.requireSource(0L, identity));
        assertThrows(NullPointerException.class, () -> conflict.requireSource(1001L, null));
    }

    @Test
    void matchingProviderResolutionIsAccepted() {
        GameDescriptor identity = identity();
        RoomSnapshotMetadataSource source = source();
        RoomSnapshotMetadataSourceProvider provider = (roomId, requested) ->
                new RoomSnapshotMetadataSourceProvider.Resolution(identity, source);
        assertSame(source, provider.requireSource(1001L, identity));
    }

    @Test
    void neutralSourcePropagatesSeparatelyFromCompatibilityOrigin() {
        RoomSnapshotMetadataSource source = source();
        RoomCreationContext context = new RoomCreationContext(1001L, 7L, Map.of(), null, source);
        AuthoritativeGameSession session = (AuthoritativeGameSession) Proxy.newProxyInstance(
                AuthoritativeGameSession.class.getClassLoader(),
                new Class<?>[]{AuthoritativeGameSession.class}, (proxy, method, args) -> null);
        BridgedGameRoom bridge = new BridgedGameRoom(new Object(), session, null, source);
        GameRoomHandle handle = new GameRoomHandle(1001L, 1001, "chengdu1001-v1.0.0",
                bridge, null, source);

        assertSame(source, context.requireRoomSnapshotMetadataSource());
        assertSame(source, bridge.requireRoomSnapshotMetadataSource());
        assertSame(source, handle.requireRoomSnapshotMetadataSource());
        assertThrows(IllegalStateException.class, context::requireCompatibilityOrigin);
    }

    @Test
    void oldCarriersWithoutNeutralSourceRemainFailClosed() {
        RoomCreationContext context = new RoomCreationContext(1002L, 7L, Map.of());
        AuthoritativeGameSession session = (AuthoritativeGameSession) Proxy.newProxyInstance(
                AuthoritativeGameSession.class.getClassLoader(),
                new Class<?>[]{AuthoritativeGameSession.class}, (proxy, method, args) -> null);
        BridgedGameRoom bridge = new BridgedGameRoom(new Object(), session);
        GameRoomHandle handle = new GameRoomHandle(1002L, 1001, "chengdu1001-v1.0.0", bridge);

        assertThrows(IllegalStateException.class, context::requireRoomSnapshotMetadataSource);
        assertThrows(IllegalStateException.class, bridge::requireRoomSnapshotMetadataSource);
        assertThrows(IllegalStateException.class, handle::requireRoomSnapshotMetadataSource);
    }

    private static RoomSnapshotMetadataSource source() {
        return new RoomSnapshotMetadataSource(
                7L, RULE_HASH, COMPONENT_HASH, "card-v1", "events-v1", "protocol-v1",
                "room-snapshot-v1", 11L, "CN-SC", SOURCE_HASH);
    }

    private static GameDescriptor identity() {
        return new GameDescriptor(1001, "chengdu1001", "成都血战麻将", GameCategory.MAHJONG,
                "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD", "chengdu1001-v1.0.0");
    }

    private static GameDescriptor otherIdentity() {
        return new GameDescriptor(1002, "other1002", "其他玩法", GameCategory.MAHJONG,
                "mahjong-other", RegionScope.CITY, "SC", "CD", "other-v1.0.0");
    }
}
