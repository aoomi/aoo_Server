package com.aoo.bcg.hall.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HallErrorRegistryContractTest {
    @Test void preservesWireCodeWhileExposingCanonicalCategory() {
        HallError pricing = HallError.conflict("HALL_COST_POLICY_MISSING", "policy missing");
        assertEquals("HALL_COST_POLICY_MISSING", pricing.code());
        assertEquals("PRICING_POLICY_MISSING", pricing.canonicalCode());

        HallError identity = HallError.notFound("identity missing");
        assertEquals("HALL_NOT_FOUND", identity.code());
        assertEquals("IDENTITY_NOT_FOUND", identity.canonicalCode());
    }

    @Test void unknownCompatibilityCodeRemainsVisibleAndIsNotRewritten() {
        HallError unknown = new HallError(409, "HALL_NEW_UNREGISTERED_CODE", "unknown");
        assertEquals("HALL_NEW_UNREGISTERED_CODE", unknown.canonicalCode());
    }
}
