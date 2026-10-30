package com.ddm.server.protocol.v2;

import java.time.Duration;
import java.util.Optional;

/** Production implementation must use Redis SET-NX plus a bounded response cache. */
public interface ProtocolIdempotencyStore {
    boolean acquire(long userId, String requestId, Duration ttl);
    Optional<ProtocolEnvelope> previous(long userId, String requestId);
    void complete(long userId, String requestId, ProtocolEnvelope response, Duration ttl);
}
