package com.aoo.bcg.common.session;

import java.time.Duration;
import java.util.Optional;

public interface SessionStore {
    void save(SessionRecord session, Duration retention);
    Optional<SessionRecord> find(String sessionId);
    Optional<SessionRecord> activeForUser(long userId);
    boolean replaceIfVersion(long userId, long expectedVersion, SessionRecord replacement, Duration retention);
    void revoke(String sessionId, String reason);
    void revokeAll(long userId, String reason);
}
