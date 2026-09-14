package business.global.pk.zypk;

import com.aoo.bcg.common.event.JdbcRoomEventJournal;
import com.aoo.bcg.common.recovery.JdbcRoomLeaseStore;
import com.aoo.bcg.common.recovery.JdbcRoomSnapshotStore;
import com.aoo.bcg.common.recovery.RoomLease;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.db.DataBaseMgr;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Production durability boundary for every successful mutating ZYPK command. */
public final class ZYPKProductionCommitter implements GameCommandCommitter {
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private final Object initLock = new Object();
    private volatile Runtime runtime;

    @Override
    public void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result) {
        if (ZYPKCommandHandler.STATE_REQUEST.equals(request.msgId())) return;
        if (room.gameId() != ZYPKPersistenceService.GAME_ID || room.roomId() != request.roomId())
            throw new SecurityException("ZYPK persistence identity mismatch");
        Runtime active = runtime();
        RoomLease lease = active.leases.acquire(room.roomId(), active.nodeId, LEASE_TTL);
        Long sequence = RedisUtil.incrLong("aoo:room:event-sequence:" + room.roomId());
        if (sequence == null || sequence <= 0) throw new IllegalStateException("cannot allocate room event sequence");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request.requestId());
        payload.put("actorPlayerId", request.authenticatedUserId());
        payload.put("seatId", request.seatId());
        payload.put("roundNo", request.roundNo());
        payload.put("command", request.body());
        payload.put("resultMsgId", result.msgId());
        active.persistence.persist(room.requireLegacyRoom(ZYPKTable.class), request.roundNo(), sequence,
                lease.fencingToken(), request.requestId(), request.msgId(), Map.copyOf(payload));
    }

    private Runtime runtime() {
        Runtime current = runtime;
        if (current != null) return current;
        synchronized (initLock) {
            current = runtime;
            if (current == null) {
                var dataSource = DataBaseMgr.get("clark_game").dataSource();
                var mapper = new ObjectMapper().findAndRegisterModules();
                Clock clock = Clock.systemUTC();
                String nodeId = System.getenv().getOrDefault("AOO_NODE_ID", Config.ServerIDStr());
                current = new Runtime(nodeId, new JdbcRoomLeaseStore(dataSource, clock),
                        new ZYPKPersistenceService(new JdbcRoomEventJournal(dataSource, mapper),
                                new JdbcRoomSnapshotStore(dataSource, mapper), clock));
                runtime = current;
            }
            return current;
        }
    }

    private record Runtime(String nodeId, JdbcRoomLeaseStore leases, ZYPKPersistenceService persistence) { }
}
