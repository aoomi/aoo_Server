package com.aoo.bcg.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Scheduled two-phase multi-node release with fail-stop rollback and complete audit evidence. */
public final class ConfigurationPublicationCoordinator {
    public enum PlanState { SCHEDULED, PREWARMED, ACTIVE, FAILED, FAILED_ROLLED_BACK }
    public enum CacheLayer { RULES, REGIONS, COMPONENTS, CLIENT_CATALOG, LOCAL_INDEX }
    public enum AuditAction { SCHEDULED, PREWARMED, ACTIVATED, PREPARE_FAILED, FAILED_ROLLED_BACK, ROLLED_BACK }

    public interface PublicationNode {
        String nodeId();
        void prepare(ConfigurationRelease release);
        void activate(ConfigurationRelease release);
        void restore(ConfigurationRelease release);
        void abort(String releaseId);
        void invalidateCaches(Set<CacheLayer> layers);
        boolean supports(ConfigurationRelease release);
        String activeReleaseId();
    }

    public record ReleaseRequest(ConfigurationRelease release,
                                 GameConfigurationDraftCompiler.ValidatedDraft content,
                                 SignedReleaseApproval approval,
                                 String expectedActiveReleaseId,
                                 long operatorId,
                                 String reason,
                                 Instant activateAt,
                                 PlayAvailabilityPolicy availability,
                                 String impactScope) {
        public ReleaseRequest {
            if (release == null || content == null || approval == null || expectedActiveReleaseId == null
                    || expectedActiveReleaseId.isBlank() || operatorId <= 0 || reason == null || reason.isBlank()
                    || activateAt == null || availability == null
                    || impactScope == null || impactScope.isBlank())
                throw new IllegalArgumentException("incomplete release request");
        }
    }

    public record PublicationAuditEntry(String auditId, AuditAction action, String releaseId,
                                        String targetReleaseId, long operatorId, String reason,
                                        Instant occurredAt, long approverId, Instant approvedAt,
                                        String contentHash, String rolloutScope, String impactScope,
                                        Map<String, String> domainDifference,
                                        Map<String, String> nodeResults) { }

    public static final class ReleasePlan {
        private final ReleaseRequest request;
        private PlanState state = PlanState.SCHEDULED;
        private final LinkedHashMap<String, String> nodeResults = new LinkedHashMap<>();
        private ReleasePlan(ReleaseRequest request) { this.request = request; }
        public ReleaseRequest request() { return request; }
        public PlanState state() { return state; }
        public Map<String, String> nodeResults() { return Map.copyOf(nodeResults); }
    }

    private static final Set<String> COMPLETE_DOMAINS = Set.of(
            "rules", "components", "protocol", "ui", "switches", "regions");
    private static final Set<CacheLayer> ALL_CACHES = Set.copyOf(EnumSet.allOf(CacheLayer.class));

    private final AtomicConfigurationReleaseStore releases;
    private final List<PublicationNode> nodes;
    private final Clock clock;
    private final Duration prewarmWindow;
    private final byte[] approvalKey;
    private final Map<String, ReleasePlan> plans = new LinkedHashMap<>();
    private final List<PublicationAuditEntry> audits = new ArrayList<>();

    public ConfigurationPublicationCoordinator(AtomicConfigurationReleaseStore releases,
                                               List<PublicationNode> nodes,
                                               Clock clock,
                                               Duration prewarmWindow,
                                               byte[] approvalKey) {
        this.releases = Objects.requireNonNull(releases);
        this.nodes = List.copyOf(nodes);
        this.clock = Objects.requireNonNull(clock);
        if (nodes.isEmpty() || nodes.stream().map(PublicationNode::nodeId).distinct().count() != nodes.size())
            throw new IllegalArgumentException("unique publication nodes are required");
        if (prewarmWindow == null || prewarmWindow.isNegative()) throw new IllegalArgumentException("invalid prewarm window");
        this.prewarmWindow = prewarmWindow;
        if (approvalKey == null || approvalKey.length < 32) throw new IllegalArgumentException("approval key is required");
        this.approvalKey = approvalKey.clone();
    }

    public synchronized ReleasePlan schedule(ReleaseRequest request) {
        request.approval().verifyPublication(request.content(), request.availability(), request.activateAt(), approvalKey);
        requireCompleteAndCompatible(request.release(), request.content());
        request.availability().verify(request.content().source().switches(), request.content().regionalRules());
        if (request.activateAt().isBefore(clock.instant())) throw new IllegalArgumentException("activation time is in the past");
        String current = releases.active().map(ConfigurationRelease::releaseId).orElse("");
        if (!current.equals(request.expectedActiveReleaseId())) throw new IllegalStateException("active release changed before scheduling");
        if (plans.putIfAbsent(request.release().releaseId(), new ReleasePlan(request)) != null)
            throw new IllegalStateException("release plan already exists");
        ReleasePlan plan = plans.get(request.release().releaseId());
        audit(plan, AuditAction.SCHEDULED, "", Map.of());
        return plan;
    }

    /** Uses the injected UTC clock for both prewarm and the activation linearization point. */
    public synchronized void tick() {
        plans.values().stream().sorted(Comparator.comparing(plan -> plan.request.activateAt())).forEach(plan -> {
            if (plan.state == PlanState.SCHEDULED
                    && !clock.instant().isBefore(plan.request.activateAt().minus(prewarmWindow))) prewarm(plan);
            if (plan.state == PlanState.PREWARMED && !clock.instant().isBefore(plan.request.activateAt())) activate(plan);
        });
    }

    public synchronized ConfigurationRelease rollback(String targetReleaseId, String expectedActiveReleaseId,
                                                        long operatorId, String reason, String impactScope) {
        if (operatorId <= 0 || reason == null || reason.isBlank() || impactScope == null || impactScope.isBlank())
            throw new IllegalArgumentException("rollback audit fields are required");
        ConfigurationRelease target = releases.find(targetReleaseId)
                .orElseThrow(() -> new IllegalArgumentException("rollback snapshot not found"));
        requireComplete(target);
        List<String> unsupported = nodes.stream().filter(node -> !node.supports(target)).map(PublicationNode::nodeId).toList();
        if (!unsupported.isEmpty()) throw new IllegalStateException("rollback component dependencies unavailable on nodes: " + unsupported);
        ConfigurationRelease previous = releases.active().orElseThrow(() -> new IllegalStateException("no active release"));
        LinkedHashMap<String, String> results = new LinkedHashMap<>();
        for (PublicationNode node : nodes) {
            try { node.prepare(target); results.put(node.nodeId(), "ROLLBACK_PREPARED"); }
            catch (RuntimeException failure) {
                results.put(node.nodeId(), "ROLLBACK_PREPARE_FAILED:" + failure.getClass().getSimpleName());
                for (PublicationNode prepared : nodes) {
                    try { prepared.abort(target.releaseId()); } catch (RuntimeException ignored) { }
                    try { prepared.invalidateCaches(ALL_CACHES); } catch (RuntimeException ignored) { }
                }
                throw new IllegalStateException("rollback preparation failed; active index was not changed", failure);
            }
        }
        ConfigurationRelease activated = releases.rollback(targetReleaseId, expectedActiveReleaseId);
        boolean restoreFailed = false;
        for (PublicationNode node : nodes) {
            try { node.restore(target); node.invalidateCaches(ALL_CACHES); results.put(node.nodeId(), "ROLLED_BACK"); }
            catch (RuntimeException failure) {
                restoreFailed = true;
                results.put(node.nodeId(), "ROLLBACK_FAILED:" + failure.getClass().getSimpleName());
            }
        }
        audits.add(new PublicationAuditEntry(UUID.randomUUID().toString(), AuditAction.ROLLED_BACK,
                previous.releaseId(), targetReleaseId, operatorId, reason, clock.instant(), 0, Instant.EPOCH, "",
                "rollback", impactScope, difference(previous, target), Map.copyOf(results)));
        if (restoreFailed) throw new IllegalStateException("rollback index changed but one or more nodes require recovery");
        return activated;
    }

    public synchronized List<PublicationAuditEntry> audits() { return List.copyOf(audits); }
    public synchronized ReleasePlan requirePlan(String releaseId) {
        ReleasePlan plan = plans.get(releaseId);
        if (plan == null) throw new IllegalArgumentException("release plan not found");
        return plan;
    }

    private void prewarm(ReleasePlan plan) {
        for (PublicationNode node : nodes) {
            try { node.prepare(plan.request.release()); plan.nodeResults.put(node.nodeId(), "PREPARED"); }
            catch (RuntimeException failure) {
                plan.nodeResults.put(node.nodeId(), "PREPARE_FAILED:" + failure.getClass().getSimpleName());
                abortAndInvalidate(plan);
                plan.state = PlanState.FAILED;
                audit(plan, AuditAction.PREPARE_FAILED, "", plan.nodeResults);
                return;
            }
        }
        plan.state = PlanState.PREWARMED;
        audit(plan, AuditAction.PREWARMED, "", plan.nodeResults);
    }

    private void activate(ReleasePlan plan) {
        plan.request.approval().verifyPublication(plan.request.content(), plan.request.availability(),
                plan.request.activateAt(), approvalKey);
        ConfigurationRelease previous = releases.active().orElseThrow(() -> new IllegalStateException("no active release"));
        try {
            releases.publish(plan.request.release(), plan.request.expectedActiveReleaseId());
            for (PublicationNode node : nodes) {
                node.activate(plan.request.release());
                plan.nodeResults.put(node.nodeId(), "ACK:" + node.activeReleaseId());
                if (!plan.request.release().releaseId().equals(node.activeReleaseId()))
                    throw new IllegalStateException("node acknowledged a different release");
            }
            plan.state = PlanState.ACTIVE;
            audit(plan, AuditAction.ACTIVATED, "", plan.nodeResults);
        } catch (RuntimeException failure) {
            plan.nodeResults.put("coordinator", "ACTIVATE_FAILED:" + failure.getClass().getSimpleName());
            if (releases.active().map(ConfigurationRelease::releaseId).orElse("").equals(plan.request.release().releaseId()))
                releases.rollback(previous.releaseId(), plan.request.release().releaseId());
            for (PublicationNode node : nodes) {
                try { node.restore(previous); plan.nodeResults.put(node.nodeId(), "RESTORED:" + previous.releaseId()); }
                catch (RuntimeException restoreFailure) {
                    plan.nodeResults.put(node.nodeId(), "RESTORE_FAILED:" + restoreFailure.getClass().getSimpleName());
                }
                try { node.invalidateCaches(ALL_CACHES); }
                catch (RuntimeException cacheFailure) {
                    plan.nodeResults.put(node.nodeId() + ":cache", "INVALIDATE_FAILED:" + cacheFailure.getClass().getSimpleName());
                }
            }
            plan.state = PlanState.FAILED_ROLLED_BACK;
            audit(plan, AuditAction.FAILED_ROLLED_BACK, previous.releaseId(), plan.nodeResults);
        }
    }

    private void abortAndInvalidate(ReleasePlan plan) {
        for (PublicationNode node : nodes) {
            try { node.abort(plan.request.release().releaseId()); } catch (RuntimeException ignored) { }
            try { node.invalidateCaches(ALL_CACHES); } catch (RuntimeException ignored) { }
        }
    }

    private void requireCompleteAndCompatible(ConfigurationRelease release,
                                              GameConfigurationDraftCompiler.ValidatedDraft content) {
        requireComplete(release);
        GameConfigurationDraft draft = content.source();
        Map<String, String> versions = release.domainVersions();
        if (!versions.get("rules").equals(draft.playVersion())
                || !versions.get("components").equals(draft.manifest().componentVersion())
                || !versions.get("protocol").equals(draft.manifest().protocolVersion())
                || !versions.get("ui").equals(draft.manifest().clientBundleVersion())
                || !versions.get("switches").equals(draft.switches().rolloutKey())
                || !versions.get("regions").equals(draft.schemaVersion()))
            throw new IllegalArgumentException("release domains are not jointly compatible with approved content");
    }

    private void requireComplete(ConfigurationRelease release) {
        if (!release.domainVersions().keySet().containsAll(COMPLETE_DOMAINS))
            throw new IllegalArgumentException("release is not a complete rollback snapshot: " + COMPLETE_DOMAINS);
    }

    private void audit(ReleasePlan plan, AuditAction action, String target, Map<String, String> results) {
        ConfigurationRelease previous = releases.find(plan.request.expectedActiveReleaseId()).orElse(null);
        audits.add(new PublicationAuditEntry(UUID.randomUUID().toString(), action, plan.request.release().releaseId(),
                target, plan.request.operatorId(), plan.request.reason(), clock.instant(),
                plan.request.approval().approverId(), plan.request.approval().approvedAt(),
                plan.request.approval().contentHash(), plan.request.availability().auditScope(), plan.request.impactScope(),
                difference(previous, plan.request.release()), Map.copyOf(results)));
    }

    private Map<String, String> difference(ConfigurationRelease oldRelease, ConfigurationRelease next) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Set<String> domains = new java.util.TreeSet<>(next.domainVersions().keySet());
        if (oldRelease != null) domains.addAll(oldRelease.domainVersions().keySet());
        for (String domain : domains) {
            String oldValue = oldRelease == null ? "" : oldRelease.domainVersions().getOrDefault(domain, "");
            String newValue = next.domainVersions().getOrDefault(domain, "");
            if (!oldValue.equals(newValue)) result.put(domain, oldValue + " -> " + newValue);
        }
        return Map.copyOf(result);
    }
}
