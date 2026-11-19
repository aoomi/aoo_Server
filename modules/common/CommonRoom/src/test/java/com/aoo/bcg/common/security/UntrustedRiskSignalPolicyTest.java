package com.aoo.bcg.common.security;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UntrustedRiskSignalPolicyTest {
    @Test void acceptsOnlyFreshBoundedSignalsAndStillReturnsRiskDataRatherThanAuthority() {
        Instant now = Instant.parse("2026-08-24T00:00:00Z");
        var policy = new UntrustedRiskSignalPolicy(Duration.ofMinutes(2), Duration.ofSeconds(5), 500);
        var clean = policy.evaluate(new UntrustedRiskSignalPolicy.Signals(30.5, 104.1, 10.0,
                now.minusSeconds(5), "device_attest_123456", false, false), now);
        assertTrue(clean.usable()); assertEquals(0, clean.riskScore());
        var suspicious = policy.evaluate(new UntrustedRiskSignalPolicy.Signals(91.0, 104.1, 900.0,
                now.plusSeconds(20), "bad", true, true), now);
        assertFalse(suspicious.usable()); assertTrue(suspicious.riskScore() >= 80);
        assertTrue(suspicious.reasons().contains("LOCATION_RANGE_INVALID"));
    }
}
