package com.aoo.bcg.config;

import com.aoo.bcg.common.config.RoomRuleSnapshot;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Optional;

/**
 * Binds a room to the immutable configuration selected at creation.
 *
 * <p>The room-lock row is the only runtime metadata projection read by
 * {@link #findMetadata(long)}. Incomplete historical rows are deliberately rejected;
 * no release, bundle, UI or default value is used to repair a missing field.</p>
 */
public final class RoomConfigurationLeaseStore {
    private static final String SHA256 = "^[0-9a-fA-F]{64}$";
    private static final String VERSION = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$";
    private static final String ROOM_LOCK_METADATA_COLUMNS =
            "game_id,region_code,play_version,index_generation,release_id,rule_hash,component_hash,"
                    + "card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash";
    private static final String ROOM_LOCK_METADATA_SQL =
            "SELECT " + ROOM_LOCK_METADATA_COLUMNS + " FROM aoo_room_rule_lock WHERE room_id=?";
    private static final String ROOM_LOCK_SNAPSHOT_SQL =
            "SELECT " + ROOM_LOCK_METADATA_COLUMNS + ",immutable_rules,created_at"
                    + " FROM aoo_room_rule_lock WHERE room_id=?";

    public record Lease(long roomId, RoomRuleSnapshot snapshot, long releaseId, RoomLockMetadata metadata) {
        public Lease {
            if (roomId <= 0 || snapshot == null || releaseId <= 0 || metadata == null
                    || metadata.releaseId() != releaseId) {
                throw new IllegalArgumentException("invalid room configuration lease");
            }
        }

        /** Compatibility constructor that fails closed rather than fabricating metadata. */
        public Lease(long roomId, RoomRuleSnapshot snapshot, long releaseId) {
            this(roomId, snapshot, releaseId, null);
        }
    }

    /**
     * Metadata-only lease for consumers that must not construct the legacy
     * {@link RoomRuleSnapshot}.  It is read from the same immutable lock row
     * and never inspects the legacy rules JSON.
     */
    public record MetadataLease(long roomId, RoomLockMetadata metadata) {
        public MetadataLease {
            if (roomId <= 0 || metadata == null) {
                throw new IllegalArgumentException("invalid room configuration metadata lease");
            }
        }
    }

    /** Complete immutable projection read from one {@code aoo_room_rule_lock} row. */
    public record RoomLockMetadata(
            long releaseId,
            int gameId,
            String regionCode,
            String playVersion,
            long indexGeneration,
            String ruleContentHash,
            String componentContentHash,
            String cardCodecVersion,
            String eventInterpreterVersion,
            String protocolVersion,
            String snapshotSchemaVersion,
            String sourceHash) {
        public RoomLockMetadata {
            if (releaseId <= 0 || gameId <= 0 || indexGeneration <= 0) {
                throw new IllegalArgumentException("ROOM_CONFIGURATION_LOCK_IDENTITY_REQUIRED");
            }
            requireText(regionCode, "ROOM_CONFIGURATION_LOCK_REGION_REQUIRED");
            requireText(playVersion, "ROOM_CONFIGURATION_LOCK_PLAY_VERSION_REQUIRED");
            requireHash(ruleContentHash, "ROOM_CONFIGURATION_LOCK_RULE_HASH_INVALID");
            requireHash(componentContentHash, "ROOM_CONFIGURATION_LOCK_COMPONENT_HASH_INVALID");
            requireText(cardCodecVersion, "ROOM_CONFIGURATION_LOCK_CARD_CODEC_REQUIRED");
            requireText(eventInterpreterVersion, "ROOM_CONFIGURATION_LOCK_EVENT_INTERPRETER_REQUIRED");
            requireText(protocolVersion, "ROOM_CONFIGURATION_LOCK_PROTOCOL_REQUIRED");
            requireVersion(snapshotSchemaVersion, "ROOM_CONFIGURATION_LOCK_SCHEMA_REQUIRED");
            requireHash(sourceHash, "ROOM_CONFIGURATION_LOCK_SOURCE_HASH_INVALID");
        }

        public RoomSnapshotMetadataSource neutralSource() {
            return new RoomSnapshotMetadataSource(
                    releaseId, ruleContentHash, componentContentHash, cardCodecVersion,
                    eventInterpreterVersion, protocolVersion, snapshotSchemaVersion,
                    indexGeneration, regionCode, sourceHash);
        }

        private static void requireHash(String value, String code) {
            if (value == null || !value.matches(SHA256)) throw new IllegalArgumentException(code);
        }

        private static void requireVersion(String value, String code) {
            if (value == null || !value.matches(VERSION)) throw new IllegalArgumentException(code);
        }

        private static void requireText(String value, String code) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(code);
        }
    }

    private final DataSource dataSource;
    private final ObjectMapper json;

    public RoomConfigurationLeaseStore(DataSource dataSource, ObjectMapper json) {
        this.dataSource = java.util.Objects.requireNonNull(dataSource, "dataSource");
        this.json = java.util.Objects.requireNonNull(json, "json");
    }

    /**
     * Creates a new lock with explicit source and snapshot-schema values.
     * Existing callers using the old signature must migrate; the old overload
     * below rejects instead of supplying a default.
     */
    public Lease open(long roomId, int gameId, String regionCode, String playVersion,
                      String sourceHash, String snapshotSchemaVersion) {
        requireHash(sourceHash, "ROOM_CONFIGURATION_LOCK_SOURCE_HASH_INVALID");
        requireVersion(snapshotSchemaVersion, "ROOM_CONFIGURATION_LOCK_SCHEMA_REQUIRED");
        if (roomId <= 0 || gameId <= 0 || regionCode == null || regionCode.isBlank()
                || playVersion == null || playVersion.isBlank()) {
            throw new IllegalArgumentException("ROOM_CONFIGURATION_LOCK_IDENTITY_REQUIRED");
        }
        /*
         * The release table is not the owner of codec/event/protocol runtime
         * versions.  Keeping the old join here would query columns that do
         * not exist on aoo_game_release and would force a guessed projection.
         * The Hall transaction must supply the complete room-lock row in its
         * own bounded batch; until that canonical writer exists, refuse to
         * create a partial lock rather than writing or repairing metadata.
         */
        throw new IllegalStateException("ROOM_CONFIGURATION_LOCK_RUNTIME_METADATA_SOURCE_REQUIRED");
    }

    /** Old creation calls cannot produce complete metadata and are rejected. */
    public Lease open(long roomId, int gameId, String regionCode, String playVersion) {
        throw new IllegalStateException("ROOM_CONFIGURATION_LOCK_METADATA_SOURCE_REQUIRED");
    }

    /**
     * Reads every canonical field from the room-lock row. Deliberately no
     * release table is joined here: version/codec/event/protocol and hashes
     * must be the values locked for this room.
     */
    public Optional<Lease> find(long roomId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(ROOM_LOCK_SNAPSHOT_SQL)) {
            statement.setLong(1, roomId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return Optional.empty();
                RoomLockMetadata metadata = readMetadata(row);
                Map<String, Object> rules = json.readValue(row.getString("immutable_rules"), new TypeReference<>() {});
                Object scoreVersion = rules.get("scoreVersion");
                if (!(scoreVersion instanceof String score) || score.isBlank()) {
                    throw new IllegalStateException("ROOM_CONFIGURATION_LOCK_SCORE_VERSION_REQUIRED");
                }
                RoomRuleSnapshot snapshot = new RoomRuleSnapshot(
                        metadata.releaseId(), metadata.gameId(), metadata.playVersion(), metadata.componentContentHash(),
                        metadata.cardCodecVersion(), metadata.eventInterpreterVersion(), score,
                        metadata.protocolVersion(), row.getTimestamp("created_at").toInstant(), rules);
                return Optional.of(new Lease(roomId, snapshot, metadata.releaseId(), metadata));
            }
        } catch (IllegalStateException | IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("cannot load room configuration lock", failure);
        }
    }

    /**
     * Reads only canonical room-lock metadata.  In particular, this query
     * does not select or parse {@code immutable_rules}, so a historical row
     * without legacy {@code scoreVersion} cannot break metadata consumers.
     */
    public Optional<MetadataLease> findMetadata(long roomId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(ROOM_LOCK_METADATA_SQL)) {
            statement.setLong(1, roomId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return Optional.empty();
                return Optional.of(new MetadataLease(roomId, readMetadata(row)));
            }
        } catch (IllegalStateException | IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("cannot load room configuration lock metadata", failure);
        }
    }

    public MetadataLease requireMetadata(long roomId) {
        return findMetadata(roomId)
                .orElseThrow(() -> new IllegalArgumentException("ROOM_CONFIGURATION_LOCK_NOT_FOUND"));
    }

    public Lease require(long roomId) {
        return find(roomId).orElseThrow(() -> new IllegalArgumentException("ROOM_CONFIGURATION_LOCK_NOT_FOUND"));
    }

    private static RoomLockMetadata readMetadata(ResultSet row) throws java.sql.SQLException {
        return new RoomLockMetadata(
                row.getLong("release_id"), row.getInt("game_id"), row.getString("region_code"),
                row.getString("play_version"), row.getLong("index_generation"),
                row.getString("rule_hash"), row.getString("component_hash"),
                row.getString("card_codec_version"), row.getString("event_interpreter_version"),
                row.getString("protocol_version"), row.getString("snapshot_schema_version"),
                row.getString("source_hash"));
    }

    private static void requireHash(String value, String code) {
        if (value == null || !value.matches(SHA256)) throw new IllegalArgumentException(code);
    }

    private static void requireVersion(String value, String code) {
        if (value == null || !value.matches(VERSION)) throw new IllegalArgumentException(code);
    }
}
