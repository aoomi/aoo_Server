package com.aoo.bcg.billing;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.aoo.bcg.billing.RoomCarryReceipt.*;

/** Caller-transaction progress, with current locking reads on every state decision. */
public final class JdbcRoomCarryReceiptRepository {
    private final Clock clock;
    private final ObjectMapper json=new ObjectMapper();
    public JdbcRoomCarryReceiptRepository(Clock clock){this.clock=Objects.requireNonNull(clock);}
    String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalArgumentException("receipt serialization",e);}}
    <T>T decode(String s,Class<T> type){try{return json.readValue(s,type);}catch(Exception e){throw new IllegalStateException("corrupt receipt",e);}}
    private Timestamp now(){return Timestamp.from(clock.instant().truncatedTo(ChronoUnit.MILLIS));}
    private record Row(Attempt attempt,String stage,long ordinal,long version,Original original,String mode) { }
    private Row row(Connection c,String id)throws SQLException {
        try(var s=c.prepareStatement("SELECT attempt_json,stage,last_ordinal,state_version,original_json,money_mode FROM aoo_room_carry_attempt WHERE attempt_id=? FOR UPDATE")){
            s.setString(1,id);try(var r=s.executeQuery()){if(!r.next())return null;return new Row(decode(r.getString(1),Attempt.class),r.getString(2),r.getLong(3),r.getLong(4),r.getString(5)==null?null:decode(r.getString(5),Original.class),r.getString(6));}}
    }
    private void insertAttempt(Connection c,Attempt a,Original original)throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO aoo_room_carry_attempt(attempt_id,attempt_json,stage,last_ordinal,state_version,original_json,original_request_id,money_mode,updated_at) VALUES(?,?,NULL,0,0,?,?,?,?)")){
            s.setString(1,a.attemptId());s.setString(2,encode(a));s.setString(3,original==null?null:encode(original));s.setString(4,original==null?null:original.operation().requestId());s.setString(5,original==null?"UNDECIDED":"BOUND");s.setTimestamp(6,now());s.executeUpdate();}
    }
    public void bindOriginal(Connection c,Attempt a,Original original,OriginalLinkVerifier verifier)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);Objects.requireNonNull(original).matches(a);Objects.requireNonNull(verifier).verify(c,a,original);
        Row old=row(c,a.attemptId());if(old==null){insertAttempt(c,a,original);return;}
        if(!old.attempt.equals(a)||!Objects.equals(old.original,original)||!"BOUND".equals(old.mode))throw new IllegalArgumentException("immutable original link conflict");
    }
    public void stageBatch(Connection c,Batch b)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);
        try(var s=c.prepareStatement("SELECT batch_json FROM aoo_room_carry_batch WHERE batch_hash=? FOR UPDATE")){s.setString(1,b.batchHash());try(var r=s.executeQuery()){if(r.next()){if(!decode(r.getString(1),Batch.class).equals(b))throw new IllegalArgumentException("batch identity conflict");return;}}}
        try(var s=c.prepareStatement("INSERT INTO aoo_room_carry_batch(batch_hash,batch_json,status,proof_json,created_at) VALUES(?,?,'STAGED',NULL,?)")){s.setString(1,b.batchHash());s.setString(2,encode(b));s.setTimestamp(3,now());s.executeUpdate();}
        // Stable ordering across attempts; within an attempt native ordinals remain ordered.
        var ordered=new ArrayList<>(b.effects());ordered.sort(Comparator.comparing((Effect e)->e.attempt().attemptId()).thenComparingLong(Effect::ordinal));
        for(Effect e:ordered){Attempt a=e.attempt();Row old=row(c,a.attemptId());if(old==null){insertAttempt(c,a,null);old=row(c,a.attemptId());}
            if(!old.attempt.equals(a)||e.ordinal()<=old.ordinal||e.stateVersion()<old.version)throw new IllegalArgumentException("attempt identity/ordering conflict");
            boolean allowed=old.stage==null?e.stage()==Stage.REQUESTED:
                old.stage.equals("REQUESTED")?(e.stage()==Stage.APPLIED||e.stage()==Stage.TERMINATED_UNUSED):
                old.stage.equals("APPLIED")?(e.stage()==Stage.USED||e.stage()==Stage.TERMINATED_UNUSED):false;
            if(!allowed)throw new IllegalStateException("invalid native carry transition");
            try(var s=c.prepareStatement("INSERT INTO aoo_room_carry_effect(effect_id,attempt_id,batch_hash,instance_id,epoch,native_ordinal,effect_json) VALUES(?,?,?,?,?,?,?)")){
                s.setString(1,e.effectId());s.setString(2,a.attemptId());s.setString(3,b.batchHash());s.setString(4,a.instanceId());s.setLong(5,a.epoch());s.setLong(6,e.ordinal());s.setString(7,encode(e));s.executeUpdate();}
            try(var s=c.prepareStatement("UPDATE aoo_room_carry_attempt SET stage=?,last_ordinal=?,state_version=?,money_mode=?,updated_at=? WHERE attempt_id=?")){
                s.setString(1,e.stage().name());s.setLong(2,e.ordinal());s.setLong(3,e.stateVersion());s.setString(4,old.original==null?"FACT_ONLY":"BOUND");s.setTimestamp(5,now());s.setString(6,a.attemptId());if(s.executeUpdate()!=1)throw new IllegalStateException("attempt lost");}
        }
    }
    public void finalizeBatch(Connection c,Batch b,Proof p,StoredProofVerifier verifier)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);Attempt first=b.effects().getFirst().attempt();
        if(!p.instanceId().equals(first.instanceId())||p.epoch()!=first.epoch()||b.effects().stream().anyMatch(e->e.stateVersion()>p.stateVersion()))throw new IllegalArgumentException("runtime proof conflict");
        try(var s=c.prepareStatement("SELECT batch_json,status,proof_json FROM aoo_room_carry_batch WHERE batch_hash=? FOR UPDATE")){s.setString(1,b.batchHash());try(var r=s.executeQuery()){
            if(!r.next()||!decode(r.getString(1),Batch.class).equals(b))throw new IllegalArgumentException("unknown batch");
            if("FINALIZED".equals(r.getString(2))&&!decode(r.getString(3),Proof.class).equals(p))throw new IllegalArgumentException("immutable final proof conflict");}}
        Objects.requireNonNull(verifier).verify(c,b,p);
        try(var s=c.prepareStatement("UPDATE aoo_room_carry_batch SET status='FINALIZED',proof_json=? WHERE batch_hash=?")){s.setString(1,encode(p));s.setString(2,b.batchHash());if(s.executeUpdate()!=1)throw new IllegalStateException("batch lost");}
    }
    public void requireReadyToCommit(Connection c,String batchHash)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);
        try(var s=c.prepareStatement("SELECT status FROM aoo_room_carry_batch WHERE batch_hash=? FOR UPDATE")){s.setString(1,batchHash);try(var r=s.executeQuery()){if(!r.next()||!"FINALIZED".equals(r.getString(1)))throw new IllegalStateException("unfinalized carry batch");}}
    }
    public Effect requireFinalized(Connection c,String effectId,StoredProofVerifier verifier)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);Effect e;String batch;
        try(var s=c.prepareStatement("SELECT effect_json,batch_hash FROM aoo_room_carry_effect WHERE effect_id=?")){s.setString(1,effectId);try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("unknown effect");e=decode(r.getString(1),Effect.class);batch=r.getString(2);}}
        try(var s=c.prepareStatement("SELECT batch_json,proof_json,status FROM aoo_room_carry_batch WHERE batch_hash=? FOR UPDATE")){s.setString(1,batch);try(var r=s.executeQuery()){
            if(!r.next()||!"FINALIZED".equals(r.getString(3)))throw new IllegalStateException("missing committed-byte proof");Objects.requireNonNull(verifier).verify(c,decode(r.getString(1),Batch.class),decode(r.getString(2),Proof.class));}}
        return e;
    }
    Original requireOriginal(Connection c,Effect e,Original expected)throws SQLException {
        Row r=row(c,e.attempt().attemptId());if(r==null||!r.attempt.equals(e.attempt())||!"BOUND".equals(r.mode)||!Objects.equals(r.original,expected))throw new IllegalArgumentException("no immutable original association");
        expected.matches(e.attempt());return r.original;
    }
}
