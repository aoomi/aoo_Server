package com.aoo.bcg.common.idempotency;

import com.aoo.bcg.common.persistence.JdbcSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Objects;
import com.aoo.bcg.common.serialization.DomainJsonDecoder;

public final class JdbcIdempotencyStore<R> implements IdempotencyStore<R> {
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final Class<R> resultType;
    private final Clock clock;
    private final DomainJsonDecoder decoder;

    public JdbcIdempotencyStore(DataSource dataSource, ObjectMapper mapper, Class<R> resultType, Clock clock) {
        this.dataSource = dataSource;
        this.mapper = mapper;
        this.resultType = resultType;
        this.clock = clock;
        this.decoder = new DomainJsonDecoder(mapper);
    }

    @Override public Optional<IdempotencyResult<R>> findResult(IdempotencyKey key) {
        String sql = "SELECT response_code,response_version,response_schema_version,response_payload,expires_at FROM aoo_business_idempotency WHERE request_id=? AND status='COMPLETED'";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.storageKey());
            try (var result = statement.executeQuery()) {
                if (!result.next() || !result.getTimestamp(5).toInstant().isAfter(clock.instant())) return Optional.empty();
                R decoded = decoder.decode(result.getString(4), resultType, Objects::requireNonNull);
                return Optional.of(new IdempotencyResult<>(result.getInt(1),result.getString(2),result.getInt(3),decoded,result.getTimestamp(5).toInstant()));
            }
        } catch (Exception exception) {
            if (exception instanceof java.sql.SQLException sqlException) throw JdbcSupport.failure("find idempotency", sqlException);
            throw new IllegalStateException("cannot deserialize idempotency result", exception);
        }
    }

    @Override public boolean acquire(IdempotencyKey key, Duration retention) {
        validate(key, retention);String requestId=key.storageKey();
        Instant now = clock.instant();
        String delete = "DELETE FROM aoo_business_idempotency WHERE request_id=? AND expires_at<=?";
        String insert = "INSERT IGNORE INTO aoo_business_idempotency(request_id,request_hash,user_id,operation,room_id,round_no,client_request_id,status,response_payload,created_at,expires_at) VALUES(?,SHA2(?,256),?,?,?,?,?,'PROCESSING','{}',?,?)";
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.prepareStatement(delete)) { statement.setString(1, requestId); statement.setTimestamp(2, Timestamp.from(now)); statement.executeUpdate(); }
            try (var statement = connection.prepareStatement(insert)) {
                statement.setString(1,requestId);statement.setString(2,requestId);statement.setString(3,key.userId());statement.setString(4,key.operation());statement.setLong(5,key.roomId());statement.setInt(6,key.roundNo());statement.setString(7,key.requestId());statement.setTimestamp(8,Timestamp.from(now));statement.setTimestamp(9,Timestamp.from(now.plus(retention)));
                return statement.executeUpdate() == 1;
            }
        } catch (java.sql.SQLException exception) { throw JdbcSupport.failure("acquire idempotency", exception); }
    }

    @Override public void saveResult(IdempotencyKey key,IdempotencyResult<R> result) {
        String requestId=key.storageKey();
        if (result == null) throw new IllegalArgumentException("invalid idempotency result");
        String sql = "INSERT INTO aoo_business_idempotency(request_id,request_hash,user_id,operation,room_id,round_no,client_request_id,status,response_code,response_version,response_schema_version,response_payload,created_at,expires_at) VALUES(?,SHA2(?,256),?,?,?,?,?,'COMPLETED',?,?,?,?,?,?) ON DUPLICATE KEY UPDATE status='COMPLETED',response_code=VALUES(response_code),response_version=VALUES(response_version),response_schema_version=VALUES(response_schema_version),response_payload=VALUES(response_payload),expires_at=VALUES(expires_at)";
        Instant now = clock.instant();
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            String payload = mapper.writeValueAsString(result.data());
            statement.setString(1,requestId);statement.setString(2,requestId);statement.setString(3,key.userId());statement.setString(4,key.operation());statement.setLong(5,key.roomId());statement.setInt(6,key.roundNo());statement.setString(7,key.requestId());statement.setInt(8,result.code());statement.setString(9,result.resultVersion());statement.setInt(10,result.schemaVersion());statement.setString(11,payload);
            statement.setTimestamp(12,Timestamp.from(now));statement.setTimestamp(13,Timestamp.from(result.expiresAt()));
            statement.executeUpdate();
        } catch (Exception exception) {
            if (exception instanceof java.sql.SQLException sqlException) throw JdbcSupport.failure("save idempotency", sqlException);
            throw new IllegalStateException("cannot serialize idempotency result", exception);
        }
    }
    @Override public void save(IdempotencyKey key,R result,Duration retention){validate(key,retention);saveResult(key,IdempotencyResult.success(result,clock.instant().plus(retention)));}

    @Override public void release(IdempotencyKey key) {
        String sql = "DELETE FROM aoo_business_idempotency WHERE request_id=? AND status='PROCESSING'";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1,key.storageKey()); statement.executeUpdate();
        } catch (java.sql.SQLException exception) { throw JdbcSupport.failure("release idempotency", exception); }
    }
    @Override public void markUnknown(IdempotencyKey key){
        String sql="UPDATE aoo_business_idempotency SET status='UNKNOWN' WHERE request_id=? AND status='PROCESSING'";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){statement.setString(1,key.storageKey());statement.executeUpdate();}
        catch(java.sql.SQLException exception){throw JdbcSupport.failure("mark idempotency unknown",exception);}
    }

    private static void validate(IdempotencyKey key, Duration retention) {
        if (key == null || retention == null || retention.isNegative() || retention.isZero()) throw new IllegalArgumentException("invalid idempotency entry");
    }
}
