package com.aoo.bcg.common.lifecycle;

import java.time.Duration;

public record DataLifecyclePolicy(String dataType, Duration hotRetention, Duration archiveRetention,
                                  boolean legalHoldSupported, boolean anonymizeOnAccountDeletion) {
    public DataLifecyclePolicy {
        if (dataType == null || dataType.isBlank() || hotRetention == null || archiveRetention == null)
            throw new IllegalArgumentException("invalid lifecycle policy");
        if (hotRetention.isNegative() || archiveRetention.compareTo(hotRetention) < 0)
            throw new IllegalArgumentException("archive retention must cover hot retention");
    }
}
