package com.aoo.bcg.bootstrap;

import com.aoo.bcg.common.idempotency.IdempotencyKey;
import com.aoo.bcg.common.idempotency.JdbcIdempotencyStore;
import com.aoo.bcg.common.recovery.JdbcRoomSnapshotStore;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.common.settlement.SettlementResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.List;

/** Atomically persists the authoritative room snapshot and the command result. */
final class JdbcGatewayGameCommandCommitter implements GameCommandCommitter {
    private final DataSource dataSource;
    private final ObjectMapper json;
    private final Clock clock;
    private final Duration retention;
    private final JdbcIdempotencyStore<GameCommandResult> results;
    private final JdbcRoomSnapshotStore snapshots;
    private final DurableGameSettlementService settlements;

    JdbcGatewayGameCommandCommitter(DataSource dataSource, ObjectMapper json, Clock clock,
                                    Duration retention, DurableGameSettlementService settlements) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.json = Objects.requireNonNull(json);
        this.clock = Objects.requireNonNull(clock);
        this.retention = Objects.requireNonNull(retention);
        this.settlements = Objects.requireNonNull(settlements);
        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("command retention must be positive");
        }
        this.results = new JdbcIdempotencyStore<>(dataSource, json, GameCommandResult.class, clock);
        this.snapshots = new JdbcRoomSnapshotStore(dataSource, json);
    }

    @Override
    public void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result) {
        Objects.requireNonNull(room);
        Objects.requireNonNull(request);
        Objects.requireNonNull(result);
        IdempotencyKey key = key(request);
        var authority = room.requireAuthoritativeSession();
        long stateVersion = authority.stateVersion();
        Map<String, Object> state = authority.authoritativeState();
        SettlementResult pendingSettlement = pendingSettlement(room, state);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Metadata metadata = metadata(connection, room.roomId(), true);
                lockProcessingResult(connection, key);
                upsertSnapshot(connection, room, metadata, stateVersion, state);
                recordReplay(connection, room, request, result, metadata, stateVersion, state);
                if (pendingSettlement != null) enqueueSettlement(connection, pendingSettlement);
                completeResult(connection, key, result);
                connection.commit();
                if (pendingSettlement != null) recoverPendingSettlements(1);
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot atomically commit room command", failure);
        }
    }

    int recoverPendingSettlements(int limit) {
        int completed = 0;
        for (int index = 0; index < Math.max(1, Math.min(limit, 256)); index++) {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement select = connection.prepareStatement("SELECT business_id,result_payload FROM aoo_settlement_outbox WHERE status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP(3) ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED")) {
                    try (ResultSet row = select.executeQuery()) {
                        if (!row.next()) { connection.rollback(); break; }
                        String businessId = row.getString(1);
                        SettlementResult settlement = json.readValue(row.getString(2), SettlementResult.class);
                        try {
                            settlements.persist(settlement);
                            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM aoo_settlement_outbox WHERE business_id=?")) {
                                delete.setString(1, businessId);delete.executeUpdate();
                            }
                            connection.commit();completed++;
                        } catch (RuntimeException failure) {
                            try (PreparedStatement update = connection.prepareStatement("UPDATE aoo_settlement_outbox SET attempts=attempts+1,last_error=?,next_attempt_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL LEAST(60,POW(2,LEAST(attempts,5))) SECOND),updated_at=CURRENT_TIMESTAMP(3) WHERE business_id=?")) {
                                update.setString(1, safeError(failure));update.setString(2, businessId);update.executeUpdate();
                            }
                            connection.commit();
                        }
                    }
                } catch (Exception failure) { connection.rollback(); throw failure; }
            } catch (Exception failure) { throw new IllegalStateException("cannot recover settlement outbox", failure); }
        }
        return completed;
    }

    @Override
    public Optional<GameCommandResult> findCommitted(GameRoomHandle room, GameCommandRequest request) {
        return results.find(key(request));
    }

    Optional<RoomSnapshot> latest(long roomId) {
        return snapshots.latest(roomId);
    }

    String hallState(long roomId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT state FROM aoo_hall_room WHERE room_id=?")) {
            statement.setLong(1, roomId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("hall room missing");
                return result.getString(1);
            }
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot load hall room state", failure);
        }
    }

    void saveSnapshot(GameRoomHandle room, long fencingToken) {
        var authority = room.requireAuthoritativeSession();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Metadata metadata = metadata(connection, room.roomId(), false);
                if (metadata.fencingToken() != fencingToken) {
                    throw new SecurityException("snapshot fencing token is stale");
                }
                upsertSnapshot(connection, room, metadata, authority.stateVersion(),
                        authority.authoritativeState());
                connection.commit();
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot save authority snapshot", failure);
        }
    }

    private Metadata metadata(Connection connection, long roomId, boolean requireActive) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT a.fencing_token,a.lifecycle_state,h.release_id
                FROM aoo_room_authority_route a
                JOIN aoo_hall_room h ON h.room_id=a.room_id
                WHERE a.room_id=? FOR UPDATE
                """)) {
            statement.setLong(1, roomId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("room authority metadata missing");
                String lifecycle = result.getString(2);
                if (requireActive && !"ACTIVE".equals(lifecycle)) {
                    throw new SecurityException("room authority is not active");
                }
                if (!requireActive && !("CREATING".equals(lifecycle) || "RECOVERING".equals(lifecycle)
                        || "ACTIVE".equals(lifecycle))) {
                    throw new SecurityException("room authority cannot persist a snapshot");
                }
                return new Metadata(result.getLong(1), result.getLong(3));
            }
        }
    }

    private void lockProcessingResult(Connection connection, IdempotencyKey key) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT user_id,operation,room_id,round_no,client_request_id,status
                FROM aoo_business_idempotency WHERE request_id=? FOR UPDATE
                """)) {
            statement.setString(1, key.storageKey());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("command idempotency lease missing");
                if (!key.userId().equals(result.getString(1))
                        || !key.operation().equals(result.getString(2))
                        || key.roomId() != result.getLong(3)
                        || key.roundNo() != result.getInt(4)
                        || !key.requestId().equals(result.getString(5))) {
                    throw new SecurityException("command idempotency scope conflict");
                }
                if (!"PROCESSING".equals(result.getString(6))) {
                    throw new IllegalStateException("command result was already committed");
                }
            }
        }
    }

    private void upsertSnapshot(Connection connection, GameRoomHandle room, Metadata metadata,
                                long stateVersion, Map<String, Object> state) throws Exception {
        if (stateVersion < 0) throw new IllegalStateException("negative authority stateVersion");
        try (PreparedStatement current = connection.prepareStatement("""
                SELECT fencing_token,state_version FROM aoo_room_snapshot WHERE room_id=? FOR UPDATE
                """)) {
            current.setLong(1, room.roomId());
            try (ResultSet result = current.executeQuery()) {
                if (result.next() && (metadata.fencingToken() < result.getLong(1)
                        || (metadata.fencingToken() == result.getLong(1)
                        && stateVersion < result.getLong(2)))) {
                    throw new SecurityException("room snapshot would regress");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO aoo_room_snapshot(
                  room_id,game_id,play_version,release_id,component_version,fencing_token,
                  state_version,last_event_sequence,captured_at,state_payload)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),
                  release_id=VALUES(release_id),component_version=VALUES(component_version),
                  fencing_token=VALUES(fencing_token),state_version=VALUES(state_version),
                  last_event_sequence=VALUES(last_event_sequence),captured_at=VALUES(captured_at),
                  state_payload=VALUES(state_payload)
                """)) {
            int index = 1;
            statement.setLong(index++, room.roomId());
            statement.setInt(index++, room.gameId());
            statement.setString(index++, room.playVersion());
            statement.setLong(index++, metadata.releaseId());
            statement.setString(index++, room.playVersion());
            statement.setLong(index++, metadata.fencingToken());
            statement.setLong(index++, stateVersion);
            statement.setLong(index++, stateVersion);
            statement.setTimestamp(index++, Timestamp.from(clock.instant()));
            statement.setString(index, json.writeValueAsString(state));
            statement.executeUpdate();
        }
    }

    private void completeResult(Connection connection, IdempotencyKey key,
                                GameCommandResult result) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE aoo_business_idempotency
                SET status='COMPLETED',response_code=0,response_version='v1',
                    response_schema_version=1,response_payload=?,expires_at=?
                WHERE request_id=? AND status='PROCESSING'
                """)) {
            statement.setString(1, json.writeValueAsString(result));
            statement.setTimestamp(2, Timestamp.from(clock.instant().plus(retention)));
            statement.setString(3, key.storageKey());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("command idempotency completion lost");
            }
        }
    }

    private SettlementResult pendingSettlement(GameRoomHandle room, Map<String,Object> state) {
        Object round = state.get("roundNo");
        if (!Boolean.TRUE.equals(state.get("roundScored")) || !(round instanceof Number number)
                || number.intValue() <= 0) return null;
        return settlements.prepare(room, number.intValue());
    }

    private void enqueueSettlement(Connection connection, SettlementResult result) throws Exception {
        String businessId = "round:" + result.roomId() + ':' + result.roundNo() + ':' + result.playVersion();
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO aoo_settlement_outbox(business_id,room_id,round_no,play_version,result_payload,status,attempts,next_attempt_at,created_at,updated_at) VALUES(?,?,?,?,?,'PENDING',0,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE business_id=business_id")) {
            statement.setString(1,businessId);statement.setLong(2,result.roomId());statement.setInt(3,result.roundNo());statement.setString(4,result.playVersion());statement.setString(5,json.writeValueAsString(result));statement.executeUpdate();
        }
    }

    private void recordReplay(Connection connection, GameRoomHandle room, GameCommandRequest request,
                              GameCommandResult result, Metadata metadata, long stateVersion,
                              Map<String,Object> state) throws Exception {
        if (stateVersion <= 0 || !replayMutation(request)) return;
        int setId = Math.max(0, request.roundNo());
        Map<Integer,Long> players = players(state.get("players"));
        try (PreparedStatement participant = connection.prepareStatement("INSERT IGNORE INTO replay_participant(room_id,set_id,player_id,seat_id,granted_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3))")) {
            for (var entry : players.entrySet()) { participant.setLong(1,room.roomId());participant.setInt(2,setId);participant.setLong(3,entry.getValue());participant.setInt(4,entry.getKey());participant.addBatch(); }
            participant.executeBatch();
        }
        Map<String,Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("msgId",request.msgId());publicPayload.put("seatId",request.seatId());
        publicPayload.put("action",replayAction(request));
        Object cards = replayCards(request);if(cards!=null)publicPayload.put("cards",cards);
        insertReplay(connection,room,setId,stateVersion,"PUBLIC",0,request.msgId(),publicPayload,metadata.releaseId());
        long actor = Long.parseLong(request.authenticatedUserId());
        insertReplay(connection,room,setId,stateVersion,"PLAYER_PRIVATE",actor,result.msgId(),
                room.requireAuthoritativeSession().viewFor(actor),metadata.releaseId());
        if (Boolean.TRUE.equals(state.get("roundScored"))) upsertReplayManifest(connection,room,setId);
    }

    private void insertReplay(Connection connection,GameRoomHandle room,int setId,long sequence,
                              String visibility,long owner,String messageId,Object payload,long releaseId)throws Exception{
        try(PreparedStatement statement=connection.prepareStatement("INSERT INTO perspective_replay_event(room_id,set_id,event_sequence,visibility,owner_player_id,message_id,schema_version,play_version,release_id,card_codec_version,event_interpreter_version,protocol_version,payload) VALUES(?,?,?,?,?,?,1,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE message_id=VALUES(message_id),payload=VALUES(payload),release_id=VALUES(release_id)")){
            statement.setLong(1,room.roomId());statement.setInt(2,setId);statement.setLong(3,sequence);statement.setString(4,visibility);statement.setLong(5,owner);statement.setString(6,messageId);statement.setString(7,room.playVersion());statement.setLong(8,releaseId);statement.setString(9,"poker-card-v1");statement.setString(10,"authority-v1");statement.setString(11,"2.0");statement.setBytes(12,json.writeValueAsBytes(payload));statement.executeUpdate();
        }
    }

    private void upsertReplayManifest(Connection connection,GameRoomHandle room,int setId)throws Exception{
        var digest=java.security.MessageDigest.getInstance("SHA-256");long count=0;
        try(PreparedStatement query=connection.prepareStatement("SELECT content_hash FROM perspective_replay_event WHERE room_id=? AND set_id=? ORDER BY event_sequence,visibility,owner_player_id")){
            query.setLong(1,room.roomId());query.setInt(2,setId);try(ResultSet rows=query.executeQuery()){while(rows.next()){digest.update(rows.getString(1).getBytes(java.nio.charset.StandardCharsets.US_ASCII));count++;}}
        }
        String hash=java.util.HexFormat.of().formatHex(digest.digest());
        try(PreparedStatement statement=connection.prepareStatement("INSERT INTO replay_set_manifest(room_id,set_id,play_version,schema_version,event_count,content_hash,closed_at,archive_after,delete_after,legal_hold) VALUES(?,?,?,1,?,?,CURRENT_TIMESTAMP(3),DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 180 DAY),DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 730 DAY),FALSE) ON DUPLICATE KEY UPDATE event_count=VALUES(event_count),content_hash=VALUES(content_hash),closed_at=VALUES(closed_at),archive_after=VALUES(archive_after),delete_after=VALUES(delete_after)")){
            statement.setLong(1,room.roomId());statement.setInt(2,setId);statement.setString(3,room.playVersion());statement.setLong(4,count);statement.setString(5,hash);statement.executeUpdate();
        }
    }

    private static Map<Integer,Long> players(Object raw){Map<Integer,Long> result=new LinkedHashMap<>();if(raw instanceof Map<?,?> map)for(var entry:map.entrySet())result.put(Integer.parseInt(String.valueOf(entry.getKey())),Long.parseLong(String.valueOf(entry.getValue())));return result;}
    private static boolean replayMutation(GameCommandRequest request){String action=replayAction(request);return !List.of("state","hint","CNJPDKGetRoomInfo","CNJPDKContinueEnterRoom").contains(action);}
    private static String replayAction(GameCommandRequest request){Object action=request.body().get("action");return action==null?request.msgId():String.valueOf(action);}
    private static Object replayCards(GameCommandRequest request){Object payload=request.body().get("payload");if(payload instanceof Map<?,?> map){Object cards=map.get("cards");if(cards==null)cards=map.get("cardList");return cards;}Object cards=request.body().get("cards");return cards==null?request.body().get("cardList"):cards;}
    private static String safeError(Throwable failure){String value=failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());return value.length()>1000?value.substring(0,1000):value;}

    private static IdempotencyKey key(GameCommandRequest request) {
        return new IdempotencyKey(request.authenticatedUserId(), request.msgId(), request.roomId(),
                request.roundNo(), request.requestId());
    }

    private record Metadata(long fencingToken, long releaseId) { }
}
