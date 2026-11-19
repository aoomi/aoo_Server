package com.aoo.bcg.common.event;

import com.aoo.bcg.common.persistence.JdbcSupport;
import java.time.Duration;
import java.util.Objects;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;

public final class JdbcConsumedEventStore {
    private final DataSource dataSource;
    private final String consumerName;
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final AuthoritativeTimeSource time;

    public JdbcConsumedEventStore(DataSource dataSource, String consumerName) {
        this(dataSource, consumerName, AuthoritativeTimeSource.systemUtc());
    }

    public JdbcConsumedEventStore(DataSource dataSource, String consumerName, AuthoritativeTimeSource time) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        if (consumerName == null || consumerName.isBlank() || consumerName.length() > 128)
            throw new IllegalArgumentException("consumerName is required");
        this.consumerName = consumerName;
        this.time = Objects.requireNonNull(time, "time");
    }

    public boolean alreadyProcessed(String eventId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT 1 FROM aoo_consumed_event WHERE consumer_name=? AND event_id=? AND status='PROCESSED'")) {
            statement.setString(1, consumerName);
            statement.setString(2, eventId);
            try (var rows = statement.executeQuery()) { return rows.next(); }
        } catch (java.sql.SQLException exception) {
            throw JdbcSupport.failure("check consumed event", exception);
        }
    }

    public boolean claim(String eventId, Duration staleAfter) {
        return claimLease(eventId, staleAfter).isPresent();
    }

    public java.util.Optional<ConsumedEventClaim> claimLease(String eventId, Duration staleAfter) {
        Objects.requireNonNull(staleAfter, "staleAfter");
        if (staleAfter.isNegative() || staleAfter.isZero()) throw new IllegalArgumentException("staleAfter must be positive");
        if (eventId == null || eventId.isBlank() || eventId.length() > 64) throw new IllegalArgumentException("eventId is required");
        String token=java.util.UUID.randomUUID().toString();
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var insert = connection.prepareStatement(
                        "INSERT IGNORE INTO aoo_consumed_event(consumer_name,event_id,status,claimed_at,claim_token,processed_at) "
                                + "VALUES(?,?,'PROCESSING',CURRENT_TIMESTAMP(3),?,NULL)")) {
                    insert.setString(1, consumerName); insert.setString(2, eventId);
                    insert.setString(3,token);
                    if (insert.executeUpdate() == 1) { connection.commit(); return java.util.Optional.of(new ConsumedEventClaim(eventId,token)); }
                }
                try (var lock = connection.prepareStatement(
                        "SELECT status,(claimed_at IS NULL OR claimed_at < DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL ? SECOND)) "
                                + "FROM aoo_consumed_event "
                                + "WHERE consumer_name=? AND event_id=? FOR UPDATE")) {
                    lock.setLong(1, Math.max(1, staleAfter.toSeconds()));
                    lock.setString(2, consumerName); lock.setString(3, eventId);
                    try (var row = lock.executeQuery()) {
                        if (!row.next() || "PROCESSED".equals(row.getString(1)) || "DEAD".equals(row.getString(1))) { connection.commit(); return java.util.Optional.empty(); }
                        if (!row.getBoolean(2)) { connection.commit(); return java.util.Optional.empty(); }
                    }
                }
                try (var reclaim = connection.prepareStatement(
                        "UPDATE aoo_consumed_event SET status='PROCESSING',claimed_at=CURRENT_TIMESTAMP(3),claim_token=? "
                                + "WHERE consumer_name=? AND event_id=? AND status IN ('PROCESSING','RETRY')")) {
                    reclaim.setString(1,token);reclaim.setString(2, consumerName); reclaim.setString(3, eventId); reclaim.executeUpdate();
                }
                connection.commit();
                return java.util.Optional.of(new ConsumedEventClaim(eventId,token));
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally { connection.setAutoCommit(true); }
        } catch (java.sql.SQLException exception) {
            throw JdbcSupport.failure("claim consumed event", exception);
        }
    }

    public void complete(String eventId,ConsumedEventResult result) {
        Objects.requireNonNull(result,"result");
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "UPDATE aoo_consumed_event SET status='PROCESSED',processed_at=CURRENT_TIMESTAMP(3),result_code=?,result_schema_version=?,result_payload=? "
                        + "WHERE consumer_name=? AND event_id=? AND status='PROCESSING'")) {
            statement.setInt(1,result.code());statement.setInt(2,result.schemaVersion());
            statement.setString(3,mapper.writeValueAsString(result.data()));statement.setString(4, consumerName);
            statement.setString(5, eventId);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("event was not claimed");
        } catch (Exception exception) {
            throw JdbcSupport.failure("mark consumed event", exception);
        }
    }

    public void complete(ConsumedEventClaim claim,ConsumedEventResult result) {
        Objects.requireNonNull(claim,"claim");Objects.requireNonNull(result,"result");
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement("UPDATE aoo_consumed_event SET status='PROCESSED',processed_at=CURRENT_TIMESTAMP(3),claim_token=NULL,result_code=?,result_schema_version=?,result_payload=? WHERE consumer_name=? AND event_id=? AND status='PROCESSING' AND claim_token=?")){
            statement.setInt(1,result.code());statement.setInt(2,result.schemaVersion());statement.setString(3,mapper.writeValueAsString(result.data()));statement.setString(4,consumerName);statement.setString(5,claim.eventId());statement.setString(6,claim.token());if(statement.executeUpdate()!=1)throw new IllegalStateException("consumed event lease lost");
        }catch(Exception error){if(error instanceof IllegalStateException state)throw state;throw JdbcSupport.failure("complete fenced consumed event",error);}
    }

    public java.util.Optional<ConsumedEventResult> previousResult(String eventId){
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement("SELECT result_code,result_schema_version,result_payload FROM aoo_consumed_event WHERE consumer_name=? AND event_id=? AND status='PROCESSED'")){
            statement.setString(1,consumerName);statement.setString(2,eventId);try(var row=statement.executeQuery()){
                if(!row.next())return java.util.Optional.empty();
                @SuppressWarnings("unchecked") var data=mapper.readValue(row.getString(3),java.util.Map.class);
                return java.util.Optional.of(new ConsumedEventResult(row.getInt(1),row.getInt(2),data));
            }
        }catch(Exception error){throw JdbcSupport.failure("read consumed event result",error);}
    }

    public void abandon(String eventId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "DELETE FROM aoo_consumed_event WHERE consumer_name=? AND event_id=? AND status='PROCESSING'")) {
            statement.setString(1, consumerName); statement.setString(2, eventId); statement.executeUpdate();
        } catch (java.sql.SQLException exception) {
            throw JdbcSupport.failure("abandon consumed event", exception);
        }
    }

    public boolean recordFailure(OutboxEvent event,Throwable error,int maxAttempts){
        Objects.requireNonNull(event,"event");if(maxAttempts<=0)throw new IllegalArgumentException("maxAttempts must be positive");
        String safe=error==null?"unknown":String.valueOf(error.getMessage());safe=safe.substring(0,Math.min(500,safe.length()));
        try(var connection=dataSource.getConnection()){connection.setAutoCommit(false);try{
            int attempts;try(var lock=connection.prepareStatement("SELECT failure_count FROM aoo_consumed_event WHERE consumer_name=? AND event_id=? AND status='PROCESSING' FOR UPDATE")){lock.setString(1,consumerName);lock.setString(2,event.eventId());try(var row=lock.executeQuery()){if(!row.next())throw new IllegalStateException("event was not claimed");attempts=row.getInt(1)+1;}}
            boolean dead=attempts>=maxAttempts;try(var update=connection.prepareStatement("UPDATE aoo_consumed_event SET status=?,claimed_at=NULL,failure_count=?,last_error=?,dead_payload=?,dead_at=? WHERE consumer_name=? AND event_id=?")){update.setString(1,dead?"DEAD":"RETRY");update.setInt(2,attempts);update.setString(3,safe);update.setString(4,mapper.writeValueAsString(event));update.setTimestamp(5,dead?java.sql.Timestamp.from(time.now()):null);update.setString(6,consumerName);update.setString(7,event.eventId());update.executeUpdate();}
            connection.commit();return dead;
        }catch(Exception failure){connection.rollback();throw failure;}finally{connection.setAutoCommit(true);}}
        catch(Exception failure){throw JdbcSupport.failure("record consumed event failure",failure);}
    }

    public boolean recordFailure(ConsumedEventClaim claim,OutboxEvent event,Throwable error,int maxAttempts){
        Objects.requireNonNull(claim,"claim");Objects.requireNonNull(event,"event");if(!claim.eventId().equals(event.eventId()))throw new IllegalArgumentException("claim/event mismatch");if(maxAttempts<=0)throw new IllegalArgumentException("maxAttempts must be positive");String safe=error==null?"unknown":String.valueOf(error.getMessage());safe=safe.substring(0,Math.min(500,safe.length()));
        try(var connection=dataSource.getConnection()){connection.setAutoCommit(false);try{int attempts;try(var lock=connection.prepareStatement("SELECT failure_count FROM aoo_consumed_event WHERE consumer_name=? AND event_id=? AND status='PROCESSING' AND claim_token=? FOR UPDATE")){lock.setString(1,consumerName);lock.setString(2,event.eventId());lock.setString(3,claim.token());try(var row=lock.executeQuery()){if(!row.next())throw new IllegalStateException("consumed event lease lost");attempts=row.getInt(1)+1;}}boolean dead=attempts>=maxAttempts;try(var update=connection.prepareStatement("UPDATE aoo_consumed_event SET status=?,claimed_at=NULL,claim_token=NULL,failure_count=?,last_error=?,dead_payload=?,dead_at=? WHERE consumer_name=? AND event_id=? AND claim_token=?")){update.setString(1,dead?"DEAD":"RETRY");update.setInt(2,attempts);update.setString(3,safe);update.setString(4,mapper.writeValueAsString(event));update.setTimestamp(5,dead?java.sql.Timestamp.from(time.now()):null);update.setString(6,consumerName);update.setString(7,event.eventId());update.setString(8,claim.token());if(update.executeUpdate()!=1)throw new IllegalStateException("consumed event lease lost");}connection.commit();return dead;}catch(Exception failure){connection.rollback();throw failure;}finally{connection.setAutoCommit(true);}}catch(Exception failure){if(failure instanceof IllegalStateException state)throw state;throw JdbcSupport.failure("record fenced consumed event failure",failure);}
    }

    public int cleanup(java.time.Instant processedBefore,java.time.Instant deadBefore,int limit){
        Objects.requireNonNull(processedBefore);Objects.requireNonNull(deadBefore);if(limit<1||limit>10_000)throw new IllegalArgumentException("invalid cleanup limit");String sql="DELETE FROM aoo_consumed_event WHERE consumer_name=? AND ((status='PROCESSED' AND processed_at<?) OR (status='DEAD' AND dead_at<?)) ORDER BY COALESCE(processed_at,dead_at) LIMIT ?";try(var c=dataSource.getConnection();var q=c.prepareStatement(sql)){q.setString(1,consumerName);q.setTimestamp(2,java.sql.Timestamp.from(processedBefore));q.setTimestamp(3,java.sql.Timestamp.from(deadBefore));q.setInt(4,limit);return q.executeUpdate();}catch(Exception e){throw JdbcSupport.failure("cleanup consumed events",e);}
    }

    public OutboxEvent prepareReplay(String eventId,String expectedPayloadSha256,long operatorId,String reason){
        if(eventId==null||eventId.isBlank()||expectedPayloadSha256==null||!expectedPayloadSha256.matches("[0-9a-f]{64}")||operatorId<=0||reason==null||reason.isBlank()||reason.length()>500)throw new IllegalArgumentException("invalid dead-letter replay approval");
        try(var connection=dataSource.getConnection()){connection.setAutoCommit(false);try{
            String payload;try(var lock=connection.prepareStatement("SELECT dead_payload FROM aoo_consumed_event WHERE consumer_name=? AND event_id=? AND status='DEAD' FOR UPDATE")){lock.setString(1,consumerName);lock.setString(2,eventId);try(var row=lock.executeQuery()){if(!row.next())throw new IllegalStateException("dead letter not found");payload=row.getString(1);}}
            String actual=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));if(!actual.equals(expectedPayloadSha256))throw new SecurityException("dead-letter payload hash mismatch");
            try(var audit=connection.prepareStatement("INSERT INTO aoo_dead_letter_replay_audit(replay_id,consumer_name,event_id,payload_sha256,operator_id,reason,replayed_at) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP(3))")){audit.setString(1,java.util.UUID.randomUUID().toString());audit.setString(2,consumerName);audit.setString(3,eventId);audit.setString(4,actual);audit.setLong(5,operatorId);audit.setString(6,reason);audit.executeUpdate();}
            try(var reset=connection.prepareStatement("UPDATE aoo_consumed_event SET status='RETRY',claimed_at=NULL,dead_at=NULL,replay_count=replay_count+1 WHERE consumer_name=? AND event_id=? AND status='DEAD'")){reset.setString(1,consumerName);reset.setString(2,eventId);if(reset.executeUpdate()!=1)throw new IllegalStateException("dead-letter state changed");}
            OutboxEvent event=mapper.readValue(payload,OutboxEvent.class);connection.commit();return event;
        }catch(Exception failure){connection.rollback();throw failure;}finally{connection.setAutoCommit(true);}}
        catch(Exception failure){throw JdbcSupport.failure("prepare dead-letter replay",failure);}
    }
}
