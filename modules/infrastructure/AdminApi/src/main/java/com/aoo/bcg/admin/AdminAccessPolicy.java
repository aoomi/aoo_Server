package com.aoo.bcg.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Unifies permission, data-scope and four-eyes authorization for high-risk commands. */
public final class AdminAccessPolicy {
    public enum Risk { NORMAL, HIGH }
    public record Grant(long operatorId, String permission, String scopeType, String scopeId) {
        public Grant {
            if (operatorId <= 0 || blank(permission) || blank(scopeType) || blank(scopeId))
                throw new IllegalArgumentException("complete scoped grant required");
        }
        boolean covers(String requiredPermission, String targetType, String targetId) {
            return permission.equals(requiredPermission)
                    && (scopeType.equals("GLOBAL") || (scopeType.equals(targetType)
                    && (scopeId.equals("*") || scopeId.equals(targetId))));
        }
    }
    public record Approval(String approvalId, long requesterId, long approverId,
            String permission, String targetType, String targetId, Instant expiresAt) {
        public Approval {
            if (blank(approvalId) || requesterId <= 0 || approverId <= 0 || requesterId == approverId
                    || blank(permission) || blank(targetType) || blank(targetId) || expiresAt == null)
                throw new IllegalArgumentException("valid four-eyes approval required");
        }
    }
    public record Decision(boolean allowed, String reason) { }

    public interface GrantStore { List<Grant> grants(long operatorId); }
    public interface ApprovalStore {
        Optional<Approval> find(String approvalId);
        boolean consume(String approvalId);
    }

    private final AdminAuthorizationService permissionService;
    private final GrantStore grants;
    private final ApprovalStore approvals;
    private final Clock clock;

    public AdminAccessPolicy(AdminAuthorizationService permissionService, GrantStore grants,
            ApprovalStore approvals, Clock clock) {
        this.permissionService = Objects.requireNonNull(permissionService);
        this.grants = Objects.requireNonNull(grants);
        this.approvals = Objects.requireNonNull(approvals);
        this.clock = Objects.requireNonNull(clock);
    }

    public Decision authorize(long operatorId, String permission, String targetType,
            String targetId, Risk risk, String approvalId) {
        if (!permissionService.allowed(operatorId, permission)) return new Decision(false, "permission_denied");
        if (grants.grants(operatorId).stream().noneMatch(grant -> grant.covers(permission, targetType, targetId))) {
            return new Decision(false, "data_scope_denied");
        }
        if (risk == Risk.NORMAL) return new Decision(true, "allowed");
        Optional<Approval> found = approvals.find(approvalId);
        if (found.isEmpty()) return new Decision(false, "secondary_approval_required");
        Approval approval = found.get();
        if (approval.requesterId() != operatorId || !approval.permission().equals(permission)
                || !approval.targetType().equals(targetType) || !approval.targetId().equals(targetId)
                || !approval.expiresAt().isAfter(clock.instant())) {
            return new Decision(false, "secondary_approval_mismatch");
        }
        return approvals.consume(approval.approvalId())
                ? new Decision(true, "allowed") : new Decision(false, "secondary_approval_already_used");
    }

    public static final class InMemoryStore implements GrantStore, ApprovalStore {
        private final Map<Long, List<Grant>> grants = new ConcurrentHashMap<>();
        private final Map<String, Approval> approvals = new ConcurrentHashMap<>();
        private final Map<String, Boolean> consumed = new ConcurrentHashMap<>();

        public void grant(Grant grant) {
            grants.compute(grant.operatorId(), (ignored, current) -> {
                List<Grant> next = new ArrayList<>(current == null ? List.of() : current);
                next.add(grant);
                return List.copyOf(next);
            });
        }
        public void approve(Approval approval) { approvals.put(approval.approvalId(), approval); }
        @Override public List<Grant> grants(long operatorId) { return grants.getOrDefault(operatorId, List.of()); }
        @Override public Optional<Approval> find(String approvalId) {
            return approvalId == null || consumed.containsKey(approvalId)
                    ? Optional.empty() : Optional.ofNullable(approvals.get(approvalId));
        }
        @Override public boolean consume(String approvalId) { return consumed.putIfAbsent(approvalId, true) == null; }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
