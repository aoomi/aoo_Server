package com.aoo.bcg.billing;

import java.sql.*;
import java.util.*;
import static com.aoo.bcg.billing.RoomBillingSourceBinding.*;

/** Same-connection immutable source claim, serialized by the existing durable Saga row. */
public final class JdbcRoomBillingSourceBindingRepository {
    private final JdbcRoomBillingIntentJournal journal;
    public JdbcRoomBillingSourceBindingRepository(JdbcRoomBillingIntentJournal journal){this.journal=Objects.requireNonNull(journal);}
    private static void transaction(Connection c)throws SQLException{Objects.requireNonNull(c);if(c.getAutoCommit())throw new IllegalArgumentException("binding requires caller transaction");}
    private long saga(Connection c,SourceKey k,boolean lock)throws SQLException {
        try(var q=c.prepareStatement("SELECT request_hash,account_id,room_id FROM aoo_room_create_saga WHERE request_id=?"+(lock?" FOR UPDATE":""))){
            q.setString(1,k.requestId());try(var r=q.executeQuery()){
                if(!r.next()||!k.requestHash().equals(r.getString(1))||k.authenticatedActor()!=r.getLong(2))throw new IllegalArgumentException("billing Saga source mismatch");
                long room=r.getLong(3);if(room<=0)throw new IllegalArgumentException("billing Saga room invalid");return room;
            }
        }
    }
    public Optional<BoundIntent> find(Connection c,SourceKey k,boolean lock)throws SQLException {
        transaction(c);Objects.requireNonNull(k);long room=saga(c,k,lock);var result=read(c,k,lock);
        if(result!=null&&room!=result.intent().operation().roomId())throw new IllegalArgumentException("billing Saga room changed");return Optional.ofNullable(result);
    }
    public BoundIntent bind(Connection c,SourceKey key,RoomBillingIntent intent,JdbcRoomChargeCoordinator.Subject subject,AuthorityReference authority)throws SQLException {
        transaction(c);var expected=new BoundIntent(key,intent,subject,authority);long room=saga(c,key,true);
        if(room!=intent.operation().roomId())throw new IllegalArgumentException("billing Saga room mismatch");
        var existing=read(c,key,true);if(existing!=null){same(existing,expected,null);return existing;}
        var stored=journal.append(c,intent).intent();expected=new BoundIntent(key,stored,subject,authority);
        String sql="INSERT INTO aoo_room_billing_source_binding(source_namespace,source_request_id,source_request_hash,authenticated_actor_id,operation_key,initiating_actor_id,creator_id,source_club_id,subject_command_hash,release_id,index_generation,runtime_source_hash,classification_region_code,binding_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var q=c.prepareStatement(sql)) {
            int n=1;q.setString(n++,key.namespace());q.setString(n++,key.requestId());q.setString(n++,key.requestHash());q.setLong(n++,key.authenticatedActor());q.setString(n++,stored.operation().operationKey());
            q.setLong(n++,subject.initiatingActorId());q.setLong(n++,subject.creatorId());q.setLong(n++,subject.sourceClubId());q.setString(n++,subject.commandHash(stored.operation()));
            q.setLong(n++,authority.releaseId());q.setLong(n++,authority.indexGeneration());q.setString(n++,authority.runtimeSourceHash());q.setString(n++,authority.classificationRegionCode());q.setString(n++,expected.bindingHash());q.setTimestamp(n,Timestamp.from(stored.createdAt()));q.executeUpdate();
        } catch(SQLException failure) {
            if(failure.getSQLState()==null||!failure.getSQLState().startsWith("23"))throw failure;
            existing=read(c,key,true);if(existing==null)throw failure;same(existing,expected,failure);return existing;
        }
        return Objects.requireNonNull(read(c,key,true));
    }
    private BoundIntent read(Connection c,SourceKey key,boolean lock)throws SQLException {
        try(var q=c.prepareStatement("SELECT source_request_hash,authenticated_actor_id,operation_key,initiating_actor_id,creator_id,source_club_id,subject_command_hash,release_id,index_generation,runtime_source_hash,classification_region_code,binding_hash,created_at FROM aoo_room_billing_source_binding WHERE source_namespace=? AND source_request_id=?"+(lock?" FOR UPDATE":""))) {
            q.setString(1,key.namespace());q.setString(2,key.requestId());try(var r=q.executeQuery()) {
                if(!r.next())return null;
                if(!key.requestHash().equals(r.getString(1))||key.authenticatedActor()!=r.getLong(2))throw new IllegalArgumentException("billing source identity conflict");
                var entry=journal.find(c,r.getString(3));if(entry==null)throw new IllegalStateException("billing source intent missing");
                var subject=new JdbcRoomChargeCoordinator.Subject(r.getLong(4),r.getLong(5),r.getLong(6));
                if(!subject.commandHash(entry.intent().operation()).equals(r.getString(7))||!entry.intent().createdAt().equals(r.getTimestamp(13).toInstant()))throw new IllegalStateException("billing source integrity mismatch");
                return new BoundIntent(key,entry.intent(),subject,new AuthorityReference(r.getLong(8),r.getLong(9),r.getString(10),r.getString(11)),r.getString(12));
            }
        }
    }
    private static void same(BoundIntent a,BoundIntent b,SQLException cause) {
        if(!a.key().equals(b.key())||!a.intent().operation().equals(b.intent().operation())||!a.subject().equals(b.subject())||!a.authority().equals(b.authority())||!a.bindingHash().equals(b.bindingHash()))throw new IllegalArgumentException("immutable billing source conflict",cause);
    }
}
