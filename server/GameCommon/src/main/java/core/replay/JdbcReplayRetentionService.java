package core.replay;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public final class JdbcReplayRetentionService implements ReplayRetentionService {
    private static final int MAX_BATCH_SIZE = 5_000;
    private final DataSource dataSource;

    public JdbcReplayRetentionService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public int archiveExpiredEvents(Instant expiredBefore, int requestedBatchSize) {
        Objects.requireNonNull(expiredBefore);int batchSize=Math.max(1,Math.min(requestedBatchSize,MAX_BATCH_SIZE));
        try(var c=dataSource.getConnection()){c.setAutoCommit(false);try{
            int archived;String copy="INSERT IGNORE INTO perspective_replay_event_archive(room_id,set_id,event_sequence,visibility,owner_player_id,message_id,schema_version,play_version,payload,created_at,archived_at) SELECT room_id,set_id,event_sequence,visibility,owner_player_id,message_id,schema_version,play_version,payload,created_at,CURRENT_TIMESTAMP(3) FROM perspective_replay_event WHERE created_at<? ORDER BY created_at LIMIT ?";try(var q=c.prepareStatement(copy)){q.setTimestamp(1,Timestamp.from(expiredBefore));q.setInt(2,batchSize);archived=q.executeUpdate();}
            String remove="DELETE FROM perspective_replay_event WHERE created_at<? AND EXISTS(SELECT 1 FROM perspective_replay_event_archive a WHERE a.room_id=perspective_replay_event.room_id AND a.set_id=perspective_replay_event.set_id AND a.event_sequence=perspective_replay_event.event_sequence AND a.visibility=perspective_replay_event.visibility AND a.owner_player_id=perspective_replay_event.owner_player_id) ORDER BY created_at LIMIT ?";try(var q=c.prepareStatement(remove)){q.setTimestamp(1,Timestamp.from(expiredBefore));q.setInt(2,batchSize);q.executeUpdate();}c.commit();return archived;
        }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(Exception e){throw new IllegalStateException("cannot archive expired replay data",e);}
    }

    @Override
    public int deleteOrphanParticipants(Instant expiredBefore, int batchSize) {
        Objects.requireNonNull(expiredBefore);int bounded=Math.max(1,Math.min(batchSize,MAX_BATCH_SIZE));try(var c=dataSource.getConnection()){c.setAutoCommit(false);try{int copied;String copy="INSERT IGNORE INTO replay_participant_archive(room_id,set_id,player_id,seat_id,granted_at,archived_at) SELECT p.room_id,p.set_id,p.player_id,p.seat_id,p.granted_at,CURRENT_TIMESTAMP(3) FROM replay_participant p WHERE p.granted_at<? AND NOT EXISTS(SELECT 1 FROM perspective_replay_event e WHERE e.room_id=p.room_id AND e.set_id=p.set_id) ORDER BY p.granted_at LIMIT ?";try(var q=c.prepareStatement(copy)){q.setTimestamp(1,Timestamp.from(expiredBefore));q.setInt(2,bounded);copied=q.executeUpdate();}try(var q=c.prepareStatement("DELETE FROM replay_participant WHERE granted_at<? AND EXISTS(SELECT 1 FROM replay_participant_archive a WHERE a.room_id=replay_participant.room_id AND a.set_id=replay_participant.set_id AND a.player_id=replay_participant.player_id) ORDER BY granted_at LIMIT ?")){q.setTimestamp(1,Timestamp.from(expiredBefore));q.setInt(2,bounded);q.executeUpdate();}c.commit();return copied;}catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(Exception e){throw new IllegalStateException("cannot archive replay participants",e);}
    }

    @Override
    public int deleteExpiredArchives(Instant deleteBefore,int requestedBatchSize){
        Objects.requireNonNull(deleteBefore,"deleteBefore");int batch=Math.max(1,Math.min(requestedBatchSize,MAX_BATCH_SIZE));
        try(var c=dataSource.getConnection()){c.setAutoCommit(false);try{
            int removed;String events="DELETE e FROM perspective_replay_event_archive e JOIN replay_set_manifest m ON m.room_id=e.room_id AND m.set_id=e.set_id WHERE m.legal_hold=FALSE AND m.delete_after<? ORDER BY m.delete_after,e.room_id,e.set_id,e.event_sequence LIMIT ?";
            try(var q=c.prepareStatement(events)){q.setTimestamp(1,Timestamp.from(deleteBefore));q.setInt(2,batch);removed=q.executeUpdate();}
            String participants="DELETE p FROM replay_participant_archive p JOIN replay_set_manifest m ON m.room_id=p.room_id AND m.set_id=p.set_id WHERE m.legal_hold=FALSE AND m.delete_after<? AND NOT EXISTS(SELECT 1 FROM perspective_replay_event_archive e WHERE e.room_id=p.room_id AND e.set_id=p.set_id) LIMIT ?";
            try(var q=c.prepareStatement(participants)){q.setTimestamp(1,Timestamp.from(deleteBefore));q.setInt(2,batch);q.executeUpdate();}
            try(var q=c.prepareStatement("DELETE FROM replay_set_manifest WHERE legal_hold=FALSE AND delete_after<? AND NOT EXISTS(SELECT 1 FROM perspective_replay_event_archive e WHERE e.room_id=replay_set_manifest.room_id AND e.set_id=replay_set_manifest.set_id) LIMIT ?")){q.setTimestamp(1,Timestamp.from(deleteBefore));q.setInt(2,batch);q.executeUpdate();}
            c.commit();return removed;
        }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
        catch(Exception e){throw new IllegalStateException("cannot delete expired replay archives",e);}
    }

    private int delete(String sql, Instant expiredBefore, int requestedBatchSize) {
        Objects.requireNonNull(expiredBefore, "expiredBefore");
        int batchSize = Math.max(1, Math.min(requestedBatchSize, MAX_BATCH_SIZE));
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(expiredBefore));
            statement.setInt(2, batchSize);
            return statement.executeUpdate();
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException("cannot clean expired replay data", error);
        }
    }
}
