package com.aoo.bcg.common.settlement;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AOO_DB_IT_URL", matches = ".+")
class JdbcSettlementRepositoryIntegrationTest {
    @Test void settlementIsImmutableAndIdempotent() {
        var dataSource = new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),
                System.getenv("AOO_DB_IT_USER"), System.getenv("AOO_DB_IT_PASSWORD"));
        var ids = new AtomicLong(System.currentTimeMillis() * 1000);
        var repository = new JdbcSettlementRepository(dataSource, new ObjectMapper(), ids::incrementAndGet,
                Clock.systemUTC());
        long roomId = System.currentTimeMillis();
        String businessId = SettlementBusinessId.round(roomId, 1, "test-v1");
        var result = new SettlementResult(roomId, 1, "test-v1", List.of(
                new SettlementEntry(1, 10, Map.of("base", 10L)),
                new SettlementEntry(2, -10, Map.of("base", -10L))));
        SettlementValidator.validate(result, roomId, 1, "test-v1", true);
        assertEquals(result, repository.save(businessId, result));
        assertEquals(result, repository.save(businessId, result));
        assertEquals(result, repository.find(businessId).orElseThrow());
        assertEquals(2, scoreRowCount(dataSource, businessId));
        var changed = new SettlementResult(roomId, 1, "test-v1", List.of(
                new SettlementEntry(1, 20, Map.of("base", 20L)),
                new SettlementEntry(2, -20, Map.of("base", -20L))));
        assertThrows(IllegalArgumentException.class, () -> repository.save(businessId, changed));
    }

    private static long scoreRowCount(DriverManagerDataSource dataSource, String businessId) {
        String sql = "SELECT COUNT(*) FROM aoo_settlement_score score "
                + "JOIN aoo_settlement settlement ON settlement.settlement_id=score.settlement_id "
                + "WHERE settlement.business_id=?";
        try (var connection = dataSource.getConnection(); var query = connection.prepareStatement(sql)) {
            query.setString(1, businessId);
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                return rows.getLong(1);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("cannot verify settlement score rows", failure);
        }
    }
}
