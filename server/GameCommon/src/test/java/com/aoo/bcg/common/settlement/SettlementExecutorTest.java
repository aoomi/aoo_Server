package com.aoo.bcg.common.settlement;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class SettlementExecutorTest {
    @Test void validatesAndUsesStableBusinessIdentity() {
        class MemoryRepository implements SettlementRepository {
            final Map<String, SettlementResult> values = new HashMap<>();
            public SettlementResult save(String id, SettlementResult value) {
                SettlementResult existing = values.putIfAbsent(id, value);
                if (existing != null && !existing.equals(value)) throw new IllegalArgumentException("changed result");
                return existing == null ? value : existing;
            }
            public Optional<SettlementResult> find(String id) { return Optional.ofNullable(values.get(id)); }
        }
        var repository = new MemoryRepository();
        var executor = new SettlementExecutor(repository);
        var result = new SettlementResult(9, 2, "v1", List.of(
                new SettlementEntry(1, 8, Map.of("base", 8L)),
                new SettlementEntry(2, -8, Map.of("base", -8L))));
        assertEquals(result, executor.execute(SettlementScope.ROUND, SettlementBalancePolicy.ZERO_SUM,
                9, 2, "v1", result));
        assertEquals(1, repository.values.size());
        assertEquals(result, executor.execute(SettlementScope.ROUND, SettlementBalancePolicy.ZERO_SUM,
                9, 2, "v1", result));
        assertThrows(IllegalStateException.class, () -> executor.execute(SettlementScope.ROUND,
                SettlementBalancePolicy.ZERO_SUM, 9, 3, "v1", result));
    }
}
