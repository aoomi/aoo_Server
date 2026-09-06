package com.aoo.bcg.common.dissolve;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class MajorityDissolvePolicy implements DissolvePolicy {
    private final boolean approveOnTimeout;
    public MajorityDissolvePolicy(boolean approveOnTimeout) { this.approveOnTimeout = approveOnTimeout; }
    @Override public DissolveDecision decide(long applicantId, Set<Long> eligiblePlayerIds,
                                              Map<Long, Boolean> votes, Instant deadline, Instant now) {
        if (!now.isBefore(deadline)) return approveOnTimeout ? DissolveDecision.EXPIRED_APPROVED : DissolveDecision.EXPIRED_REJECTED;
        long rejections = votes.values().stream().filter(Boolean.FALSE::equals).count();
        if (rejections > 0) return DissolveDecision.REJECTED;
        long approvals = votes.values().stream().filter(Boolean.TRUE::equals).count();
        if (approvals == eligiblePlayerIds.size()) return DissolveDecision.APPROVED;
        return DissolveDecision.WAITING;
    }
}
