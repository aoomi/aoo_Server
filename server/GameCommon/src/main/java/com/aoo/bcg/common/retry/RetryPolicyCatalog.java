package com.aoo.bcg.common.retry;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/** Single bounded retry policy source for every infrastructure failure domain. */
public final class RetryPolicyCatalog {
    public enum Domain { HTTP, DATABASE, MQ, CONFIG, DOWNLOAD, COMPENSATION }

    public record Entry(ControlledRetry.Policy policy, FailureDisposition exhaustedDisposition) {
        public Entry {
            if (policy == null || exhaustedDisposition == null)
                throw new IllegalArgumentException("retry policy and exhausted disposition are required");
        }
    }

    public enum FailureDisposition { RETURN_ERROR, DEAD_LETTER, FAILURE_LEDGER }

    private static final Map<Domain, Entry> ENTRIES = entries();

    private RetryPolicyCatalog() {}

    public static Entry forDomain(Domain domain) {
        Entry entry = ENTRIES.get(domain);
        if (entry == null) throw new IllegalArgumentException("unsupported retry domain: " + domain);
        return entry;
    }

    public static Map<Domain, Entry> all() { return ENTRIES; }

    private static Map<Domain, Entry> entries() {
        EnumMap<Domain, Entry> entries = new EnumMap<>(Domain.class);
        entries.put(Domain.HTTP, entry(3, 100, 2_000, 8_000, FailureDisposition.RETURN_ERROR));
        entries.put(Domain.DATABASE, entry(3, 50, 1_000, 5_000, FailureDisposition.RETURN_ERROR));
        entries.put(Domain.MQ, entry(10, 250, 30_000, 300_000, FailureDisposition.DEAD_LETTER));
        entries.put(Domain.CONFIG, entry(3, 250, 3_000, 10_000, FailureDisposition.RETURN_ERROR));
        entries.put(Domain.DOWNLOAD, entry(4, 500, 8_000, 30_000, FailureDisposition.RETURN_ERROR));
        entries.put(Domain.COMPENSATION, entry(8, 1_000, 60_000, 600_000, FailureDisposition.FAILURE_LEDGER));
        if (entries.size() != Domain.values().length) throw new IllegalStateException("retry domain policy missing");
        return Map.copyOf(entries);
    }

    private static Entry entry(int attempts, long initialMs, long maximumMs, long elapsedMs,
            FailureDisposition disposition) {
        return new Entry(new ControlledRetry.Policy(attempts, Duration.ofMillis(initialMs),
                Duration.ofMillis(maximumMs), Duration.ofMillis(elapsedMs), 0.20), disposition);
    }
}
