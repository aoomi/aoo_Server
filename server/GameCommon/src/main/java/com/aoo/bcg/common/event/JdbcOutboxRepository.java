package com.aoo.bcg.common.event;

import com.aoo.bcg.common.persistence.JdbcSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcOutboxRepository implements OutboxRepository {
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    public JdbcOutboxRepository(DataSource dataSource, ObjectMapper mapper) { this.dataSource = dataSource; this.mapper = mapper; }

    @Override public void append(OutboxEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) throw new IllegalArgumentException("eventId is required");
        String sql = "INSERT INTO aoo_outbox(event_id,aggregate_type,aggregate_id,event_type,schema_version,payload,created_at) VALUES(?,?,?,?,?,?,?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.eventId()); statement.setString(2, event.aggregateType()); statement.setLong(3, event.aggregateId());
            statement.setString(4,event.eventType());statement.setInt(5,event.schemaVersion());statement.setString(6,mapper.writeValueAsString(event.payload()));statement.setTimestamp(7,Timestamp.from(event.createdAt()));
            statement.executeUpdate();
        } catch (Exception exception) { throw new IllegalStateException("cannot append outbox event", exception); }
    }

    @Override public List<OutboxClaim> claim(int limit,String workerId,java.time.Instant now,java.time.Duration lease) {
        if(limit<=0||workerId==null||workerId.isBlank()||now==null||lease==null||lease.isNegative()||lease.isZero())return List.of();
        int safeLimit=Math.min(limit,500);java.time.Instant lockedUntil=now.plus(lease);
        String select="SELECT event_id,aggregate_type,aggregate_id,event_type,schema_version,payload,created_at,retry_count FROM aoo_outbox WHERE (status='PENDING' AND (next_attempt_at IS NULL OR next_attempt_at<=?)) OR (status='PROCESSING' AND locked_until<?) ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED";
        try(var connection=dataSource.getConnection()){connection.setAutoCommit(false);try{
            List<OutboxClaim> claims=new ArrayList<>();
            try(var statement=connection.prepareStatement(select)){statement.setTimestamp(1,Timestamp.from(now));statement.setTimestamp(2,Timestamp.from(now));statement.setInt(3,safeLimit);try(var result=statement.executeQuery()){
                while(result.next()){OutboxEvent event=new OutboxEvent(result.getString(1),result.getString(2),result.getLong(3),result.getString(4),result.getInt(5),mapper.readValue(result.getString(6),Object.class),result.getTimestamp(7).toInstant());claims.add(new OutboxClaim(event,workerId,UUID.randomUUID().toString(),lockedUntil,result.getInt(8)+1));}
            }}
            try(var update=connection.prepareStatement("UPDATE aoo_outbox SET status='PROCESSING',locked_by=?,claim_token=?,locked_until=?,retry_count=retry_count+1 WHERE event_id=?")){for(OutboxClaim claim:claims){update.setString(1,workerId);update.setString(2,claim.claimToken());update.setTimestamp(3,Timestamp.from(lockedUntil));update.setString(4,claim.event().eventId());update.addBatch();}update.executeBatch();}
            connection.commit();return List.copyOf(claims);
        }catch(Exception error){connection.rollback();throw error;}finally{connection.setAutoCommit(true);}}
        catch(Exception exception){throw new IllegalStateException("cannot claim outbox events",exception);}
    }

    @Override public void markPublished(OutboxClaim claim) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("UPDATE aoo_outbox SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP(3),locked_by=NULL,claim_token=NULL,locked_until=NULL WHERE event_id=? AND status='PROCESSING' AND locked_by=? AND claim_token=?")) {
            statement.setString(1, claim.event().eventId());statement.setString(2,claim.workerId());statement.setString(3,claim.claimToken());
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("outbox claim ownership lost");
        } catch (java.sql.SQLException exception) { throw JdbcSupport.failure("mark outbox published", exception); }
    }

    @Override public boolean recordFailure(OutboxClaim claim, String error, java.time.Instant nextAttemptAt,int maxAttempts) {
        if (claim == null || nextAttemptAt == null || maxAttempts<=0) throw new IllegalArgumentException("failure event and retry time are required");
        String safeError = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 500));
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("UPDATE aoo_outbox SET status=IF(retry_count>=?,'DEAD','PENDING'),last_error=?,next_attempt_at=IF(retry_count>=?,NULL,?),dead_at=IF(retry_count>=?,CURRENT_TIMESTAMP(3),NULL),locked_by=NULL,claim_token=NULL,locked_until=NULL WHERE event_id=? AND status='PROCESSING' AND locked_by=? AND claim_token=?")) {
            statement.setInt(1,maxAttempts);statement.setString(2,safeError);statement.setInt(3,maxAttempts);statement.setTimestamp(4,Timestamp.from(nextAttemptAt));statement.setInt(5,maxAttempts);statement.setString(6,claim.event().eventId());statement.setString(7,claim.workerId());statement.setString(8,claim.claimToken());
            if(statement.executeUpdate()!=1)throw new IllegalStateException("outbox claim ownership lost");return claim.attempt()>=maxAttempts;
        } catch (java.sql.SQLException exception) { throw JdbcSupport.failure("record outbox failure", exception); }
    }
    @Override public int purgeTerminalBefore(java.time.Instant cutoff,int limit){if(cutoff==null||limit<=0)return 0;try(var connection=dataSource.getConnection();var statement=connection.prepareStatement("DELETE FROM aoo_outbox WHERE (status='PUBLISHED' AND published_at<?) OR (status='DEAD' AND dead_at<?) ORDER BY created_at LIMIT ?")){statement.setTimestamp(1,Timestamp.from(cutoff));statement.setTimestamp(2,Timestamp.from(cutoff));statement.setInt(3,Math.min(limit,1000));return statement.executeUpdate();}catch(java.sql.SQLException error){throw JdbcSupport.failure("purge terminal outbox",error);}}
    @Override public long deadLetterCount(){try(var connection=dataSource.getConnection();var statement=connection.prepareStatement("SELECT COUNT(*) FROM aoo_outbox WHERE status='DEAD'");var result=statement.executeQuery()){result.next();return result.getLong(1);}catch(java.sql.SQLException error){throw JdbcSupport.failure("count dead outbox",error);}}
    @Override public OutboxBacklog backlog() {
        String sql = "SELECT COUNT(*),COALESCE(SUM(retry_count>0),0),MIN(created_at) FROM aoo_outbox WHERE status IN ('PENDING','PROCESSING','DEAD')";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql); var result = statement.executeQuery()) {
            result.next(); var oldest = result.getTimestamp(3);
            return new OutboxBacklog(result.getLong(1), result.getLong(2),java.util.Optional.ofNullable(oldest).map(Timestamp::toInstant));
        } catch (java.sql.SQLException exception) { throw JdbcSupport.failure("read outbox backlog", exception); }
    }
}
