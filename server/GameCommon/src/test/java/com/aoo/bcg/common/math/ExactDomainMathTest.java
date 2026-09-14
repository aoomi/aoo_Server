package com.aoo.bcg.common.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExactDomainMathTest {
    @Test void rejectsCounterAndScoreOverflow() {
        assertEquals(2, ExactDomainMath.increment(1, "round"));
        assertThrows(ArithmeticException.class, () -> ExactDomainMath.increment(Integer.MAX_VALUE, "round"));
        assertThrows(ArithmeticException.class, () -> ExactDomainMath.increment(Long.MAX_VALUE, "revision"));
        assertThrows(ArithmeticException.class, () -> ExactDomainMath.add(Long.MAX_VALUE, 1, "score"));
    }
    @Test void rejectsInvalidGeneratedIds() {
        assertEquals(7, ExactDomainMath.requirePositiveId(7, "ledger"));
        assertThrows(IllegalStateException.class, () -> ExactDomainMath.requirePositiveId(0, "ledger"));
    }
}
