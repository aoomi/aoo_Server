package com.aoo.bcg.common.recovery;

import com.aoo.bcg.common.serialization.DomainJsonDecoder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** JDBC snapshot store with one immutable sidecar contract for every durable write. */
public final class JdbcRoomSnapshotStore implements RoomSnapshotStore {
    private static final TypeReference<Map<String, Object>> STATE_TYPE = new TypeReference<>() { };
    private static final String SNAPSHOT_SELECT = """
            SELECT s.room_id,s.game_id,s.play_version,s.component_version,
                   s.release_id,s.rule_content_hash,s.component_content_hash,
                   s.card_codec_version,s.event_interpreter_version,s.protocol_version,
                   s.fencing_token,s.state_version,s.last_event_sequence,s.captured_at,s.state_payload,
                   l.index_generation,l.region_code,l.source_hash,l.snapshot_schema_version
            FROM aoo_room_snapshot s
            LEFT JOIN aoo_room_rule_lock l ON l.room_id=s.room_id
            """;

    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final DomainJsonDecoder decoder;

    public JdbcRoomSnapshotStore(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.mapper = Objects.requireNonNull(mapper);
        this.decoder = new DomainJsonDecoder(mapper);
    }

    @Override
    public void save(RoomSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot").metadata().requireSnapshotColumns();
        String sql = """
                INSERT INTO aoo_room_snapshot(
                  room_id,game_id,play_version,release_id,component_version,
                  rule_content_hash,component_content_hash,card_codec_version,
                  event_interpreter_version,protocol_version,fencing_token,state_version,
                  last_event_sequence,captured_at,state_payload)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),
                  release_id=VALUES(release_id),component_version=VALUES(component_version),
                  rule_content_hash=VALUES(rule_content_hash),component_content_hash=VALUES(component_content_hash),
                  card_codec_version=VALUES(card_codec_version),event_interpreter_version=VALUES(event_interpreter_version),
                  protocol_version=VALUES(protocol_version),fencing_token=VALUES(fencing_token),
                  state_version=VALUES(state_version),last_event_sequence=VALUES(last_event_sequence),
                  captured_at=VALUES(captured_at),state_payload=VALUES(state_payload)
                """;
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var current = connection.prepareStatement(
                        "SELECT fencing_token,state_version,last_event_sequence FROM aoo_room_snapshot WHERE room_id=? FOR UPDATE")) {
                    current.setLong(1, snapshot.roomId());
                    try (var result = current.executeQuery()) {
                        if (result.next()) {
                            if (snapshot.fencingToken() < result.getLong(1)
                                    || (snapshot.fencingToken() == result.getLong(1)
                                    && (snapshot.stateVersion() < result.getLong(2)
                                    || snapshot.lastEventSequence() < result.getLong(3)))) {
                                throw new IllegalStateException("snapshot sequence or fencing regression");
                            }
                        }
                    }
                }
                try (var statement = connection.prepareStatement(sql)) {
                    bindSnapshot(statement, snapshot);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("cannot save room snapshot", exception);
            }
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot save room snapshot", exception);
        }
    }

    @Override
    public Optional<RoomSnapshot> latest(long roomId) {
        String sql = SNAPSHOT_SELECT + " WHERE s.room_id=?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(readSnapshot(result)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("cannot load room snapshot", exception);
        }
    }

    @Override
    public List<RoomSnapshot> recoverable(Instant now, int limit) {
        if (now == null || limit < 1 || limit > 10000) throw new IllegalArgumentException("invalid recovery query");
        String sql = SNAPSHOT_SELECT
                + " LEFT JOIN aoo_room_lease lease ON lease.room_id=s.room_id"
                + " WHERE lease.room_id IS NULL OR lease.expires_at<=? ORDER BY s.captured_at LIMIT ?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setInt(2, limit);
            try (var result = statement.executeQuery()) {
                List<RoomSnapshot> values = new ArrayList<>();
                while (result.next()) values.add(readSnapshot(result));
                return List.copyOf(values);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("cannot list recoverable room snapshots", exception);
        }
    }

    private RoomSnapshot readSnapshot(ResultSet result) throws Exception {
        long roomId = result.getLong(1);
        return new RoomSnapshot(roomId, result.getInt(2), result.getString(3), result.getString(4),
                result.getLong(11), result.getLong(12), result.getLong(13),
                result.getTimestamp(14).toInstant(),
                decoder.decode(result.getString(15), STATE_TYPE, DomainJsonDecoder::requireDocument),
                metadata(result));
    }

    private static RoomSnapshotMetadata metadata(ResultSet result) throws Exception {
        return new RoomSnapshotMetadata(
                nullableLong(result, 5), result.getString(6), result.getString(7),
                result.getString(8), result.getString(9), result.getString(10),
                result.getString(19), nullableLong(result, 16), result.getString(17), result.getString(18));
    }

    private static Long nullableLong(ResultSet result, int index) throws Exception {
        long value = result.getLong(index);
        return result.wasNull() ? null : value;
    }

    private void bindSnapshot(java.sql.PreparedStatement statement, RoomSnapshot snapshot)
            throws Exception {
        RoomSnapshotMetadata metadata = snapshot.metadata();
        RoomSnapshotMetadata.SnapshotColumns columns = metadata.requireSnapshotColumns();
        int index = 1;
        statement.setLong(index++, snapshot.roomId());
        statement.setInt(index++, snapshot.gameId());
        statement.setString(index++, snapshot.playVersion());
        statement.setLong(index++, columns.releaseId());
        statement.setString(index++, snapshot.componentVersion());
        statement.setString(index++, columns.ruleContentHash());
        statement.setString(index++, columns.componentContentHash());
        statement.setString(index++, columns.cardCodecVersion());
        statement.setString(index++, columns.eventInterpreterVersion());
        statement.setString(index++, columns.protocolVersion());
        statement.setLong(index++, snapshot.fencingToken());
        statement.setLong(index++, snapshot.stateVersion());
        statement.setLong(index++, snapshot.lastEventSequence());
        statement.setTimestamp(index++, Timestamp.from(snapshot.capturedAt()));
        statement.setString(index, mapper.writeValueAsString(snapshot.authoritativeState()));
    }
}
