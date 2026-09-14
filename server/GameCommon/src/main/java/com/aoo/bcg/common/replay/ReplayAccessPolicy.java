package com.aoo.bcg.common.replay;

public interface ReplayAccessPolicy<S, V> {
    V playerView(S authoritativeReplay, long authenticatedViewerId);
    V spectatorView(S authoritativeReplay);
    V privilegedView(S authoritativeReplay, long operatorId, String auditReason);
}
