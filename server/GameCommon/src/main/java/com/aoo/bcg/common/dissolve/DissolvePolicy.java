package com.aoo.bcg.common.dissolve;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface DissolvePolicy {
    DissolveDecision decide(long applicantId, Set<Long> eligiblePlayerIds,
                             Map<Long, Boolean> votes, Instant deadline, Instant now);
}
