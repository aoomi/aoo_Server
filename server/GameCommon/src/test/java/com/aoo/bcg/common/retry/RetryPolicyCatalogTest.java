package com.aoo.bcg.common.retry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RetryPolicyCatalogTest {
    @Test void everyInfrastructureDomainHasBoundedBackoffJitterAndTerminalDisposition() {
        assertEquals(RetryPolicyCatalog.Domain.values().length, RetryPolicyCatalog.all().size());
        for (var domain : RetryPolicyCatalog.Domain.values()) {
            var entry = RetryPolicyCatalog.forDomain(domain);
            var policy = entry.policy();
            assertTrue(policy.maxAttempts() > 0 && policy.maxAttempts() <= 10);
            assertTrue(policy.initialBackoff().isPositive());
            assertTrue(policy.maximumBackoff().compareTo(policy.initialBackoff()) >= 0);
            assertTrue(policy.maximumElapsed().isPositive());
            assertTrue(policy.jitterRatio() > 0 && policy.jitterRatio() <= 1);
        }
        assertEquals(RetryPolicyCatalog.FailureDisposition.DEAD_LETTER,
                RetryPolicyCatalog.forDomain(RetryPolicyCatalog.Domain.MQ).exhaustedDisposition());
        assertEquals(RetryPolicyCatalog.FailureDisposition.FAILURE_LEDGER,
                RetryPolicyCatalog.forDomain(RetryPolicyCatalog.Domain.COMPENSATION).exhaustedDisposition());
    }
}
