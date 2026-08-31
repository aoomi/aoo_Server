package core.server;

import com.aoo.bcg.common.event.RoomEventJournal;
import com.aoo.bcg.common.event.JdbcRoomEventJournal;
import com.aoo.bcg.common.event.RoomEventIdentity;
import com.aoo.bcg.common.recovery.RoomLease;
import com.aoo.bcg.common.recovery.RoomLeaseStore;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.common.recovery.RoomSnapshotStore;
import com.aoo.bcg.common.recovery.RoomDeadlineSnapshot;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/** Persists every successful V2 authority mutation as event then snapshot. */
final class ProductionRoomCommandCommitter implements GameCommandCommitter {
    interface AtomicPersistence { void commit(RoomSnapshot snapshot, RoomEventIdentity identity, Map<String,Object> payload,long expectedStateVersion); }
    private final AtomicPersistence persistence;
    private final RoomSnapshotStore snapshots;
    private final RoomLeaseStore leases;
    private final Clock clock;
    private final String nodeId;
    private final RoomEdgeRuntimePolicy edgePolicy;

    ProductionRoomCommandCommitter(RoomEventJournal events, RoomSnapshotStore snapshots,
                                   RoomLeaseStore leases, Clock clock, String nodeId) {
        if (!(events instanceof JdbcRoomEventJournal jdbc))
            throw new IllegalArgumentException("production room persistence must atomically commit event and snapshot");
        this.persistence = (snapshot,identity,payload,expectedStateVersion)->jdbc.appendAndSnapshotAndOutboxCas(snapshot,identity,payload,
                new com.aoo.bcg.common.event.OutboxEvent("room:"+identity.roomId()+":"+identity.sequence(),"ROOM",
                        identity.roomId(),"ROOM_COMMAND_COMMITTED",1,payload,snapshot.capturedAt()),expectedStateVersion);
        this.snapshots = snapshots;
        this.leases = leases;
        this.clock = clock;
        this.nodeId = nodeId;
        this.edgePolicy = RoomEdgeRuntimePolicy.production();
    }

    ProductionRoomCommandCommitter(AtomicPersistence persistence, RoomSnapshotStore snapshots,
            RoomLeaseStore leases, Clock clock, String nodeId, RoomEdgeRuntimePolicy edgePolicy) {
        this.persistence=java.util.Objects.requireNonNull(persistence); this.snapshots=snapshots;
        this.leases=leases; this.clock=clock; this.nodeId=nodeId; this.edgePolicy=edgePolicy;
    }

    @Override public void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result) {
        edgePolicy.validateCommandIdentity(room.roomId(), request.roomId(), request.roundNo());
        edgePolicy.fault("before-lease");
        RoomLease lease = leases.acquire(room.roomId(), nodeId, RoomEdgeRuntimePolicy.LEASE_TTL);
        if (!leases.isCurrent(lease)) throw new IllegalStateException("room lease lost before commit");
        var session=room.requireAuthoritativeSession();
        long stateVersion=session.stateVersion();
        long expectedStateVersion=snapshots.latest(room.roomId()).map(RoomSnapshot::stateVersion).orElse(0L);
        if(stateVersion==expectedStateVersion) return; // read-only command
        if(stateVersion!=Math.addExact(expectedStateVersion,1))
            throw new java.util.ConcurrentModificationException("in-memory authority is not based on persisted stateVersion");
        long sequence=stateVersion;
        Map<String,Object> mutable=new java.util.LinkedHashMap<>(session.authoritativeState());
        mutable.put("deadlines",RoomDeadlineSnapshot.from(session).toMap());
        mutable.put("_lastCommittedRequestId", request.requestId());
        mutable.put("_lastCommittedMsgId", request.msgId());
        mutable.put("_lastCommittedResultMsgId",result.msgId());
        mutable.put("_lastCommittedResultBody",result.body().asMap());
        mutable.put("_lastCommittedServerTime",result.serverTimeEpochMillis());
        mutable.put("_lastCommittedDeadline",result.operationDeadline());
        Map<String, Object> state = Map.copyOf(mutable);
        RoomSnapshot snapshot = new RoomSnapshot(room.roomId(), room.gameId(), room.playVersion(),
                room.playVersion(), lease.fencingToken(), sequence, clock.instant(), state);
        Map<String,Object> payload = Map.of("request",request,"result",result,"stateAfter",state);
        RoomEventIdentity identity=RoomEventIdentity.v1(room.roomId(),request.roundNo(),sequence,
                request.requestId(),request.msgId());
        persist(snapshot, identity, payload);
        // Never fail upward after durability: the router would release its PROCESSING
        // reservation and execute the already-applied handler again.  Fencing was
        // checked immediately before persistence and is enforced transactionally by
        // the snapshot store; a later lease loss affects the next command.
    }

    @Override public java.util.Optional<GameCommandResult> findCommitted(GameRoomHandle room,GameCommandRequest request){
        return snapshots.latest(room.roomId()).filter(saved->requestIdentityMatches(saved,
                RoomEventIdentity.v1(room.roomId(),request.roundNo(),saved.lastEventSequence(),request.requestId(),request.msgId())))
                .map(saved->new GameCommandResult(String.valueOf(saved.authoritativeState().get("_lastCommittedResultMsgId")),
                        request.requestId(),com.aoo.bcg.gamespi.CommandPayload.copyOf(castMap(saved.authoritativeState().get("_lastCommittedResultBody"))),
                        ((Number)saved.authoritativeState().getOrDefault("_lastCommittedServerTime",0L)).longValue(),
                        castMap(saved.authoritativeState().get("_lastCommittedDeadline"))));
    }

    private static Map<String,Object> castMap(Object value){
        if(!(value instanceof Map<?,?> source))return Map.of();
        Map<String,Object> target=new java.util.LinkedHashMap<>();source.forEach((k,v)->target.put(String.valueOf(k),v));return Map.copyOf(target);
    }

    private void persist(RoomSnapshot snapshot, RoomEventIdentity identity, Map<String,Object> payload) {
        edgePolicy.fault("before-persistence");
        try {
            persistence.commit(snapshot,identity,payload,Math.subtractExact(snapshot.stateVersion(),1));
        } catch (RuntimeException failure) {
            // A connection failure may mean COMMIT succeeded but its acknowledgement was lost.
            // Never blindly append the same mutation again.  A snapshot at this exact event
            // sequence is authoritative proof that the transaction committed.
            boolean committed = snapshots.latest(snapshot.roomId())
                    .filter(saved -> saved.lastEventSequence() == snapshot.lastEventSequence()
                            && saved.fencingToken() == snapshot.fencingToken()
                            && requestIdentityMatches(saved, identity))
                    .isPresent();
            if (!committed) throw failure;
        }
    }

    private static boolean requestIdentityMatches(RoomSnapshot saved, RoomEventIdentity identity) {
        Object requestId=saved.authoritativeState().get("_lastCommittedRequestId");
        Object msgId=saved.authoritativeState().get("_lastCommittedMsgId");
        return identity.businessEventId().equals(String.valueOf(requestId))
                && identity.eventType().equals(String.valueOf(msgId));
    }
}
