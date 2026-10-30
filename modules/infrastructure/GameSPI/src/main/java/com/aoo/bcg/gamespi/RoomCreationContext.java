package com.aoo.bcg.gamespi;

import java.util.Map;
import java.util.Objects;

public record RoomCreationContext(long roomId, long ownerId, RulePayload immutableRules,
                                  CompatibilityOrigin compatibilityOrigin,
                                  RoomSnapshotMetadataSource snapshotMetadataSource) {
    public RoomCreationContext {
        if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("roomId and ownerId must be positive");
        immutableRules = immutableRules == null ? RulePayload.empty() : immutableRules;
    }

    public RoomCreationContext(long roomId, long ownerId, RulePayload immutableRules) {
        this(roomId, ownerId, immutableRules, null, null);
    }

    public RoomCreationContext(long roomId, long ownerId, RulePayload immutableRules,
                               CompatibilityOrigin compatibilityOrigin) {
        this(roomId, ownerId, immutableRules, compatibilityOrigin, null);
    }

    public RoomCreationContext(long roomId, long ownerId, Map<String, ?> immutableRules) {
        this(roomId, ownerId, RulePayload.copyOf(immutableRules), null, null);
    }

    public RoomCreationContext(long roomId, long ownerId, Map<String, ?> immutableRules,
                               CompatibilityOrigin compatibilityOrigin) {
        this(roomId, ownerId, RulePayload.copyOf(immutableRules), compatibilityOrigin, null);
    }

    public RoomCreationContext(long roomId, long ownerId, Map<String, ?> immutableRules,
                               CompatibilityOrigin compatibilityOrigin,
                               RoomSnapshotMetadataSource snapshotMetadataSource) {
        this(roomId, ownerId, RulePayload.copyOf(immutableRules), compatibilityOrigin,
                snapshotMetadataSource);
    }

    public CompatibilityOrigin requireCompatibilityOrigin() {
        if (compatibilityOrigin == null) throw new IllegalStateException("COMPATIBILITY_ORIGIN_REQUIRED");
        return compatibilityOrigin;
    }

    public CompatibilityOrigin.SnapshotMetadataSource requireSnapshotMetadataSource() {
        return requireCompatibilityOrigin().requireSnapshotMetadataSource();
    }

    public RoomSnapshotMetadataSource requireRoomSnapshotMetadataSource() {
        if (snapshotMetadataSource == null) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED");
        }
        return snapshotMetadataSource;
    }

    /** Immutable admission proof for a historical Compatibility room. */
    public record CompatibilityOrigin(String source, GameDescriptor staticIdentity,
                                      String providerKey, String releaseId,
                                      String contractVersion, String snapshotSchemaVersion,
                                      SnapshotMetadataSource snapshotSource) {
        public static final String HISTORICAL_SOURCE = "historical-compatibility";
        public static final String CONTRACT_VERSION = "compatibility-v1";
        public static final String SNAPSHOT_SCHEMA_VERSION = "room-snapshot-v1";

        /** Retained only as a source-compatible constructor; it always fails closed. */
        public CompatibilityOrigin(String source, GameDescriptor staticIdentity,
                                   String providerKey, String releaseId,
                                   String contractVersion, String snapshotSchemaVersion) {
            this(source, staticIdentity, providerKey, releaseId, contractVersion,
                    snapshotSchemaVersion, null);
        }

        public CompatibilityOrigin {
            requireText(source, "source");
            Objects.requireNonNull(staticIdentity, "staticIdentity");
            requireText(providerKey, "providerKey");
            requireText(releaseId, "releaseId");
            requireText(contractVersion, "contractVersion");
            requireText(snapshotSchemaVersion, "snapshotSchemaVersion");
            if (snapshotSource == null) {
                throw new IllegalArgumentException("COMPATIBILITY_SNAPSHOT_SOURCE_REQUIRED");
            }
            if (!HISTORICAL_SOURCE.equals(source)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_NOT_HISTORICAL");
            }
            if (!GameRegistryFactory.isCompatibilityIdentityAllowed(staticIdentity)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_IDENTITY_NOT_ALLOWLISTED");
            }
            if (!staticIdentity.version().equals(releaseId)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_RELEASE_MISMATCH");
            }
            if (!CONTRACT_VERSION.equals(contractVersion)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_CONTRACT_UNSUPPORTED");
            }
            if (!SNAPSHOT_SCHEMA_VERSION.equals(snapshotSchemaVersion)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_SNAPSHOT_SCHEMA_UNSUPPORTED");
            }
            snapshotSource.requireMatches(staticIdentity, releaseId, contractVersion, snapshotSchemaVersion);
        }

        public SnapshotMetadataSource requireSnapshotMetadataSource() {
            if (snapshotSource == null) throw new IllegalStateException("COMPATIBILITY_SNAPSHOT_SOURCE_REQUIRED");
            return snapshotSource;
        }

        public void requireMatches(GameDescriptor descriptor, String actualProviderKey) {
            Objects.requireNonNull(descriptor, "provider.descriptor");
            requireText(actualProviderKey, "actualProviderKey");
            if (!staticIdentity.equals(descriptor)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_IDENTITY_MISMATCH");
            }
            if (!providerKey.equals(actualProviderKey)) {
                throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_PROVIDER_MISMATCH");
            }
        }

        private static void requireText(String value, String field) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        }

        /** Immutable source metadata that must accompany every Compatibility origin. */
        public record SnapshotMetadataSource(
                GameDescriptor staticIdentity,
                String releaseId,
                String ruleContentHash,
                String componentContentHash,
                String cardCodecVersion,
                String eventInterpreterVersion,
                String protocolVersion,
                String snapshotSchemaVersion,
                String indexGeneration,
                String region,
                String sourceHash,
                String contractVersion) {
            public SnapshotMetadataSource {
                Objects.requireNonNull(staticIdentity, "snapshotSource.staticIdentity");
                requireText(releaseId, "snapshotSource.releaseId");
                requireSha256(ruleContentHash, "snapshotSource.ruleContentHash");
                requireSha256(componentContentHash, "snapshotSource.componentContentHash");
                requireText(cardCodecVersion, "snapshotSource.cardCodecVersion");
                requireText(eventInterpreterVersion, "snapshotSource.eventInterpreterVersion");
                requireText(protocolVersion, "snapshotSource.protocolVersion");
                requireText(snapshotSchemaVersion, "snapshotSource.snapshotSchemaVersion");
                requireText(indexGeneration, "snapshotSource.indexGeneration");
                requireText(region, "snapshotSource.region");
                requireSha256(sourceHash, "snapshotSource.sourceHash");
                requireText(contractVersion, "snapshotSource.contractVersion");
                if (!staticIdentity.version().equals(releaseId)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_RELEASE_MISMATCH");
                }
                if (!CONTRACT_VERSION.equals(contractVersion)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_CONTRACT_UNSUPPORTED");
                }
                if (!SNAPSHOT_SCHEMA_VERSION.equals(snapshotSchemaVersion)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_SCHEMA_UNSUPPORTED");
                }
                if (!expectedRegion(staticIdentity).equals(region)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_REGION_MISMATCH");
                }
            }

            private void requireMatches(GameDescriptor identity, String release,
                                        String contract, String schema) {
                if (!staticIdentity.equals(identity)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_IDENTITY_MISMATCH");
                }
                if (!releaseId.equals(release)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_RELEASE_MISMATCH");
                }
                if (!contractVersion.equals(contract)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_CONTRACT_MISMATCH");
                }
                if (!snapshotSchemaVersion.equals(schema)) {
                    throw new IllegalArgumentException("SNAPSHOT_SOURCE_SCHEMA_MISMATCH");
                }
            }

            private static String expectedRegion(GameDescriptor identity) {
                return switch (identity.regionScope()) {
                    case NATIONAL -> "NATIONAL";
                    case PROVINCE -> identity.provinceCode();
                    case CITY -> identity.provinceCode() + "/" + identity.cityCode();
                };
            }

            private static void requireText(String value, String field) {
                if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
            }

            private static void requireSha256(String value, String field) {
                requireText(value, field);
                if (!value.matches("[0-9a-fA-F]{64}")) {
                    throw new IllegalArgumentException(field + " must be a SHA-256 hex digest");
                }
            }
        }
    }
}
