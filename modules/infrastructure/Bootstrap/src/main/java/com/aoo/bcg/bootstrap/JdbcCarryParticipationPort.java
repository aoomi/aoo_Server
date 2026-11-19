package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.*;
import com.aoo.bcg.gamespi.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Native facts and durable byte proofs. This port cannot create a CARRY charge. */
final class JdbcCarryParticipationPort {
    private final DataSource source;
    private final ObjectMapper json;
    private final JdbcRoomCarryReceiptRepository receipts;
    private volatile JdbcRoomCarryRefundConsumer consumer;
    private final Map<Long,Gate> gates=new ConcurrentHashMap<>();
    private static final class Gate {
        final ReentrantReadWriteLock lock=new ReentrantReadWriteLock();
        volatile boolean unknown;
    }
    static final class Checkpoint {final long roomId;final RoomRuntimeIdentity identity;boolean finished;Checkpoint(long roomId,RoomRuntimeIdentity identity){this.roomId=roomId;this.identity=identity;}long roomId(){return roomId;}RoomRuntimeIdentity identity(){return identity;}}
    record Staged(RoomCarryReceipt.Batch batch,String nativeHash,GameCommandRequest command,RoomCarryReceipt.Proof priorProof) { }

    JdbcCarryParticipationPort(DataSource source,ObjectMapper json,Clock clock) {
        this.source=Objects.requireNonNull(source);this.json=Objects.requireNonNull(json);
        receipts=new JdbcRoomCarryReceiptRepository(clock);
    }
    void configureCharges(LongSupplier ids,Clock clock) {
        if(consumer!=null)throw new IllegalStateException("carry consumer already configured");
        consumer=new JdbcRoomCarryRefundConsumer(receipts,new JdbcRoomChargeCoordinator(source,
                new JdbcBillingService(source,ids,clock),new JdbcRoomCostPolicyRepository(source,false),clock));
    }
    private Gate gate(long room){return gates.computeIfAbsent(room,k->new Gate());}
    void requireAvailable(long room) {
        Gate g=gate(room);
        if(g.unknown||g.lock.isWriteLocked())throw new IllegalStateException("CARRY_ROOM_RECOVERY_REQUIRED");
    }
    <T>T read(long room,Supplier<T> work) {
        Gate g=gate(room);
        if(!g.lock.readLock().tryLock())throw new IllegalStateException("CARRY_ROOM_RECOVERY_REQUIRED");
        try {if(g.unknown)throw new IllegalStateException("CARRY_ROOM_RECOVERY_REQUIRED");return work.get();}
        finally {g.lock.readLock().unlock();}
    }
    Checkpoint begin(GameRoomHandle room,com.aoo.bcg.common.recovery.RoomSnapshot snapshot) {
        if(!(room.requireAuthoritativeSession() instanceof CarryBillingEffectSource))return null;
        Gate g=gate(room.roomId());
        if(!g.lock.writeLock().tryLock())throw new IllegalStateException("CARRY_ROOM_BUSY");
        try {
            if(g.unknown||room.runtimeIdentity()==null||snapshot==null
                    ||snapshot.roomId()!=room.roomId()||snapshot.stateVersion()!=room.runtimeIdentity().stateVersion()
                    ||snapshot.fencingToken()!=room.runtimeIdentity().fencingToken())
                throw new IllegalStateException("CARRY_DURABLE_CHECKPOINT_REQUIRED");
            return new Checkpoint(room.roomId(),room.runtimeIdentity());
        } catch(RuntimeException ex){g.lock.writeLock().unlock();throw ex;}
    }
    void finish(Checkpoint checkpoint,boolean recovered) {
        if(checkpoint==null||checkpoint.finished)return;Gate g=gate(checkpoint.roomId());
        if(!g.lock.isWriteLockedByCurrentThread())throw new IllegalStateException("carry gate owner required");
        g.unknown=!recovered;checkpoint.finished=true;g.lock.writeLock().unlock();
    }
    private static String hash(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private String commandHash(GameCommandRequest command)throws Exception {
        var canonical=json.copy().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
        return hash(canonical.writeValueAsBytes(List.of("CARRY_AUTH_COMMAND_V1",command)));
    }
    private RoomCarryReceipt.Attempt existing(Connection c,String attempt)throws Exception {
        try(var q=c.prepareStatement("SELECT attempt_json FROM aoo_room_carry_attempt WHERE attempt_id=? FOR UPDATE")) {
            q.setString(1,attempt);try(var rows=q.executeQuery()) {
                return rows.next()?json.readValue(rows.getString(1),RoomCarryReceipt.Attempt.class):null;
            }
        }
    }
    Staged stage(Connection c,GameRoomHandle room,GameCommandRequest command)throws Exception {
        if(!(room.requireAuthoritativeSession() instanceof CarryBillingEffectSource nativeSource))return null;
        var nativeBatch=nativeSource.pendingCarryEffects();if(nativeBatch.effects().isEmpty())return null;
        var identity=Objects.requireNonNull(room.runtimeIdentity());
        Map<String,RoomCarryReceipt.Attempt> attempts=new HashMap<>();var effects=new ArrayList<RoomCarryReceipt.Effect>();
        for(var e:nativeBatch.effects()) {
            RoomCarryReceipt.Attempt attempt=attempts.get(e.attemptId());
            if(attempt==null) {
                attempt=existing(c,e.attemptId());
                if(attempt==null) {
                    if(command==null||e.stage()!=CarryBillingEffectSource.Stage.REQUESTED
                            ||!e.originalRequestId().equals(command.requestId())
                            ||e.actorId()!=Long.parseLong(command.authenticatedUserId()))
                        throw new IllegalStateException("CARRY_ORIGINAL_AUTHENTICATED_REQUEST_REQUIRED");
                    attempt=new RoomCarryReceipt.Attempt(e.attemptId(),e.originalRequestId(),commandHash(command),
                            e.actorId(),e.roomId(),identity.roomInstanceId(),identity.lifecycleEpoch(),e.seatId(),
                            e.carryScore(),RoomCarryReceipt.Kind.valueOf(e.kind().name()));
                }
                attempts.put(e.attemptId(),attempt);
            }
            if(attempt.roomId()!=room.roomId()||!attempt.instanceId().equals(identity.roomInstanceId())
                    ||attempt.epoch()!=identity.lifecycleEpoch()||!attempt.originalRequestId().equals(e.originalRequestId())
                    ||attempt.nativeActorId()!=e.actorId()||attempt.seat()!=e.seatId()
                    ||attempt.amount()!=e.carryScore()||!attempt.kind().name().equals(e.kind().name()))
                throw new IllegalStateException("CARRY_NATIVE_ATTEMPT_CONFLICT");
            effects.add(new RoomCarryReceipt.Effect(e.effectId(),attempt,
                    RoomCarryReceipt.Stage.valueOf(e.stage().name()),e.ordinal(),e.stateVersion()));
        }
        var batch=new RoomCarryReceipt.Batch(nativeBatch.hash(),nativeBatch.throughOrdinal(),effects);
        RoomCarryReceipt.Proof prior=null;
        try(var q=c.prepareStatement("SELECT batch_json,status,proof_json FROM aoo_room_carry_batch WHERE batch_hash=? FOR UPDATE")) {
            q.setString(1,batch.batchHash());try(var rows=q.executeQuery()) {
                if(rows.next()&&"FINALIZED".equals(rows.getString(2))) {
                    if(!json.readValue(rows.getString(1),RoomCarryReceipt.Batch.class).equals(batch))throw new IllegalStateException("CARRY_FINALIZED_BATCH_CONFLICT");
                    prior=json.readValue(rows.getString(3),RoomCarryReceipt.Proof.class);
                }
            }
        }
        if(prior==null)receipts.stageBatch(c,batch);
        nativeSource.acknowledgeCarryEffects(nativeBatch.throughOrdinal(),nativeBatch.hash());
        return new Staged(batch,nativeBatch.hash(),command,prior);
    }
    private static long number(Object value) {
        if(!(value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long))
            throw new IllegalStateException("CARRY_STORED_INTEGER_REQUIRED");
        return ((Number)value).longValue();
    }
    private void requireNativeProof(Map<?,?> state,RoomCarryReceipt.Batch batch) {
        if(!(state.get("carryBilling") instanceof Map<?,?> carry)
                ||number(carry.get("ackThroughOrdinal"))<batch.throughOrdinal())
            throw new IllegalStateException("CARRY_STORED_ACK_REQUIRED");
        if(number(carry.get("ackThroughOrdinal"))==batch.throughOrdinal()
                &&!batch.batchHash().equals(carry.get("ackBatchHash")))
            throw new IllegalStateException("CARRY_STORED_ACK_HASH_CONFLICT");
        if(!(carry.get("attempts") instanceof List<?> nativeAttempts))
            throw new IllegalStateException("CARRY_STORED_ATTEMPTS_REQUIRED");
        for(var effect:batch.effects()) {
            var a=effect.attempt();Map<?,?> found=null;
            for(Object raw:nativeAttempts)if(raw instanceof Map<?,?> value&&a.attemptId().equals(value.get("attemptId"))) {
                if(found!=null)throw new IllegalStateException("CARRY_STORED_ATTEMPT_AMBIGUOUS");found=value;
            }
            if(found==null||!a.originalRequestId().equals(found.get("originalRequestId"))
                    ||number(found.get("roomId"))!=a.roomId()||number(found.get("actorId"))!=a.nativeActorId()
                    ||number(found.get("seatId"))!=a.seat()||number(found.get("carryScore"))!=a.amount()
                    ||!a.kind().name().equals(found.get("kind")))throw new IllegalStateException("CARRY_STORED_ATTEMPT_CONFLICT");
            if(effect.stage()==RoomCarryReceipt.Stage.APPLIED&&!Boolean.TRUE.equals(found.get("applied"))
                    ||effect.stage()==RoomCarryReceipt.Stage.USED&&!Boolean.TRUE.equals(found.get("used"))
                    ||effect.stage()==RoomCarryReceipt.Stage.TERMINATED_UNUSED
                    &&(!Boolean.TRUE.equals(found.get("terminated"))||Boolean.TRUE.equals(found.get("used"))))
                throw new IllegalStateException("CARRY_STORED_STAGE_CONFLICT");
        }
    }
    private String snapshot(Connection c,long room,RoomCarryReceipt.Proof proof)throws Exception {
        try(var q=c.prepareStatement("SELECT s.state_payload,s.state_version,s.fencing_token,i.room_instance_id,i.lifecycle_epoch,i.fencing_token,i.state_version FROM aoo_room_snapshot s JOIN aoo_room_runtime_identity_authority i ON i.room_id=s.room_id WHERE s.room_id=? FOR UPDATE")) {
            q.setLong(1,room);try(var rows=q.executeQuery()) {
                if(!rows.next()||rows.getLong(2)!=proof.stateVersion()||rows.getLong(3)!=proof.fence()
                        ||!proof.instanceId().equals(rows.getString(4))||rows.getLong(5)!=proof.epoch()
                        ||rows.getLong(6)!=proof.fence()||rows.getLong(7)!=proof.stateVersion())
                    throw new IllegalStateException("CARRY_STORED_RUNTIME_CONFLICT");
                return rows.getString(1);
            }
        }
    }
    void finalizeStored(Connection c,GameRoomHandle room,Staged staged,String systemTrigger)throws Exception {
        if(staged==null)return;
        if(staged.priorProof()!=null) {
            try(var q=c.prepareStatement("SELECT batch_json,status,proof_json FROM aoo_room_carry_batch WHERE batch_hash=? FOR UPDATE")) {
                q.setString(1,staged.batch().batchHash());try(var rows=q.executeQuery()) {
                    if(!rows.next()||!"FINALIZED".equals(rows.getString(2))
                            ||!json.readValue(rows.getString(1),RoomCarryReceipt.Batch.class).equals(staged.batch())
                            ||!json.readValue(rows.getString(3),RoomCarryReceipt.Proof.class).equals(staged.priorProof()))
                        throw new IllegalStateException("CARRY_FINALIZED_RECEIPT_CONFLICT");
                }
            }
            receipts.requireReadyToCommit(c,staged.batch().batchHash());return;
        }
        var id=room.runtimeIdentity();String payload;
        try(var q=c.prepareStatement("SELECT state_payload FROM aoo_room_snapshot WHERE room_id=? FOR UPDATE")) {
            q.setLong(1,room.roomId());try(var rows=q.executeQuery()) {
                if(!rows.next())throw new IllegalStateException("CARRY_FINAL_SNAPSHOT_REQUIRED");payload=rows.getString(1);
            }
        }
        String response=null;
        if(staged.command()!=null) {
            var command=staged.command();var key=new com.aoo.bcg.common.idempotency.IdempotencyKey(
                    command.authenticatedUserId(),command.msgId(),command.roomId(),command.roundNo(),command.requestId());
            try(var q=c.prepareStatement("SELECT response_payload,status FROM aoo_business_idempotency WHERE request_id=? FOR UPDATE")) {
                q.setString(1,key.storageKey());try(var rows=q.executeQuery()) {
                    if(!rows.next()||!"COMPLETED".equals(rows.getString(2)))throw new IllegalStateException("CARRY_FINAL_RESPONSE_REQUIRED");
                    response=rows.getString(1);
                }
            }
        }
        var proof=new RoomCarryReceipt.Proof(staged.command()==null?RoomCarryReceipt.WriterKind.SYSTEM_SNAPSHOT:RoomCarryReceipt.WriterKind.COMMAND,
                staged.command()==null?systemTrigger:staged.command().requestId(),id.roomInstanceId(),id.lifecycleEpoch(),id.fencingToken(),id.stateVersion(),
                hash(payload.getBytes(StandardCharsets.UTF_8)),response==null?null:hash(response.getBytes(StandardCharsets.UTF_8)));
        final String actualResponse=response;
        RoomCarryReceipt.StoredProofVerifier verifier=(connection,batch,p)-> {
            try {
                String stored=snapshot(connection,room.roomId(),p);
                if(!hash(stored.getBytes(StandardCharsets.UTF_8)).equals(p.snapshotHash()))throw new IllegalStateException("CARRY_FINAL_BYTES_CONFLICT");
                requireNativeProof(json.readValue(stored,Map.class),batch);
                if(p.writerKind()==RoomCarryReceipt.WriterKind.COMMAND
                        &&(actualResponse==null||!hash(actualResponse.getBytes(StandardCharsets.UTF_8)).equals(p.responseHash())))
                    throw new IllegalStateException("CARRY_FINAL_RESPONSE_CONFLICT");
            } catch(SQLException ex){throw ex;}catch(Exception ex){throw new SQLException("carry stored proof rejected",ex);}
        };
        receipts.finalizeBatch(c,staged.batch(),proof,verifier);
        // Only a pre-existing bound link is consumable. The current producer never invents one.
        for(var effect:staged.batch().effects()) {
            if(effect.stage()!=RoomCarryReceipt.Stage.USED&&effect.stage()!=RoomCarryReceipt.Stage.TERMINATED_UNUSED)continue;
            try(var q=c.prepareStatement("SELECT original_json,money_mode FROM aoo_room_carry_attempt WHERE attempt_id=? FOR UPDATE")) {
                q.setString(1,effect.attempt().attemptId());try(var rows=q.executeQuery()) {
                    if(!rows.next())throw new IllegalStateException("carry attempt lost");
                    if("BOUND".equals(rows.getString(2))) {
                        if(consumer==null)throw new IllegalStateException("CARRY_ORIGINAL_CONSUMER_REQUIRED");
                        consumer.consumeOriginal(c,effect.effectId(),json.readValue(rows.getString(1),RoomCarryReceipt.Original.class),verifier);
                    }
                }
            }
        }
        receipts.requireReadyToCommit(c,staged.batch().batchHash());
    }
}
