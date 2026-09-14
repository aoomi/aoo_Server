package com.aoo.bcg.config;

import java.time.Instant;
import java.util.Objects;

public record ProfileAuditEntry(long gameId, String version, Action action,
                                long operatorId, String reason, Instant occurredAt) {
    public enum Action { INSERT_VERSION, ACTIVATE_VERSION }

    public ProfileAuditEntry {
        if (gameId <= 0 || operatorId <= 0) throw new IllegalArgumentException("invalid audit identity");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
