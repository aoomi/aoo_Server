package com.aoo.bcg.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public final class AuditedCommandService {
    private final PermissionService permissions;
    private final AuditRepository audits;
    private final Clock clock;

    public AuditedCommandService(PermissionService permissions, AuditRepository audits, Clock clock) {
        this.permissions = Objects.requireNonNull(permissions); this.audits = Objects.requireNonNull(audits); this.clock = Objects.requireNonNull(clock);
    }

    public <T> T execute(long operatorId, String permission, PermissionService.Resource resource,
                         String requestId, String reason, Supplier<T> command) {
        if (requestId == null || requestId.isBlank() || reason == null || reason.isBlank())
            throw new IllegalArgumentException("requestId and reason are required");
        permissions.authorize(operatorId, permission, resource).requireAllowed();
        AuditRecord previous = audits.findByRequestId(requestId).orElse(null);
        if (previous != null) throw new IllegalStateException("admin command already executed");
        Instant startedAt = clock.instant();
        try {
            T result = command.get();
            audits.append(new AuditRecord(requestId, operatorId, permission, resource, reason, "SUCCESS", startedAt, clock.instant()));
            return result;
        } catch (RuntimeException exception) {
            audits.append(new AuditRecord(requestId, operatorId, permission, resource, reason, "FAILED", startedAt, clock.instant()));
            throw exception;
        }
    }

    public interface AuditRepository {
        java.util.Optional<AuditRecord> findByRequestId(String requestId);
        void append(AuditRecord record);
    }
    public record AuditRecord(String requestId, long operatorId, String permission,
                              PermissionService.Resource resource, String reason, String status,
                              Instant startedAt, Instant completedAt) {}
}
