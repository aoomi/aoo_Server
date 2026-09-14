package com.aoo.bcg.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class JdbcRoomEventJournal implements RoomEventJournal {
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final EventSchemaRegistry schemas=new EventSchemaRegistry();
    public JdbcRoomEventJournal(DataSource dataSource, ObjectMapper mapper) { this.dataSource=dataSource; this.mapper=mapper; }
    @Override public void append(RoomEventIdentity identity, Object payload) {
        if(identity==null) throw new IllegalArgumentException("event identity is required");
        String sql="INSERT INTO aoo_room_event(room_id,round_no,event_sequence,business_event_id,event_type,schema_version,event_payload,created_at) VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP(3))";
        try(var connection=dataSource.getConnection(); var statement=connection.prepareStatement(sql)) {
            bindEvent(statement,identity,mapper.writeValueAsString(payload)); statement.executeUpdate();
        } catch(Exception exception) { throw new IllegalStateException("cannot append event "+identity.businessEventId(),exception); }
    }

    /** Atomically records the command event and its resulting authority snapshot. */
    public void appendAndSnapshot(RoomSnapshot snapshot, RoomEventIdentity identity, Object payload) {
        appendAndSnapshot(snapshot,identity,payload,null,Math.subtractExact(snapshot.stateVersion(),1));
    }

    /** Same database transaction is the sole mutation/event/notification durability boundary. */
    public void appendAndSnapshotAndOutbox(RoomSnapshot snapshot,RoomEventIdentity identity,Object payload,OutboxEvent outbox){
        if(outbox==null)throw new IllegalArgumentException("outbox event is required");
        appendAndSnapshot(snapshot,identity,payload,outbox,Math.subtractExact(snapshot.stateVersion(),1));
    }

    public void appendAndSnapshotAndOutboxCas(RoomSnapshot snapshot,RoomEventIdentity identity,Object payload,
                                               OutboxEvent outbox,long expectedStateVersion){
        if(outbox==null)throw new IllegalArgumentException("outbox event is required");
        appendAndSnapshot(snapshot,identity,payload,outbox,expectedStateVersion);
    }

    private void appendAndSnapshot(RoomSnapshot snapshot, RoomEventIdentity identity, Object payload,OutboxEvent outbox,
                                   long expectedStateVersion) {
        if (snapshot == null || identity == null || snapshot.roomId()!=identity.roomId()
                || snapshot.lastEventSequence()!=identity.sequence() || expectedStateVersion < 0
                || snapshot.stateVersion()!=Math.addExact(expectedStateVersion,1))
            throw new IllegalArgumentException("event, snapshot and expected stateVersion mismatch");
        String eventSql="INSERT INTO aoo_room_event(room_id,round_no,event_sequence,business_event_id,event_type,schema_version,event_payload,created_at) VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP(3))";
        String insertSnapshotSql="INSERT INTO aoo_room_snapshot(room_id,game_id,play_version,component_version,fencing_token,state_version,last_event_sequence,captured_at,state_payload) VALUES(?,?,?,?,?,?,?,?,?)";
        String updateSnapshotSql="UPDATE aoo_room_snapshot SET game_id=?,play_version=?,component_version=?,fencing_token=?,state_version=?,last_event_sequence=?,captured_at=?,state_payload=? WHERE room_id=? AND fencing_token<=? AND state_version=?";
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean exists;
                try(var current=connection.prepareStatement("SELECT fencing_token,state_version,last_event_sequence FROM aoo_room_snapshot WHERE room_id=? FOR UPDATE")) {
                    current.setLong(1,snapshot.roomId());
                    try(var result=current.executeQuery()) {
                        exists=result.next();
                        if(exists)requireCasTransition(true,result.getLong(1),result.getLong(2),result.getLong(3),
                                snapshot.fencingToken(),expectedStateVersion,snapshot.stateVersion(),snapshot.lastEventSequence());
                        else requireCasTransition(false,0,0,0,snapshot.fencingToken(),expectedStateVersion,
                                snapshot.stateVersion(),snapshot.lastEventSequence());
                    }
                }
                try(var event=connection.prepareStatement(eventSql)) {
                    bindEvent(event,identity,mapper.writeValueAsString(payload)); event.executeUpdate();
                }
                if(!exists)try(var state=connection.prepareStatement(insertSnapshotSql)) {
                    bindSnapshotInsert(state,snapshot,mapper.writeValueAsString(snapshot.authoritativeState()));
                    if(state.executeUpdate()!=1)throw new java.util.ConcurrentModificationException("room authority initial insert failed");
                } else try(var state=connection.prepareStatement(updateSnapshotSql)) {
                    state.setInt(1,snapshot.gameId());state.setString(2,snapshot.playVersion());state.setString(3,snapshot.componentVersion());
                    state.setLong(4,snapshot.fencingToken());state.setLong(5,snapshot.stateVersion());state.setLong(6,snapshot.lastEventSequence());
                    state.setTimestamp(7,Timestamp.from(snapshot.capturedAt()));state.setString(8,mapper.writeValueAsString(snapshot.authoritativeState()));
                    state.setLong(9,snapshot.roomId());state.setLong(10,snapshot.fencingToken());state.setLong(11,expectedStateVersion);
                    if(state.executeUpdate()!=1)throw new java.util.ConcurrentModificationException("room authority stateVersion compare-and-set lost");
                }
                if(outbox!=null)try(var message=connection.prepareStatement("INSERT INTO aoo_outbox(event_id,aggregate_type,aggregate_id,event_type,schema_version,payload,created_at) VALUES(?,?,?,?,?,?,?)")){
                    message.setString(1,outbox.eventId());message.setString(2,outbox.aggregateType());message.setLong(3,outbox.aggregateId());message.setString(4,outbox.eventType());message.setInt(5,outbox.schemaVersion());message.setString(6,mapper.writeValueAsString(outbox.payload()));message.setTimestamp(7,Timestamp.from(outbox.createdAt()));message.executeUpdate();
                }
                connection.commit();
            } catch(Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch(Exception exception) { throw new IllegalStateException("cannot atomically commit room event and snapshot",exception); }
    }
    private static void bindSnapshotInsert(java.sql.PreparedStatement state,RoomSnapshot snapshot,String payload)throws java.sql.SQLException{
        state.setLong(1,snapshot.roomId());state.setInt(2,snapshot.gameId());state.setString(3,snapshot.playVersion());
        state.setString(4,snapshot.componentVersion());state.setLong(5,snapshot.fencingToken());state.setLong(6,snapshot.stateVersion());
        state.setLong(7,snapshot.lastEventSequence());state.setTimestamp(8,Timestamp.from(snapshot.capturedAt()));state.setString(9,payload);
    }
    static void requireCasTransition(boolean exists,long storedFence,long storedVersion,long storedEventSequence,
                                     long nextFence,long expectedVersion,long nextVersion,long nextEventSequence){
        boolean valid=nextFence>0 && expectedVersion>=0 && nextVersion==Math.addExact(expectedVersion,1)
                && nextEventSequence==nextVersion;
        if(exists)valid&=nextFence>=storedFence&&storedVersion==expectedVersion
                && nextEventSequence==Math.addExact(storedEventSequence,1);
        else valid&=expectedVersion==0&&nextEventSequence==1;
        if(!valid)throw new java.util.ConcurrentModificationException("room authority stateVersion compare-and-set failed");
    }
    private static void bindEvent(java.sql.PreparedStatement statement, RoomEventIdentity identity,
                                  String payload) throws java.sql.SQLException {
        statement.setLong(1,identity.roomId()); statement.setInt(2,identity.roundNo());
        statement.setLong(3,identity.sequence()); statement.setString(4,identity.businessEventId());
        statement.setString(5,identity.eventType()); statement.setInt(6,identity.schemaVersion());
        statement.setString(7,payload);
    }
    @Override public List<Object> after(long roomId, long sequenceExclusive) {
        String sql="SELECT event_type,schema_version,event_payload FROM aoo_room_event WHERE room_id=? AND event_sequence>? ORDER BY event_sequence";
        try(var connection=dataSource.getConnection(); var statement=connection.prepareStatement(sql)) { statement.setLong(1,roomId); statement.setLong(2,sequenceExclusive); try(var result=statement.executeQuery()) { List<Object> events=new ArrayList<>(); while(result.next()) events.add(mapper.treeToValue(schemas.upcast(result.getString(1),result.getInt(2),mapper.readTree(result.getString(3))),Object.class)); return List.copyOf(events); } }
        catch(Exception exception) { throw new IllegalStateException("cannot load room events",exception); }
    }
    public void appendCompensation(RoomEventIdentity identity,CompensationEvent compensation){
        if(identity==null||compensation==null||!identity.eventType().endsWith(".compensated"))throw new IllegalArgumentException("compensation event type must end with .compensated");
        String sql="INSERT INTO aoo_room_event(room_id,round_no,event_sequence,business_event_id,event_type,schema_version,event_payload,is_compensation,compensates_business_event_id,compensation_reason,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP(3))";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){
            statement.setLong(1,identity.roomId());statement.setInt(2,identity.roundNo());statement.setLong(3,identity.sequence());statement.setString(4,identity.businessEventId());statement.setString(5,identity.eventType());statement.setInt(6,identity.schemaVersion());statement.setString(7,mapper.writeValueAsString(compensation.correction()));statement.setBoolean(8,true);statement.setString(9,compensation.compensatesBusinessEventId());statement.setString(10,compensation.reason());statement.executeUpdate();
        }catch(Exception error){throw new IllegalStateException("cannot append compensation event",error);}
    }
}
