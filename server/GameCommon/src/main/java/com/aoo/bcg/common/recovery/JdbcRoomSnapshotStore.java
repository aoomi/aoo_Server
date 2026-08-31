package com.aoo.bcg.common.recovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import com.aoo.bcg.common.serialization.DomainJsonDecoder;

public final class JdbcRoomSnapshotStore implements RoomSnapshotStore {
    private static final TypeReference<Map<String,Object>> STATE_TYPE = new TypeReference<>() {};
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final DomainJsonDecoder decoder;
    public JdbcRoomSnapshotStore(DataSource dataSource, ObjectMapper mapper) { this.dataSource = dataSource; this.mapper = mapper; this.decoder = new DomainJsonDecoder(mapper); }
    @Override public void save(RoomSnapshot snapshot) {
        String sql = "INSERT INTO aoo_room_snapshot(room_id,game_id,play_version,component_version,fencing_token,state_version,last_event_sequence,captured_at,state_payload) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),component_version=VALUES(component_version),fencing_token=VALUES(fencing_token),state_version=VALUES(state_version),captured_at=VALUES(captured_at),state_payload=VALUES(state_payload),last_event_sequence=VALUES(last_event_sequence)";
        try (var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var current=connection.prepareStatement("SELECT fencing_token,state_version,last_event_sequence FROM aoo_room_snapshot WHERE room_id=? FOR UPDATE")) {
                    current.setLong(1,snapshot.roomId());
                    try(var result=current.executeQuery()) {
                        if (result.next()) {
                            if (snapshot.fencingToken() < result.getLong(1))
                                throw new IllegalStateException("snapshot fencing token regression");
                            if (snapshot.fencingToken() == result.getLong(1)
                                    && (snapshot.stateVersion() < result.getLong(2)||snapshot.lastEventSequence() < result.getLong(3)))
                                throw new IllegalStateException("snapshot sequence regression");
                        }
                    }
                }
                try (var statement=connection.prepareStatement(sql)) {
                    statement.setLong(1,snapshot.roomId()); statement.setInt(2,snapshot.gameId()); statement.setString(3,snapshot.playVersion()); statement.setString(4,snapshot.componentVersion()); statement.setLong(5,snapshot.fencingToken());statement.setLong(6,snapshot.stateVersion()); statement.setLong(7,snapshot.lastEventSequence()); statement.setTimestamp(8,Timestamp.from(snapshot.capturedAt())); statement.setString(9,mapper.writeValueAsString(snapshot.authoritativeState())); statement.executeUpdate();
                }
                connection.commit();
            } catch (RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) { throw new IllegalStateException("cannot save room snapshot", exception); }
    }
    @Override public Optional<RoomSnapshot> latest(long roomId) {
        try (var connection=dataSource.getConnection(); var statement=connection.prepareStatement("SELECT game_id,play_version,component_version,fencing_token,state_version,last_event_sequence,captured_at,state_payload FROM aoo_room_snapshot WHERE room_id=?")) {
            statement.setLong(1,roomId); try(var result=statement.executeQuery()) { if(!result.next()) return Optional.empty(); return Optional.of(new RoomSnapshot(roomId,result.getInt(1),result.getString(2),result.getString(3),result.getLong(4),result.getLong(5),result.getLong(6),result.getTimestamp(7).toInstant(),decoder.decode(result.getString(8),STATE_TYPE,DomainJsonDecoder::requireDocument))); }
        } catch (Exception exception) { throw new IllegalStateException("cannot load room snapshot", exception); }
    }
    @Override public List<RoomSnapshot> recoverable(Instant now, int limit) {
        if (now == null || limit < 1 || limit > 10000) throw new IllegalArgumentException("invalid recovery query");
        String sql = "SELECT s.room_id,s.game_id,s.play_version,s.component_version,s.fencing_token,s.state_version,s.last_event_sequence,s.captured_at,s.state_payload FROM aoo_room_snapshot s LEFT JOIN aoo_room_lease l ON l.room_id=s.room_id WHERE l.room_id IS NULL OR l.expires_at<=? ORDER BY s.captured_at LIMIT ?";
        try (var connection=dataSource.getConnection(); var statement=connection.prepareStatement(sql)) {
            statement.setTimestamp(1,Timestamp.from(now)); statement.setInt(2,limit);
            try(var result=statement.executeQuery()) { List<RoomSnapshot> values=new ArrayList<>(); while(result.next()) values.add(new RoomSnapshot(result.getLong(1),result.getInt(2),result.getString(3),result.getString(4),result.getLong(5),result.getLong(6),result.getLong(7),result.getTimestamp(8).toInstant(),decoder.decode(result.getString(9),STATE_TYPE,DomainJsonDecoder::requireDocument))); return List.copyOf(values); }
        } catch(Exception exception) { throw new IllegalStateException("cannot list recoverable room snapshots",exception); }
    }
}
