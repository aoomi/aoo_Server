package com.aoo.bcg.billing;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AOO_DB_IT_URL", matches = ".+")
class JdbcBillingIntegrationTest {
    @Test void debitAndRefundAreTransactionalAndIdempotentUnderConcurrency() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),
                System.getenv("AOO_DB_IT_USER"), System.getenv("AOO_DB_IT_PASSWORD"));
        long playerId = 8_000_000_000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000L);
        String prefix = "room:" + playerId + ":player:" + playerId;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "INSERT INTO aoo_currency_balance(player_id,currency,balance,version,updated_at) VALUES(?,'ROOM_CARD',100,0,CURRENT_TIMESTAMP(3))")) {
            statement.setLong(1, playerId);
            statement.executeUpdate();
        }
        var ids = new AtomicLong(System.currentTimeMillis() * 1000);
        var billing = new JdbcBillingService(dataSource, ids::incrementAndGet, Clock.systemUTC());
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 16)
                    .<java.util.concurrent.Callable<LedgerEntry>>mapToObj(ignored -> () ->
                            billing.debit(prefix + ":consume", playerId, "ROOM_CARD", 30, "CREATE_ROOM"))
                    .toList();
            for (var result : executor.invokeAll(tasks)) assertEquals(70, result.get().balanceAfter());
        }
        assertEquals(100, billing.credit(prefix + ":refund", playerId, "ROOM_CARD", 30, "ROOM_CANCELLED").balanceAfter());
        assertEquals(100, billing.credit(prefix + ":refund", playerId, "ROOM_CARD", 30, "ROOM_CANCELLED").balanceAfter());
    }
}
