package com.aoo.bcg.billing;

import java.sql.*;
import java.time.Instant;
import static com.aoo.bcg.billing.RoomAaParticipation.*;

/** Persistence only. The session row serializes attempt allocation; the caller owns every transaction. */
final class JdbcRoomAaParticipationRepository {
    record Attempt(long ordinal,String operationKey,String subjectHash,String bindingHash,String state) { }
    record Command(long attempt,String hash,PreSeatState pre,String status,SeatReceipt receipt) { }
    long session(Connection c,SessionKey key,Context context,Instant at,boolean create)throws SQLException {
        try(var s=c.prepareStatement("SELECT session_id,room_id,room_instance_id,lifecycle_epoch,creator_id,context_hash FROM aoo_room_aa_session WHERE session_key=? FOR UPDATE")) {
            s.setString(1,key.sessionHash());try(var r=s.executeQuery()) { if(r.next()) {
                if(r.getLong(2)!=key.roomId()||!r.getString(3).equals(key.roomInstanceId())||r.getLong(4)!=key.lifecycleEpoch()
                        ||r.getLong(5)!=context.creatorId()||!r.getString(6).equals(context.contextHash()))throw new IllegalArgumentException("AA immutable session conflict");
                return r.getLong(1);
            }}
        }
        if(!create)return 0;
        try(var s=c.prepareStatement("INSERT INTO aoo_room_aa_session(session_key,room_id,room_instance_id,lifecycle_epoch,creator_id,context_hash,created_at) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,key.sessionHash());s.setLong(2,key.roomId());s.setString(3,key.roomInstanceId());s.setLong(4,key.lifecycleEpoch());s.setLong(5,context.creatorId());s.setString(6,context.contextHash());s.setTimestamp(7,Timestamp.from(at));s.executeUpdate();
            try(var r=s.getGeneratedKeys()){if(!r.next())throw new IllegalStateException("AA session identity missing");return r.getLong(1);}
        }
    }
    Attempt attempt(Connection c,long session,long actor,Long ordinal)throws SQLException {
        try(var s=c.prepareStatement("SELECT attempt_no,operation_key,subject_command_hash,binding_hash,state FROM aoo_room_aa_attempt WHERE session_id=? AND actor_id=?"+(ordinal==null?" ORDER BY attempt_no DESC LIMIT 1":" AND attempt_no=?")+" FOR UPDATE")) {
            s.setLong(1,session);s.setLong(2,actor);if(ordinal!=null)s.setLong(3,ordinal);
            try(var r=s.executeQuery()){return r.next()?new Attempt(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)):null;}
        }
    }
    void insertAttempt(Connection c,long session,long actor,long ordinal,RoomBillingIntent intent,String subject,String binding,Instant at)throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO aoo_room_aa_attempt(session_id,actor_id,attempt_no,operation_key,subject_command_hash,binding_hash,state,created_at) VALUES(?,?,?,?,?,?,'PREPARED',?)")) {
            s.setLong(1,session);s.setLong(2,actor);s.setLong(3,ordinal);s.setString(4,intent.operation().operationKey());s.setString(5,subject);s.setString(6,binding);s.setTimestamp(7,Timestamp.from(at));s.executeUpdate();
        }
    }
    Command command(Connection c,long session,SessionKey key,CommandKey command)throws SQLException {
        try(var s=c.prepareStatement("SELECT request_id,request_hash,actor_id,attempt_no,preparation_hash,pre_seat,pre_fence,pre_version,pre_snapshot_hash,status,post_seat,post_fence,post_version,post_snapshot_hash,response_hash,committed_at FROM aoo_room_aa_command WHERE session_id=? AND command_key=? FOR UPDATE")) {
            s.setLong(1,session);s.setString(2,command.commandKey());try(var r=s.executeQuery()) {
                if(!r.next())return null;
                if(!r.getString(1).equals(command.requestId())||!r.getString(2).equals(command.requestHash())||r.getLong(3)!=command.authenticatedActorId())throw new IllegalArgumentException("AA command identity conflict");
                long ordinal=r.getLong(4);String status=r.getString(10);SeatReceipt receipt=null;
                if("COMMITTED".equals(status)) {
                    var a=attempt(c,session,command.authenticatedActorId(),ordinal);if(a==null)throw new IllegalStateException("AA receipt attempt missing");
                    receipt=new SeatReceipt(session,ordinal,a.operationKey(),new SeatProof(command.requestId(),command.requestHash(),command.authenticatedActorId(),r.getInt(11),key.roomInstanceId(),key.lifecycleEpoch(),r.getLong(12),r.getLong(13),r.getString(14),r.getString(15)),r.getTimestamp(16).toInstant());
                } else if(!"PREPARED".equals(status))throw new IllegalStateException("unknown AA command status");
                return new Command(ordinal,r.getString(5),new PreSeatState(r.getInt(6),r.getLong(7),r.getLong(8),r.getString(9)),status,receipt);
            }
        }
    }
    void insertCommand(Connection c,Preparation p,Instant at)throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO aoo_room_aa_command(session_id,command_key,request_id,request_hash,actor_id,attempt_no,preparation_hash,pre_seat,pre_fence,pre_version,pre_snapshot_hash,status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,'PREPARED',?)")) {
            int i=1;s.setLong(i++,p.sessionId());s.setString(i++,p.command().commandKey());s.setString(i++,p.command().requestId());s.setString(i++,p.command().requestHash());s.setLong(i++,p.command().authenticatedActorId());s.setLong(i++,p.attemptNo());s.setString(i++,p.preparationHash());s.setInt(i++,p.preState().actorSeatBefore());s.setLong(i++,p.preState().fencingToken());s.setLong(i++,p.preState().stateVersion());s.setString(i++,p.preState().snapshotHash());s.setTimestamp(i,Timestamp.from(at));s.executeUpdate();
        }
    }
    void commit(Connection c,Preparation p,SeatProof proof,Instant at)throws SQLException {
        try(var s=c.prepareStatement("UPDATE aoo_room_aa_command SET status='COMMITTED',post_seat=?,post_fence=?,post_version=?,post_snapshot_hash=?,response_hash=?,committed_at=? WHERE session_id=? AND command_key=? AND status='PREPARED' AND preparation_hash=?")) {
            s.setInt(1,proof.seatId());s.setLong(2,proof.fencingToken());s.setLong(3,proof.stateVersion());s.setString(4,proof.snapshotHash());s.setString(5,proof.responseHash());s.setTimestamp(6,Timestamp.from(at));s.setLong(7,p.sessionId());s.setString(8,p.command().commandKey());s.setString(9,p.preparationHash());if(s.executeUpdate()!=1)throw new IllegalStateException("AA preparation lost");
        }
        try(var s=c.prepareStatement("UPDATE aoo_room_aa_attempt SET state='SEATED' WHERE session_id=? AND actor_id=? AND attempt_no=?")) {
            s.setLong(1,p.sessionId());s.setLong(2,p.command().authenticatedActorId());s.setLong(3,p.attemptNo());if(s.executeUpdate()!=1)throw new IllegalStateException("AA attempt lost");
        }
    }
}
