package com.aoo.bcg.admin;

import java.time.Instant;

/** Shared one-shot nonce boundary. Implementations must make consume atomic across instances. */
public interface AdminNonceStore {
    boolean consume(String namespace, String nonce, Instant expiresAt);
}
