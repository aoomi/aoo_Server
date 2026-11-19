package com.aoo.bcg.hall.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A missing active cost policy is deterministic; unrelated failures remain generic. */
final class HallCostPolicyErrorMappingTest {
    @Test
    void exposesMachineReadableCostPolicyMissingError() {
        HallError failure = HallError.conflict(
                "HALL_COST_POLICY_MISSING", "active room cost policy is missing for ROUND_COUNT=10");

        assertEquals(409, failure.status());
        assertEquals("HALL_COST_POLICY_MISSING", failure.code());
        assertTrue(failure.getMessage().contains("ROUND_COUNT=10"));
    }

    @Test
    void routesDeterministicHallErrorsBeforeUnknownErrorFallback() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/http/HallHttpRoutes.java"));

        assertTrue(source.contains("catch(HallError x)"));
        assertTrue(source.contains("HALL_INTERNAL_ERROR"));
        assertEquals(2, source.split("catch\\(HallError x\\)", -1).length - 1);
    }
}
