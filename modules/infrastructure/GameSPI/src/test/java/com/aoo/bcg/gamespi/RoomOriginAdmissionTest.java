package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomOriginAdmissionTest {
    private static final String RULE_HASH = "a".repeat(64);
    private static final String COMPONENT_HASH = "b".repeat(64);
    private static final String SOURCE_HASH = "c".repeat(64);

    @Test
    void exactHistoricalOriginIsImmutableAndPropagatesAcrossContextBridgeAndHandle() {
        RoomCreationContext.CompatibilityOrigin origin = legacyOrigin();
        RoomCreationContext context = new RoomCreationContext(516001L, 10L, Map.of(), origin);
        AuthoritativeGameSession session = (AuthoritativeGameSession) Proxy.newProxyInstance(
                AuthoritativeGameSession.class.getClassLoader(),
                new Class<?>[]{AuthoritativeGameSession.class}, (proxy, method, args) -> null);
        BridgedGameRoom bridge = new BridgedGameRoom(new Object(), session, origin);
        GameRoomHandle handle = new GameRoomHandle(516001L, 516, "legacy-equivalent-1", bridge, origin);

        assertSame(origin, context.requireCompatibilityOrigin());
        assertSame(origin, bridge.compatibilityOrigin());
        assertSame(origin, handle.requireCompatibilityOrigin());
        assertSame(origin.snapshotSource(), context.requireSnapshotMetadataSource());
        assertSame(origin.snapshotSource(), bridge.compatibilityOrigin().snapshotSource());
        assertSame(origin.snapshotSource(), handle.requireCompatibilityOrigin().snapshotSource());
        assertEquals(RULE_HASH, origin.snapshotSource().ruleContentHash());
        assertEquals(COMPONENT_HASH, origin.snapshotSource().componentContentHash());
        assertEquals("card-codec-v1", origin.snapshotSource().cardCodecVersion());
        assertEquals("event-interpreter-v1", origin.snapshotSource().eventInterpreterVersion());
        assertEquals("protocol-v1", origin.snapshotSource().protocolVersion());
        assertEquals("index-20261006-01", origin.snapshotSource().indexGeneration());
        assertEquals("SC/CD", origin.snapshotSource().region());
        assertEquals(SOURCE_HASH, origin.snapshotSource().sourceHash());
        assertEquals(516, origin.staticIdentity().gameId());
    }

    @Test
    void missingOriginCannotBePromotedFromAPlainCreationContext() {
        RoomCreationContext context = new RoomCreationContext(516002L, 10L, Map.of());
        assertThrows(IllegalStateException.class, context::requireCompatibilityOrigin);
    }

    @Test
    void currentCanonicalIdentityCannotBeStampedAsHistoricalOrigin() {
        GameDescriptor current = new GameDescriptor(1001, "chengdu1001", "成都血战麻将",
                GameCategory.MAHJONG, "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD",
                "chengdu1001-v1.0.0");
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, current,
                "canonical.provider", "chengdu1001-v1.0.0",
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION,
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                snapshotMetadata(current)));
    }

    @Test
    void releaseContractSchemaAndProviderMustMatchExactly() {
        GameDescriptor descriptor = legacyDescriptor();
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, descriptor,
                "legacy.provider", "wrong-release",
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION,
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                snapshotMetadata(descriptor)));
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, descriptor,
                "legacy.provider", descriptor.version(), "wrong-contract",
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                snapshotMetadata(descriptor)));
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, descriptor,
                "legacy.provider", descriptor.version(),
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION, "wrong-schema",
                snapshotMetadata(descriptor)));
    }

    @Test
    void sourceLessCompatibilityOriginConstructorFailsClosed() {
        GameDescriptor descriptor = legacyDescriptor();
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, descriptor,
                "legacy.provider", descriptor.version(),
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION,
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION));
    }

    @Test
    void snapshotSourceMustMatchStaticIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, legacyDescriptor(),
                "legacy.provider", legacyDescriptor().version(),
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION,
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                snapshotMetadata(otherAllowlistedDescriptor())));
    }

    @Test
    void snapshotSourceRejectsMalformedHashAndRegion() {
        GameDescriptor descriptor = legacyDescriptor();
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin.SnapshotMetadataSource(
                descriptor, descriptor.version(), "not-a-hash", COMPONENT_HASH,
                "card-codec-v1", "event-interpreter-v1", "protocol-v1",
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                "index-20261006-01", "SC/CD", SOURCE_HASH,
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION));
        assertThrows(IllegalArgumentException.class, () -> new RoomCreationContext.CompatibilityOrigin.SnapshotMetadataSource(
                descriptor, descriptor.version(), RULE_HASH, COMPONENT_HASH,
                "card-codec-v1", "event-interpreter-v1", "protocol-v1",
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                "index-20261006-01", "AH/XC", SOURCE_HASH,
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION));
    }

    @Test
    void handleRejectsOriginIdentityMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new GameRoomHandle(
                516003L, 1001, "chengdu1001-v1.0.0", Map.of(), legacyOrigin()));
    }

    private static RoomCreationContext.CompatibilityOrigin legacyOrigin() {
        return new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE,
                legacyDescriptor(), "legacy.provider", "legacy-equivalent-1",
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION,
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                snapshotMetadata(legacyDescriptor()));
    }

    private static GameDescriptor legacyDescriptor() {
        return new GameDescriptor(516, "chengdu1001", "成都血战麻将", GameCategory.MAHJONG,
                "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD", "legacy-equivalent-1");
    }

    private static GameDescriptor otherAllowlistedDescriptor() {
        return new GameDescriptor(618, "xcpdk", "小车跑得快", GameCategory.POKER,
                "poker:pao-de-kuai", RegionScope.CITY, "AH", "XC", "legacy-equivalent-1");
    }

    private static RoomCreationContext.CompatibilityOrigin.SnapshotMetadataSource snapshotMetadata(
            GameDescriptor descriptor) {
        return new RoomCreationContext.CompatibilityOrigin.SnapshotMetadataSource(
                descriptor, descriptor.version(), RULE_HASH, COMPONENT_HASH,
                "card-codec-v1", "event-interpreter-v1", "protocol-v1",
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION,
                "index-20261006-01", descriptor.provinceCode() + "/" + descriptor.cityCode(),
                SOURCE_HASH, RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION);
    }
}
