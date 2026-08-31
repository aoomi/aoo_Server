package com.aoo.bcg.common.settlement;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SettlementValidatorTest {
    @Test void acceptsBalancedUniqueSettlement() {
        SettlementResult result = new SettlementResult(1, 2, "v1", List.of(
                new SettlementEntry(10, 5, Map.of("base", 5L)),
                new SettlementEntry(11, -5, Map.of("base", -5L))));
        assertDoesNotThrow(() -> SettlementValidator.validate(result, 1, 2, "v1", true));
    }
    @Test void rejectsNonZeroSumAndComponentMismatch() {
        SettlementResult unbalanced = new SettlementResult(1, 2, "v1", List.of(new SettlementEntry(10, 5, Map.of("base", 5L))));
        assertThrows(IllegalStateException.class, () -> SettlementValidator.validate(unbalanced, 1, 2, "v1", true));
        SettlementResult mismatch = new SettlementResult(1, 2, "v1", List.of(new SettlementEntry(10, 5, Map.of("base", 4L))));
        assertThrows(IllegalStateException.class, () -> SettlementValidator.validate(mismatch, 1, 2, "v1", false));
    }
}
