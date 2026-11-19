package com.aoo.bcg.billing;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.aoo.bcg.billing.RoomBillingSourceBinding.*;
import static com.aoo.bcg.billing.RoomBillingBusinessOutcome.EventType.*;

/** All outcome writers fence on the same immutable source binding row in the caller transaction. */
public final class JdbcRoomBillingBusinessOutcomeRepository {
    private final JdbcRoomBillingSourceBindingRepository bindings;
    public JdbcRoomBillingBusinessOutcomeRepository(JdbcRoomBillingSourceBindingRepository bindings){this.bindings=Objects.requireNonNull(bindings);}
    private BoundIntent stored(Connection c,SourceKey key,boolean lock)throws SQLException{return bindings.find(c,key,lock).orElseThrow(()->new IllegalArgumentException("billing source missing"));}
    private BoundIntent matching(Connection c,BoundIntent b)throws SQLException{var actual=stored(c,b.key(),true);if(!actual.equals(b))throw new IllegalArgumentException("business source envelope conflict");return actual;}
    public Optional<RoomBillingBusinessOutcome> find(Connection c,SourceKey key,RoomBillingBusinessOutcome.EventType event)throws SQLException {var b=stored(c,key,false);return Optional.ofNullable(read(c,b,event));}
    public RoomBillingBusinessOutcome recordCommit(Connection c,BoundIntent b,RoomBillingBusinessOutcome outcome)throws SQLException {
        matching(c,b);if(outcome.eventType()!=CREATE_COMMITTED)throw new IllegalArgumentException("expected commit receipt");
        if(read(c,b,CREATE_ABORTED_BEFORE_COMMIT)!=null)throw new IllegalStateException("create producer fenced by abort");return write(c,b,outcome);
    }
    public RoomBillingBusinessOutcome abortBeforeCommit(Connection c,SourceKey key)throws SQLException {
        var b=stored(c,key,true);if(read(c,b,CREATE_COMMITTED)!=null)throw new IllegalStateException("committed create cannot abort");
        var op=b.intent().operation();return write(c,b,new RoomBillingBusinessOutcome(CREATE_ABORTED_BEFORE_COMMIT,op.roomId(),b.subject().creatorId(),null,null,null,null,op.gameId(),op.playVersion(),Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    }
    public RoomBillingBusinessOutcome recordDestroy(Connection c,BoundIntent b,RoomBillingBusinessOutcome outcome)throws SQLException {
        matching(c,b);if(outcome.eventType()!=CREATE_DESTROYED)throw new IllegalArgumentException("expected destroy receipt");
        var original=read(c,b,CREATE_COMMITTED);
        if(original==null||!Objects.equals(original.roomInstanceId(),outcome.roomInstanceId())||!Objects.equals(original.lifecycleEpoch(),outcome.lifecycleEpoch())||!Objects.equals(original.fencingToken(),outcome.fencingToken())||outcome.stateVersion()<original.stateVersion())throw new IllegalArgumentException("destroy lacks exact committed identity");return write(c,b,outcome);
    }
    private void context(BoundIntent b,RoomBillingBusinessOutcome o) {
        var op=b.intent().operation();if(op.roomId()!=o.roomId()||b.subject().creatorId()!=o.creatorId()||op.gameId()!=o.gameId()||!op.playVersion().equals(o.playVersion()))throw new IllegalArgumentException("business outcome context mismatch");
    }
    private RoomBillingBusinessOutcome write(Connection c,BoundIntent b,RoomBillingBusinessOutcome o)throws SQLException {
        context(b,o);var old=read(c,b,o.eventType());if(old!=null){same(old,o,null);return old;}
        String sql="INSERT INTO aoo_room_billing_business_receipt(source_namespace,source_request_id,event_type,operation_key,room_id,creator_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,game_id,play_version,outcome_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var q=c.prepareStatement(sql)){
            int i=1;q.setString(i++,b.key().namespace());q.setString(i++,b.key().requestId());q.setString(i++,o.eventType().name());q.setString(i++,b.intent().operation().operationKey());q.setLong(i++,o.roomId());q.setLong(i++,o.creatorId());q.setString(i++,o.roomInstanceId());q.setObject(i++,o.lifecycleEpoch(),Types.BIGINT);q.setObject(i++,o.fencingToken(),Types.BIGINT);q.setObject(i++,o.stateVersion(),Types.BIGINT);q.setLong(i++,o.gameId());q.setString(i++,o.playVersion());q.setString(i++,o.hash());q.setTimestamp(i,Timestamp.from(o.createdAt()));q.executeUpdate();
        }catch(SQLException error){if(error.getSQLState()==null||!error.getSQLState().startsWith("23"))throw error;old=read(c,b,o.eventType());if(old==null)throw error;same(old,o,error);return old;}return Objects.requireNonNull(read(c,b,o.eventType()));
    }
    private RoomBillingBusinessOutcome read(Connection c,BoundIntent b,RoomBillingBusinessOutcome.EventType type)throws SQLException {
        Objects.requireNonNull(type);try(var q=c.prepareStatement("SELECT operation_key,room_id,creator_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,game_id,play_version,outcome_hash,created_at FROM aoo_room_billing_business_receipt WHERE source_namespace=? AND source_request_id=? AND event_type=?")) {
            q.setString(1,b.key().namespace());q.setString(2,b.key().requestId());q.setString(3,type.name());try(var r=q.executeQuery()){
                if(!r.next())return null;if(!b.intent().operation().operationKey().equals(r.getString(1)))throw new IllegalStateException("business receipt operation mismatch");
                var o=new RoomBillingBusinessOutcome(type,r.getLong(2),r.getLong(3),r.getString(4),r.getObject(5,Long.class),r.getObject(6,Long.class),r.getObject(7,Long.class),r.getLong(8),r.getString(9),r.getTimestamp(11).toInstant());context(b,o);if(!o.hash().equals(r.getString(10)))throw new IllegalStateException("business receipt hash mismatch");return o;
            }
        }
    }
    private static void same(RoomBillingBusinessOutcome old,RoomBillingBusinessOutcome value,SQLException cause){if(!old.hash().equals(value.hash()))throw new IllegalArgumentException("immutable business outcome conflict",cause);}
}
