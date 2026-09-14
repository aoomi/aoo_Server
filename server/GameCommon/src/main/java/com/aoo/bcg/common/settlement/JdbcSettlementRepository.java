package com.aoo.bcg.common.settlement;

import com.aoo.bcg.common.persistence.JdbcSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import javax.sql.DataSource;

/** Immutable, idempotent persistence for round and final settlement results. */
public final class JdbcSettlementRepository implements SettlementRepository {
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final LongSupplier ids;
    private final Clock clock;

    public JdbcSettlementRepository(DataSource dataSource, ObjectMapper mapper, LongSupplier ids, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public SettlementResult save(String businessId, SettlementResult result) {
        validate(businessId, result);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                SettlementResult existing = find(connection, businessId);
                if (existing != null) {
                    requireSame(existing, result);
                    connection.rollback();
                    return existing;
                }
                long settlementId = com.aoo.bcg.common.math.ExactDomainMath.requirePositiveId(
                        ids.getAsLong(), "settlement id");
                Timestamp createdAt = Timestamp.from(clock.instant());
                try (var statement = connection.prepareStatement(
                        "INSERT INTO aoo_settlement(settlement_id,business_id,room_id,round_no,settlement_version,result_payload,created_at) VALUES(?,?,?,?,?,?,?)")) {
                    statement.setLong(1, settlementId);
                    statement.setString(2, businessId);
                    statement.setLong(3, result.roomId());
                    statement.setInt(4, result.roundNo());
                    statement.setString(5, result.playVersion());
                    statement.setString(6, mapper.writeValueAsString(result));
                    statement.setTimestamp(7, createdAt);
                    statement.executeUpdate();
                }
                saveScores(connection, settlementId, businessId, result, createdAt);
                connection.commit();
                return result;
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException argument) throw argument;
            throw new IllegalStateException("cannot persist settlement " + businessId, error);
        }
    }

    private void saveScores(java.sql.Connection connection, long settlementId, String businessId,
                            SettlementResult result, Timestamp createdAt) throws Exception {
        String sql = "INSERT INTO aoo_settlement_score(settlement_id,player_id,score_scale,score_delta,score_after,multiplier,score_reason,calculation_hash,created_at) VALUES(?,?,0,?,NULL,1,?,?,?)";
        try (var statement = connection.prepareStatement(sql)) {
            for (SettlementEntry entry : result.entries()) {
                statement.setLong(1, settlementId);
                statement.setLong(2, entry.playerId());
                statement.setBigDecimal(3, BigDecimal.valueOf(entry.scoreDelta()));
                statement.setString(4, businessId.startsWith("final-room:") ? "FINAL_ROOM" : "ROUND");
                statement.setString(5, calculationHash(businessId, entry));
                statement.setTimestamp(6, createdAt);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private String calculationHash(String businessId, SettlementEntry entry) throws Exception {
        String canonical = businessId + '|' + entry.playerId() + '|' + entry.scoreDelta() + '|'
                + mapper.writeValueAsString(new TreeMap<>(entry.components()));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public Optional<SettlementResult> find(String businessId) {
        validateBusinessId(businessId);
        try (var connection = dataSource.getConnection()) {
            return Optional.ofNullable(find(connection, businessId));
        } catch (Exception error) {
            throw new IllegalStateException("cannot read settlement " + businessId, error);
        }
    }

    private SettlementResult find(java.sql.Connection connection, String businessId) throws Exception {
        try (var statement = connection.prepareStatement(
                "SELECT result_payload FROM aoo_settlement WHERE business_id=? FOR UPDATE")) {
            statement.setString(1, businessId);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? mapper.readValue(rows.getString(1), SettlementResult.class) : null;
            }
        }
    }

    private static void requireSame(SettlementResult existing, SettlementResult requested) {
        if (!existing.equals(requested)) throw new IllegalArgumentException(
                "businessId reused with a different settlement result");
    }

    private static void validate(String businessId, SettlementResult result) {
        validateBusinessId(businessId);
        Objects.requireNonNull(result, "result");
        if (result.roomId() <= 0 || result.roundNo() < 0 || result.playVersion() == null
                || result.playVersion().isBlank() || result.playVersion().length() > 64)
            throw new IllegalArgumentException("invalid settlement identity");
    }

    private static void validateBusinessId(String businessId) {
        if (businessId == null || businessId.isBlank() || businessId.length() > 128)
            throw new IllegalArgumentException("invalid settlement businessId");
    }
}
