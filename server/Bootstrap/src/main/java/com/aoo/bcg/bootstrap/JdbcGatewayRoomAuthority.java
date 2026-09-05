package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gateway.GatewayRuntimeProvider;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.gateway.GatewayRoomBroadcastHub;
import com.aoo.bcg.gamespi.RoomLifecycleAuthority;
import com.aoo.bcg.gateway.WaitingRoomExpirationPolicy;
import com.aoo.bcg.gateway.RoomAuthorityBusinessError;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Persistent, fenced owner of game-room runtime instances. */
final class JdbcGatewayRoomAuthority implements GatewayRuntimeProvider.RoomAuthority, AutoCloseable {
    private final DataSource dataSource;
    private final GameRegistry games;
    private final RuntimeGameRoomRegistry rooms;
    private final String nodeId;
    private final String endpoint;
    private final Clock clock;
    private final ObjectMapper json;
    private final JdbcGatewayGameCommandCommitter stateStore;
    private final GatewayRoomBroadcastHub broadcasts;
    private final HallRoomLifecycleClient hallLifecycle;
    private final ConcurrentHashMap<Long, Object> roomLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService leaseKeeper;

    JdbcGatewayRoomAuthority(DataSource dataSource, GameRegistry games, RuntimeGameRoomRegistry rooms,
                             Clock clock, String nodeId, String endpoint, ObjectMapper json,
                             JdbcGatewayGameCommandCommitter stateStore,
                             GatewayRoomBroadcastHub broadcasts,
                             HallRoomLifecycleClient hallLifecycle) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.games = Objects.requireNonNull(games);
        this.rooms = Objects.requireNonNull(rooms);
        this.clock = Objects.requireNonNull(clock);
        this.json = Objects.requireNonNull(json);
        this.stateStore = Objects.requireNonNull(stateStore);
        this.broadcasts = Objects.requireNonNull(broadcasts);
        this.hallLifecycle = Objects.requireNonNull(hallLifecycle);
        this.nodeId = required(nodeId, "nodeId");
        this.endpoint = required(endpoint, "endpoint");
        this.leaseKeeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-room-authority-lease");
            thread.setDaemon(true);
            return thread;
        });
        recoverDurableRooms();
        finishInterruptedRemovals();
        this.leaseKeeper.scheduleWithFixedDelay(this::renewActiveLeases, 30, 30, TimeUnit.SECONDS);
        this.leaseKeeper.scheduleWithFixedDelay(this::processRoomLifecycles, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> command) {
        long roomId = positive(command, "roomId");
        synchronized (lock(roomId)) {
            return createLocked(command, roomId);
        }
    }

    @Override
    public Map<String, Object> recover(Map<String, Object> command) {
        long roomId = positive(command, "roomId");
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            verifyIdentity(route, command);
            if ("REMOVED".equals(route.state()) || "REMOVING".equals(route.state())) {
                throw new IllegalStateException("room authority is closed");
            }
            Route claimed = claimForRecovery(route);
            if (claimed == null) throw new SecurityException("room authority is owned by another node");
            try {
                requireOrCreateRuntime(command, claimed);
                transition(roomId, claimed.fencingToken(), "ACTIVE");
                return response(claimed);
            } catch (RuntimeException failure) {
                failRoute(roomId, claimed.fencingToken(), failure);
                throw failure;
            }
        }
    }

@Override
public Map<String, Object> join(Map<String, Object> command) {
    long roomId = positive(command, "roomId");
    long accountId = positive(command, "accountId");
    int seatNo = Math.toIntExact(nonNegative(command, "seatNo"));
    String playVersion = required(command, "playVersion");
    String requestId = required(command, "requestId");
    synchronized (lock(roomId)) {
        Route route = route(roomId);
        if (!"ACTIVE".equals(route.state()) || !nodeId.equals(route.nodeId())
                || !clock.instant().isBefore(route.leaseExpiresAt())) {
            throw new SecurityException("room authority is not active on this node");
        }
        if (!route.playVersion().equals(playVersion)) {
            throw new SecurityException("room membership playVersion mismatch");
        }
        GameRoomHandle room = rooms.require(roomId);
        var session = room.requireAuthoritativeSession();
        if(route.firstRoundStartedAt()==null
                && !clock.instant().isBefore(route.createdAt().plus(WaitingRoomExpirationPolicy.TIMEOUT))) {
            if(WaitingRoomExpirationPolicy.expired(route.createdAt(),clock.instant(),session.authoritativeState()))
                throw expireAndClose(room,route);
        }
        try {
            admit(session,accountId,seatNo,command.get("admission"));
        } catch (IllegalArgumentException | IllegalStateException rejected) {
            if ("invalid admission identity".equals(rejected.getMessage())) {
                throw new RoomAuthorityBusinessError(409,"ROOM_FULL","房间人数已满");
            }
            throw new RoomAuthorityBusinessError(409,"ROOM_JOIN_REJECTED","room rejected membership");
        }
        int roundNo = session.authoritativeState().get("roundNo") instanceof Number number
                ? Math.max(0, number.intValue()) : 0;
        session.execute(new com.aoo.bcg.gamespi.GameCommandRequest("common.room.join_req",
                requestId, Math.max(1L, session.stateVersion() + 1L), roomId, roundNo,
                playVersion, Long.toString(accountId), seatNo, Map.of()));
        stateStore.saveSnapshot(room, route.fencingToken());
        // 加入由 Hall 经内部端口完成，不经过玩家 WSS 的 publish 链。如果这里不主动
        // 广播，已在房内的客户端会永久停留在旧成员快照，进而无法满足“满员后准备”。
        // 广播器按接收者重新生成私有视图，不会泄露其他玩家暗牌。
        broadcasts.publishAuthoritativeState(roomId, requestId,
                String.valueOf(command.getOrDefault("traceId", requestId)), session.stateVersion());
        return Map.of("roomId", roomId, "accountId", accountId, "seatNo", seatNo,
                "playVersion", playVersion, "stateVersion", session.stateVersion());
    }
    }

    @Override
    public void remove(long roomId, long fencingToken) {
        if (roomId <= 0 || fencingToken <= 0) throw new IllegalArgumentException("invalid room removal");
        synchronized (lock(roomId)) {
            Route route = route(roomId);
            if (route.fencingToken() != fencingToken) throw new SecurityException("stale fencing token");
            if ("REMOVED".equals(route.state())) return;
            transition(roomId, fencingToken, "REMOVING");
            rooms.remove(roomId);
            transition(roomId, fencingToken, "REMOVED");
            roomLocks.remove(roomId);
        }
    }

    void completeTerminal(long roomId,String requestId,String traceId,String reason) {
        synchronized(lock(roomId)) {
            GameRoomHandle room;
            try { room=rooms.require(roomId); } catch(IllegalArgumentException alreadyRemoved) { return; }
            if(!(room.requireAuthoritativeSession() instanceof RoomLifecycleAuthority lifecycle)||!lifecycle.isTerminal())throw new IllegalStateException("room lifecycle is not terminal");
            Route route=route(roomId);
            if("REMOVED".equals(route.state())){rooms.remove(roomId);return;}
            if(!nodeId.equals(route.nodeId()))throw new SecurityException("room lifecycle owned by another node");
            if("ACTIVE".equals(route.state()))transition(roomId,route.fencingToken(),"REMOVING");else if(!"REMOVING".equals(route.state()))throw new IllegalStateException("room lifecycle route is not removable: "+route.state());
            hallLifecycle.close(roomId,requestId,traceId,reason);
            rooms.remove(roomId);
            transition(roomId,route.fencingToken(),"REMOVED");
            roomLocks.remove(roomId);
        }
    }

    void completeMemberExit(long roomId,long accountId,String requestId,String traceId) {
        synchronized(lock(roomId)) {
            GameRoomHandle room=rooms.require(roomId);
            Object rawPlayers=room.requireAuthoritativeSession().authoritativeState().get("players");
            if(rawPlayers instanceof Map<?,?> players&&players.values().stream().anyMatch(value->Long.parseLong(String.valueOf(value))==accountId))throw new IllegalStateException("room member is still seated in authority state");
            Route route=route(roomId);
            if(!"ACTIVE".equals(route.state())||!nodeId.equals(route.nodeId()))throw new SecurityException("room membership lifecycle owned by another node");
            hallLifecycle.memberLeft(roomId,accountId,requestId,traceId);
        }
    }

    private void processRoomLifecycles() {
        try{finishInterruptedRemovals();}catch(RuntimeException failure){System.err.println("room removal recovery deferred cause="+failure.getMessage());}
        for(GameRoomHandle room:rooms.snapshot())try {
            if(expireWaitingRoom(room))continue;
            if(!(room.requireAuthoritativeSession() instanceof RoomLifecycleAuthority lifecycle))continue;
            boolean changed=lifecycle.tickLifecycle(clock.instant());
            Route route=route(room.roomId());
            if(changed){long version=room.requireAuthoritativeSession().stateVersion();stateStore.saveSnapshot(room,route.fencingToken());broadcasts.publishAuthoritativeState(room.roomId(),"room-lifecycle-"+room.roomId()+"-"+version,"room-lifecycle-"+room.roomId(),version);}
            boolean durable=stateStore.latest(room.roomId()).map(snapshot->{Object value=snapshot.authoritativeState().get("stateVersion");return value instanceof Number number&&number.longValue()>=room.requireAuthoritativeSession().stateVersion();}).orElse(false);
            if(lifecycle.isTerminal()&&durable)completeTerminal(room.roomId(),"room-lifecycle-"+room.roomId()+"-"+room.requireAuthoritativeSession().stateVersion(),"room-lifecycle-"+room.roomId(),lifecycle.terminalReason());
        } catch(RuntimeException failure){System.err.println("room lifecycle processing deferred roomId="+room.roomId()+" cause="+failure.getMessage());}
    }

    private boolean expireWaitingRoom(GameRoomHandle room){
        synchronized(lock(room.roomId())){
            var session=room.requireAuthoritativeSession();
            synchronized(session){
                Route route=route(room.roomId());
                if(!"ACTIVE".equals(route.state())||!nodeId.equals(route.nodeId())
                        ||route.firstRoundStartedAt()!=null)return false;
                if(!WaitingRoomExpirationPolicy.expired(route.createdAt(),clock.instant(),session.authoritativeState()))return false;
                expireAndClose(room,route);
                return true;
            }
        }
    }

    /** 所有到期入口共用同一持久化关闭链，确保成员、票据和可恢复运行态一起释放。 */
    private RoomAuthorityBusinessError expireAndClose(GameRoomHandle room,Route route) {
        String requestId="waiting-room-expiration-"+room.roomId();
        beginAutomaticRemoval(room.roomId(),route.fencingToken(),requestId);
        broadcasts.publishWaitingRoomExpired(room.roomId(),requestId,requestId);
        hallLifecycle.close(room.roomId(),requestId,requestId,"WAITING_ROOM_EXPIRED");
        rooms.remove(room.roomId());
        finishAutomaticRemoval(room.roomId(),route.fencingToken());
        roomLocks.remove(room.roomId());
        return new RoomAuthorityBusinessError(409,"ROOM_ENDED","房间超过300秒未开始，已自动解散");
    }

    private void beginAutomaticRemoval(long roomId,long fence,String requestId){try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE aoo_room_authority_route SET lifecycle_state='REMOVING',last_request_id=?,trace_id=?,updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND fencing_token=? AND lifecycle_state='ACTIVE' AND first_round_started_at IS NULL AND created_at<=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 300 SECOND)")){p.setString(1,requestId);p.setString(2,requestId);p.setLong(3,roomId);p.setLong(4,fence);if(p.executeUpdate()!=1)throw new SecurityException("waiting room expiration lost authority race");}catch(SQLException e){throw new IllegalStateException("waiting room expiration persistence failed",e);}}

    private void finishAutomaticRemoval(long roomId,long fence){try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try(PreparedStatement route=c.prepareStatement("UPDATE aoo_room_authority_route SET lifecycle_state='REMOVED',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND fencing_token=? AND lifecycle_state='REMOVING'");PreparedStatement snapshot=c.prepareStatement("DELETE FROM aoo_room_snapshot WHERE room_id=?")){route.setLong(1,roomId);route.setLong(2,fence);if(route.executeUpdate()!=1)throw new SecurityException("等待房间解散失去权威状态");snapshot.setLong(1,roomId);snapshot.executeUpdate();c.commit();}catch(Exception failure){c.rollback();throw failure;}}catch(Exception failure){if(failure instanceof RuntimeException runtime)throw runtime;throw new IllegalStateException("等待房间可恢复状态清理失败",failure);}}

    private void finishInterruptedRemovals() {
        List<Object[]> pending=new ArrayList<>();
        try(Connection connection=dataSource.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT room_id,fencing_token,last_request_id FROM aoo_room_authority_route WHERE node_id=? AND lifecycle_state='REMOVING' ORDER BY room_id")) {
            statement.setString(1,nodeId);
            try(ResultSet result=statement.executeQuery()){while(result.next())pending.add(new Object[]{result.getLong(1),result.getLong(2),result.getString(3)});}
        } catch(SQLException failure){throw new IllegalStateException("cannot enumerate interrupted room removals",failure);}
        for(Object[] item:pending){long roomId=(long)item[0],fence=(long)item[1];String priorRequest=String.valueOf(item[2]);boolean waitingExpiration=priorRequest.startsWith("waiting-room-expiration-");var snapshot=stateStore.latest(roomId).orElseThrow(()->new IllegalStateException("interrupted room removal has no durable snapshot: "+roomId));if(!waitingExpiration&&!Boolean.TRUE.equals(snapshot.authoritativeState().get("dissolved")))throw new IllegalStateException("interrupted room removal has no terminal authority state: "+roomId);Object version=snapshot.authoritativeState().get("stateVersion");String suffix=version instanceof Number number?String.valueOf(number.longValue()):"unknown";String requestId=waitingExpiration?priorRequest:"room-lifecycle-recover-"+roomId+"-"+suffix;String reason=waitingExpiration?"WAITING_ROOM_EXPIRED":"RECOVERED_ROOM_DISSOLUTION";hallLifecycle.close(roomId,requestId,requestId,reason);rooms.remove(roomId);if(waitingExpiration)finishAutomaticRemoval(roomId,fence);else transition(roomId,fence,"REMOVED");roomLocks.remove(roomId);}
    }

    private Map<String, Object> createLocked(Map<String, Object> command, long roomId) {
        int gameId = Math.toIntExact(positive(command, "gameId"));
        long ownerId = positive(command, "ownerId");
        String playVersion = required(command, "playVersion");
        String requestId = required(command, "requestId");
        String traceId = required(command, "traceId");
        long requestedFence = positive(command, "fencingToken");
        Route route = claim(roomId, gameId, playVersion, requestId, traceId, requestedFence);
        try {
            requireOrCreateRuntime(command, route, ownerId);
            transition(roomId, route.fencingToken(), "ACTIVE");
            return response(route);
        } catch (RuntimeException failure) {
            failRoute(roomId, route.fencingToken(), failure);
            throw failure;
        }
    }

    private void requireOrCreateRuntime(Map<String, Object> command, Route route) {
        requireOrCreateRuntime(command, route, positive(command, "ownerId"));
    }

    private void requireOrCreateRuntime(Map<String, Object> command, Route route, long ownerId) {
        boolean runtimeMissing = false;
        try {
            var existing = rooms.require(route.roomId());
            if (existing.gameId() != route.gameId() || !existing.playVersion().equals(route.playVersion())) {
                throw new SecurityException("runtime room identity conflict");
            }
            return;
        } catch (IllegalArgumentException missing) {
            runtimeMissing = true;
        }
        if (!runtimeMissing) throw new IllegalStateException("room runtime lookup failed");
        var provider = games.require(route.gameId(), route.playVersion());
        var snapshot = stateStore.latest(route.roomId());
        if (snapshot.isPresent()) {
            var durable = snapshot.orElseThrow();
            if (durable.gameId() != route.gameId()
                    || !durable.playVersion().equals(route.playVersion())
                    || durable.fencingToken() > route.fencingToken()) {
                throw new SecurityException("durable room identity conflict");
            }
            var authority = provider.restoreAuthoritativeSession(durable.authoritativeState())
                    .orElseThrow(() -> new IllegalStateException(
                            "game provider cannot restore authoritative state"));
            GameRoomHandle restored = rooms.bind(new GameRoomHandle(route.roomId(), route.gameId(),
                    route.playVersion(), authority));
            stateStore.saveSnapshot(restored, route.fencingToken());
            return;
        }
        if ("PLAYING".equals(stateStore.hallState(route.roomId()))) {
            throw new IllegalStateException("playing room has no durable snapshot");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rules = command.get("rules") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        GameRoomHandle created = rooms.create(provider,
                new RoomCreationContext(route.roomId(), ownerId, rules));
        admit(created.requireAuthoritativeSession(),ownerId,0,command.get("admission"));
        stateStore.saveSnapshot(created, route.fencingToken());
    }

    private static void admit(com.aoo.bcg.gamespi.AuthoritativeGameSession session,long playerId,
            int seatId,Object raw){
        if(!(session instanceof com.aoo.bcg.gamespi.RoomAdmissionAuthority authority))return;
        Map<?,?> value=raw instanceof Map<?,?> map?map:Map.of();
        Object ip=value.get("ipAddress"),latitude=value.get("latitude"),longitude=value.get("longitude");
        authority.admitParticipant(playerId,seatId,new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                ip==null?null:String.valueOf(ip),latitude instanceof Number n?n.doubleValue():null,
                longitude instanceof Number n?n.doubleValue():null));
    }

    private void recoverDurableRooms() {
        List<StartupRoom> candidates = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT a.room_id,a.game_id,a.play_version,a.last_request_id,a.trace_id,
                            h.owner_account_id,h.rules_json
                     FROM aoo_room_authority_route a
                     JOIN aoo_hall_room h ON h.room_id=a.room_id
                     WHERE h.state IN ('OPEN','PLAYING')
                       AND a.lifecycle_state IN ('CREATING','ACTIVE','RECOVERING','FAILED')
                       AND (a.node_id=? OR a.lease_expires_at<=CURRENT_TIMESTAMP(3))
                     ORDER BY a.room_id LIMIT 10000
                     """)) {
            statement.setString(1, nodeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    candidates.add(new StartupRoom(result.getLong(1), result.getInt(2),
                            result.getString(3), result.getString(4), result.getString(5),
                            result.getLong(6), json.readValue(result.getString(7),
                            new TypeReference<Map<String, Object>>() { })));
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException("cannot enumerate durable rooms", failure);
        }
        List<String> failures = new ArrayList<>();
        for (StartupRoom candidate : candidates) {
            synchronized (lock(candidate.roomId())) {
                Route route = route(candidate.roomId());
                Route claimed = claimForRecovery(route);
                if (claimed == null) continue;
                Map<String, Object> command = new LinkedHashMap<>();
                command.put("roomId", candidate.roomId());
                command.put("gameId", candidate.gameId());
                command.put("playVersion", candidate.playVersion());
                command.put("requestId", candidate.requestId());
                command.put("traceId", candidate.traceId());
                command.put("ownerId", candidate.ownerId());
                command.put("fencingToken", claimed.fencingToken());
                command.put("rules", candidate.rules());
                try {
                    rooms.remove(candidate.roomId());
                    requireOrCreateRuntime(command, claimed, candidate.ownerId());
                    transition(candidate.roomId(), claimed.fencingToken(), "ACTIVE");
                } catch (RuntimeException failure) {
                    failRoute(candidate.roomId(), claimed.fencingToken(), failure);
                    failures.add(candidate.roomId() + ":" + failure.getClass().getSimpleName());
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("durable room recovery failed: " + String.join(",", failures));
        }
    }

    private Route claimForRecovery(Route expected) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            Route current = selectForUpdate(connection, expected.roomId());
            if (current == null) throw new IllegalArgumentException("authority route missing");
            if (current.gameId() != expected.gameId()
                    || !current.playVersion().equals(expected.playVersion())
                    || !current.requestId().equals(expected.requestId())) {
                throw new SecurityException("room authority recovery identity conflict");
            }
            if (!nodeId.equals(current.nodeId())
                    && current.leaseExpiresAt().isAfter(clock.instant())) {
                connection.commit();
                return null;
            }
            long fence = Math.addExact(current.fencingToken(), 1);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE aoo_room_authority_route
                    SET node_id=?,authority_endpoint=?,fencing_token=?,lifecycle_state='RECOVERING',
                        lease_expires_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND),
                        updated_at=CURRENT_TIMESTAMP(3)
                    WHERE room_id=? AND fencing_token=?
                    """)) {
                statement.setString(1, nodeId);
                statement.setString(2, endpoint);
                statement.setLong(3, fence);
                statement.setLong(4, current.roomId());
                statement.setLong(5, current.fencingToken());
                if (statement.executeUpdate() != 1) throw new SecurityException("stale fencing token");
            }
            connection.commit();
            return new Route(current.roomId(), current.gameId(), current.playVersion(), fence,
                    "RECOVERING", current.requestId(), nodeId, clock.instant().plusSeconds(90),
                    current.createdAt(),current.firstRoundStartedAt());
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("cannot claim durable room", failure);
        }
    }

    private Route claim(long roomId, int gameId, String playVersion, String requestId,
                        String traceId, long requestedFence) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            Route existing = selectForUpdate(connection, roomId);
            long fence = requestedFence;
            Instant createdAt = clock.instant();
            if (existing != null) {
                if (existing.gameId() != gameId || !existing.playVersion().equals(playVersion)) {
                    throw new SecurityException("room authority identity conflict");
                }
                if (!existing.requestId().equals(requestId)) {
                    throw new SecurityException("room authority request conflict");
                }
                if (!"FAILED".equals(existing.state())) {
                    connection.commit();
                    return existing;
                }
                createdAt = existing.createdAt();
                fence = Math.max(requestedFence, Math.addExact(existing.fencingToken(), 1));
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO aoo_room_authority_route(
                      room_id,game_id,play_version,node_id,authority_endpoint,fencing_token,
                      lifecycle_state,last_request_id,trace_id,lease_expires_at,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,'CREATING',?,?,DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND),
                      CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
                    ON DUPLICATE KEY UPDATE
                      node_id=VALUES(node_id),authority_endpoint=VALUES(authority_endpoint),
                      fencing_token=VALUES(fencing_token),lifecycle_state='CREATING',
                      last_request_id=VALUES(last_request_id),trace_id=VALUES(trace_id),
                      lease_expires_at=VALUES(lease_expires_at),updated_at=CURRENT_TIMESTAMP(3)
                    """)) {
                int index = 1;
                statement.setLong(index++, roomId);
                statement.setInt(index++, gameId);
                statement.setString(index++, playVersion);
                statement.setString(index++, nodeId);
                statement.setString(index++, endpoint);
                statement.setLong(index++, fence);
                statement.setString(index++, requestId);
                statement.setString(index, traceId);
                statement.executeUpdate();
            }
            connection.commit();
            return new Route(roomId, gameId, playVersion, fence, "CREATING", requestId,
                    nodeId, clock.instant().plusSeconds(90), createdAt,
                    existing==null?null:existing.firstRoundStartedAt());
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("authority route persistence failed", failure);
        }
    }

    private Route route(long roomId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT game_id,play_version,fencing_token,lifecycle_state,last_request_id,
                            node_id,lease_expires_at,created_at,first_round_started_at
                     FROM aoo_room_authority_route WHERE room_id=?
                     """)) {
            statement.setLong(1, roomId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("authority route missing");
                return readRoute(roomId, result);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("authority route lookup failed", failure);
        }
    }

    private Route selectForUpdate(Connection connection, long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT game_id,play_version,fencing_token,lifecycle_state,last_request_id,
                       node_id,lease_expires_at,created_at,first_round_started_at
                FROM aoo_room_authority_route WHERE room_id=? FOR UPDATE
                """)) {
            statement.setLong(1, roomId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readRoute(roomId, result) : null;
            }
        }
    }

    private static Route readRoute(long roomId, ResultSet result) throws SQLException {
        java.sql.Timestamp firstRoundStartedAt=result.getTimestamp(9);
        return new Route(roomId, result.getInt(1), result.getString(2), result.getLong(3),
                result.getString(4), result.getString(5), result.getString(6),
                result.getTimestamp(7).toInstant(),result.getTimestamp(8).toInstant(),
                firstRoundStartedAt==null?null:firstRoundStartedAt.toInstant());
    }

    private void transition(long roomId, long fence, String state) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE aoo_room_authority_route
                     SET lifecycle_state=?,updated_at=CURRENT_TIMESTAMP(3),
                         lease_expires_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND)
                     WHERE room_id=? AND fencing_token=?
                     """)) {
            statement.setString(1, state);
            statement.setLong(2, roomId);
            statement.setLong(3, fence);
            if (statement.executeUpdate() != 1) throw new SecurityException("stale fencing token");
        } catch (SQLException failure) {
            throw new IllegalStateException("authority state persistence failed", failure);
        }
    }

    private void failRoute(long roomId, long fence, RuntimeException original) {
        try {
            transition(roomId, fence, "FAILED");
        } catch (RuntimeException persistenceFailure) {
            original.addSuppressed(persistenceFailure);
        }
    }

    private void renewActiveLeases() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE aoo_room_authority_route
                     SET lease_expires_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 90 SECOND),
                         updated_at=CURRENT_TIMESTAMP(3)
                     WHERE node_id=? AND lifecycle_state='ACTIVE'
                     """)) {
            statement.setString(1, nodeId);
            statement.executeUpdate();
        } catch (SQLException failure) {
            System.err.println("room-authority lease renewal failed: " + failure.getSQLState());
        }
    }

    @Override public void close(){leaseKeeper.shutdownNow();roomLocks.clear();}

    private void verifyIdentity(Route route, Map<String, Object> command) {
        if (route.gameId() != Math.toIntExact(positive(command, "gameId"))
                || !route.playVersion().equals(required(command, "playVersion"))
                || !route.requestId().equals(required(command, "requestId"))) {
            throw new SecurityException("room authority recovery identity conflict");
        }
    }

    private Object lock(long roomId) {
        return roomLocks.computeIfAbsent(roomId, ignored -> new Object());
    }

    private Map<String, Object> response(Route route) {
        return Map.of(
                "roomId", route.roomId(),
                "gameId", route.gameId(),
                "playVersion", route.playVersion(),
                "nodeId", nodeId,
                "route", endpoint,
                "fencingToken", route.fencingToken(),
                "state", "ACTIVE");
    }

    private static long positive(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return number.longValue();
    }

private static long nonNegative(Map<String, Object> command, String key) {
    Object value = command.get(key);
    if (!(value instanceof Number number) || number.longValue() < 0) {
        throw new IllegalArgumentException(key + " must be non-negative");
    }
    return number.longValue();
}

    private static String required(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return text;
    }

    private static String required(String value, String key) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private record Route(long roomId, int gameId, String playVersion, long fencingToken,
                         String state, String requestId, String nodeId, Instant leaseExpiresAt,
                         Instant createdAt,Instant firstRoundStartedAt) { }
    private record StartupRoom(long roomId, int gameId, String playVersion, String requestId,
                               String traceId, long ownerId, Map<String, Object> rules) {
        private StartupRoom { rules = Map.copyOf(rules); }
    }
}
