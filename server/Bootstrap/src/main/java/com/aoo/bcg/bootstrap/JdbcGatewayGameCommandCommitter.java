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
import java.sql.SQLException;
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
    private final core.replay.ReplayCodeRepository replayCodeRepository;

    JdbcGatewayGameCommandCommitter(DataSource dataSource, ObjectMapper json, Clock clock,
                                    Duration retention, DurableGameSettlementService settlements) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.json = Objects.requireNonNull(json);
        this.clock = Objects.requireNonNull(clock);
        this.retention = Objects.requireNonNull(retention);
        this.settlements = Objects.requireNonNull(settlements);
        this.replayCodeRepository = new core.replay.JdbcReplayCodeRepository(dataSource);
        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("command retention must be positive");
        }
        this.results = new JdbcIdempotencyStore<>(dataSource, json, GameCommandResult.class, clock);
        this.snapshots = new JdbcRoomSnapshotStore(dataSource, json);
    }

    @Override
    public void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result) {
        commitResult(room, request, result);
    }

    @Override
    public GameCommandResult commitResult(GameRoomHandle room, GameCommandRequest request,
                                          GameCommandResult result) {
        Objects.requireNonNull(room);
        Objects.requireNonNull(request);
        Objects.requireNonNull(result);
        IdempotencyKey key = key(request);
        var authority = room.requireAuthoritativeSession();
        long stateVersion = authority.stateVersion();
        Map<String, Object> state = authority.authoritativeState();
        SettlementResult pendingSettlement = pendingSettlement(room, state);
        boolean replayMutation = replayMutation(request);
        // Settlement roundNo is one-based while replay setId is zero-based. A reconnect or
        // recovery command carries the current authoritative round number, so deriving setId
        // from request.roundNo incorrectly rejects a valid completed round (for example 1 != 0).
        int completedReplaySetId = pendingSettlement == null
                ? Math.max(0, request.roundNo())
                : pendingSettlement.roundNo() - 1;
        GameCommandResult durableResult = result;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Metadata metadata = metadata(connection, room.roomId(), true);
                lockProcessingResult(connection, key);
                markFirstRoundStarted(connection,room.roomId(),state);
                upsertSnapshot(connection, room, metadata, stateVersion, state);
                recordReplay(connection, room, request, result, metadata, stateVersion, state);
                if (pendingSettlement != null) enqueueSettlement(connection, pendingSettlement);
                if (pendingSettlement != null && replayMutation) {
                    // The replay manifest and code mapping share this transaction. Therefore a
                    // FINISHED response can never become visible without its exact round code.
                    var mapping = new core.replay.ReplayCodeService(
                            new TransactionReplayCodeRepository(connection))
                            .allocate(pendingSettlement.roomId(), completedReplaySetId);
                    durableResult = withReplayCode(result, mapping, pendingSettlement.roundNo());
                }
                completeResult(connection, key, durableResult);
                connection.commit();
                if (pendingSettlement != null) {
                    recoverPendingSettlements(1);
                }
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot atomically commit room command", failure);
        }
        return durableResult;
    }

    Map<String,Object> decorateReplayCode(Map<String,Object> perspective) {
        Object rawRound = perspective.get("roundNo");
        String phase = String.valueOf(perspective.getOrDefault("phase", ""));
        if (!(rawRound instanceof Number number) || number.intValue() <= 0
                || !java.util.Set.of("FINISHED","ROUND_SETTLEMENT","INTER_ROUND","SETTLED","DIRECT_WIN").contains(phase)) {
            return perspective;
        }
        int setId = number.intValue() - 1;
        long roomId = ((Number) perspective.get("roomId")).longValue();
        var mapping = replayCodeRepository.findByTarget(roomId,setId).orElse(null);
        if (mapping == null) return perspective;
        Map<String,Object> decorated = new LinkedHashMap<>(perspective);
        decorated.put("replayCode",mapping.code());
        decorated.put("replaySetId",mapping.setId());
        return Map.copyOf(decorated);
    }

    private static GameCommandResult withReplayCode(GameCommandResult result,
                                                     core.replay.ReplayCodeRepository.Mapping mapping,
                                                     int roundNo) {
        Map<String,Object> root = new LinkedHashMap<>(result.body().asMap());
        Object nested = root.get("payload");
        Map<String,Object> authority;
        if (nested instanceof com.aoo.bcg.gamespi.CommandPayload payload) {
            authority = new LinkedHashMap<>(payload.asMap());
            root.put("payload", authority);
        } else if (nested instanceof Map<?,?> map) {
            authority = new LinkedHashMap<>();
            map.forEach((key,value) -> authority.put(String.valueOf(key),value));
            root.put("payload", authority);
        } else {
            authority = root;
        }
        authority.put("replayCode", mapping.code());
        authority.put("replaySetId", mapping.setId());
        authority.put("roundNo", roundNo);
        return new GameCommandResult(result.msgId(), result.requestId(),
                com.aoo.bcg.gamespi.CommandPayload.copyOf(root), result.serverTimeEpochMillis(),
                result.operationDeadline());
    }

    private void markFirstRoundStarted(Connection connection,long roomId,Map<String,Object> state)throws Exception{
        if(!firstRoundStarted(state))return;
        try(PreparedStatement statement=connection.prepareStatement("UPDATE aoo_room_authority_route SET first_round_started_at=COALESCE(first_round_started_at,CURRENT_TIMESTAMP(3)),updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND lifecycle_state='ACTIVE' AND (first_round_started_at IS NOT NULL OR created_at>DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 300 SECOND))")){
            statement.setLong(1,roomId);
            if(statement.executeUpdate()!=1)throw new SecurityException("房间已到期，首局开始未获得权威状态");
        }
    }

    private static boolean firstRoundStarted(Map<String,Object> state){
        for(String key:List.of("roundNo","setID","setId")){Object value=state.get(key);if(value instanceof Number number&&number.longValue()>0)return true;}
        if(Boolean.TRUE.equals(state.get("started")))return true;
        String phase=String.valueOf(state.getOrDefault("phase","")).strip().toUpperCase(java.util.Locale.ROOT);
        return List.of("PLAYING","IN_GAME","ROUND_PLAYING","DEALING").contains(phase);
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
    private static boolean replayMutation(GameCommandRequest request){
        String action=replayAction(request).strip().toLowerCase(java.util.Locale.ROOT);
        String msgId=request.msgId().strip().toLowerCase(java.util.Locale.ROOT);
        if(List.of("state","hint","reconnect").contains(action))return false;
        return !(msgId.endsWith(".state_req")||msgId.endsWith(".hint_req")||msgId.endsWith(".reconnect_req"));
    }
    private static String replayAction(GameCommandRequest request){Object action=request.body().get("action");return action==null?request.msgId():String.valueOf(action);}
    private static Object replayCards(GameCommandRequest request){Object payload=request.body().get("payload");if(payload instanceof Map<?,?> map){Object cards=map.get("cards");if(cards==null)cards=map.get("cardList");return cards;}Object cards=request.body().get("cards");return cards==null?request.body().get("cardList"):cards;}
    private static String safeError(Throwable failure){String value=failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());return value.length()>1000?value.substring(0,1000):value;}

    /** Replay-code repository bound to the command transaction; it never closes the connection. */
    private static final class TransactionReplayCodeRepository implements core.replay.ReplayCodeRepository {
        private final Connection connection;
        private TransactionReplayCodeRepository(Connection connection) { this.connection = connection; }

        @Override public Optional<Mapping> findShortCode(String code) {
            try (PreparedStatement q=connection.prepareStatement("SELECT short_code,room_id,set_id,status,expires_at FROM replay_short_code WHERE short_code=?")) {
                q.setString(1,code);try(ResultSet r=q.executeQuery()){return r.next()?Optional.of(mapping(r)):Optional.empty();}
            } catch(Exception e){throw failure("find transactional replay code",e);}
        }
        @Override public Optional<Mapping> findByTarget(long roomId,int setId) {
            try (PreparedStatement q=connection.prepareStatement("SELECT short_code,room_id,set_id,status,expires_at FROM replay_short_code WHERE room_id=? AND set_id=?")) {
                q.setLong(1,roomId);q.setInt(2,setId);try(ResultSet r=q.executeQuery()){return r.next()?Optional.of(mapping(r)):Optional.empty();}
            } catch(Exception e){throw failure("find transactional replay target",e);}
        }
        @Override public boolean replayReady(long roomId,int setId) {
            try (PreparedStatement q=connection.prepareStatement("SELECT 1 FROM replay_set_manifest WHERE room_id=? AND set_id=? LIMIT 1")) {
                q.setLong(1,roomId);q.setInt(2,setId);try(ResultSet r=q.executeQuery()){return r.next();}
            } catch(Exception e){throw failure("check transactional replay readiness",e);}
        }
        @Override public long allocatedCount(int length) {
            try (PreparedStatement q=connection.prepareStatement("SELECT COUNT(*) FROM replay_short_code WHERE code_length=?")) {
                q.setInt(1,length);try(ResultSet r=q.executeQuery()){r.next();return r.getLong(1);}
            } catch(Exception e){throw failure("count transactional replay codes",e);}
        }
        @Override public InsertResult insert(String code,long roomId,int setId) {
            try (PreparedStatement q=connection.prepareStatement("INSERT INTO replay_short_code(short_code,code_length,room_id,set_id,status,created_at) VALUES(?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP(3))")) {
                q.setString(1,code);q.setInt(2,code.length());q.setLong(3,roomId);q.setInt(4,setId);q.executeUpdate();return InsertResult.INSERTED;
            } catch(SQLException e){
                if(!"23000".equals(e.getSQLState()))throw failure("insert transactional replay code",e);
                return findByTarget(roomId,setId).isPresent()?InsertResult.TARGET_EXISTS:InsertResult.CODE_COLLISION;
            } catch(Exception e){throw failure("insert transactional replay code",e);}
        }
        @Override public Optional<LegacyTarget> findLegacyCode(String code){throw new UnsupportedOperationException();}
        @Override public boolean mayView(long playerId,long roomId,int setId){throw new UnsupportedOperationException();}
        @Override public void grantCodeAccess(String code,long playerId){throw new UnsupportedOperationException();}
        private static Mapping mapping(ResultSet r)throws SQLException{
            Timestamp expiry=r.getTimestamp(5);
            return new Mapping(r.getString(1),r.getLong(2),r.getInt(3),r.getString(4),expiry==null?null:expiry.toInstant());
        }
        private static IllegalStateException failure(String action,Exception error){return new IllegalStateException("cannot "+action,error);}
    }

    private static IdempotencyKey key(GameCommandRequest request) {
        return new IdempotencyKey(request.authenticatedUserId(), request.msgId(), request.roomId(),
                request.roundNo(), request.requestId());
    }

    private record Metadata(long fencingToken, long releaseId) { }
}
