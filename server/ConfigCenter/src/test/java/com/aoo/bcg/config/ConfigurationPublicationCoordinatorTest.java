package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.common.operations.OperationSwitches;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigurationPublicationCoordinatorTest {
    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes();
    private static final String HASH = "c".repeat(64);

    @Test void prewarmsAndAtomicallyActivatesOnlyAtAuthoritativeTimeWithEveryNodeAck() {
        Instant start = Instant.parse("2026-08-24T00:00:00Z");
        MutableClock clock = new MutableClock(start);
        AtomicConfigurationReleaseStore store = new AtomicConfigurationReleaseStore();
        ConfigurationRelease old = release("release-1", "play-v1", "components-v1", "protocol-v1", "client-v1", "old", "schema-v1", start);
        store.publish(old, "");
        TestNode first = new TestNode("node-a", old.releaseId());
        TestNode second = new TestNode("node-b", old.releaseId());
        ConfigurationPublicationCoordinator coordinator = new ConfigurationPublicationCoordinator(
                store, List.of(first, second), clock, Duration.ofSeconds(10), KEY);
        var content = validated("draft-2", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2");
        PlayAvailabilityPolicy availability = availability(10);
        var approval = SignedReleaseApproval.approvePublication(content, availability, start.plusSeconds(60), 7001, clock, KEY);
        ConfigurationRelease next = release("release-2", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2", start.plusSeconds(60));
        var plan = coordinator.schedule(new ConfigurationPublicationCoordinator.ReleaseRequest(next, content, approval,
                old.releaseId(), 8001, "灰度发布成都", start.plusSeconds(60), availability, "新建房间"));

        coordinator.tick();
        assertEquals(ConfigurationPublicationCoordinator.PlanState.SCHEDULED, plan.state());
        clock.set(start.plusSeconds(50)); coordinator.tick();
        assertEquals(ConfigurationPublicationCoordinator.PlanState.PREWARMED, plan.state());
        assertEquals(old.releaseId(), store.active().orElseThrow().releaseId());
        clock.set(start.plusSeconds(60)); coordinator.tick();
        assertEquals(ConfigurationPublicationCoordinator.PlanState.ACTIVE, plan.state());
        assertEquals(next.releaseId(), store.active().orElseThrow().releaseId());
        assertEquals(next.releaseId(), first.activeReleaseId());
        assertEquals(next.releaseId(), second.activeReleaseId());
        var activated = coordinator.audits().stream().filter(audit -> audit.action() == ConfigurationPublicationCoordinator.AuditAction.ACTIVATED).findFirst().orElseThrow();
        assertEquals(8001, activated.operatorId());
        assertEquals(7001, activated.approverId());
        assertTrue(activated.rolloutScope().contains("percent=10"));
        assertTrue(activated.domainDifference().containsKey("ui"));
        assertEquals(Set.of("node-a", "node-b"), activated.nodeResults().keySet());

        ConfigurationVersionAdmissionGate gate = new ConfigurationVersionAdmissionGate();
        assertDoesNotThrow(() -> gate.requireNewRoomAllowed(next.releaseId(), first.activeReleaseId(), content.source().switches()));
        assertThrows(IllegalStateException.class, () -> gate.requireNewRoomAllowed(next.releaseId(), old.releaseId(), content.source().switches()));
    }

    @Test void activationFailureStopsExpansionRollsBackIndexAndInvalidatesEveryCacheLayer() {
        Instant start = Instant.parse("2026-08-24T01:00:00Z");
        MutableClock clock = new MutableClock(start);
        AtomicConfigurationReleaseStore store = new AtomicConfigurationReleaseStore();
        ConfigurationRelease old = release("release-10", "play-v1", "components-v1", "protocol-v1", "client-v1", "old", "schema-v1", start);
        store.publish(old, "");
        TestNode first = new TestNode("node-a", old.releaseId());
        TestNode broken = new TestNode("node-b", old.releaseId()); broken.failActivate = true;
        TestNode untouched = new TestNode("node-c", old.releaseId());
        ConfigurationPublicationCoordinator coordinator = new ConfigurationPublicationCoordinator(
                store, List.of(first, broken, untouched), clock, Duration.ZERO, KEY);
        var content = validated("draft-11", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2");
        PlayAvailabilityPolicy availability = availability(100);
        var approval = SignedReleaseApproval.approvePublication(content, availability, start, 7001, clock, KEY);
        ConfigurationRelease next = release("release-11", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2", start);
        var plan = coordinator.schedule(new ConfigurationPublicationCoordinator.ReleaseRequest(next, content, approval,
                old.releaseId(), 8001, "故障注入", start, availability, "大厅与新建房间"));
        coordinator.tick();

        assertEquals(ConfigurationPublicationCoordinator.PlanState.FAILED_ROLLED_BACK, plan.state());
        assertEquals(old.releaseId(), store.active().orElseThrow().releaseId());
        assertEquals(old.releaseId(), first.activeReleaseId());
        assertEquals(old.releaseId(), broken.activeReleaseId());
        assertEquals(0, untouched.activateCalls, "node after first failure must not receive candidate activation");
        for (TestNode node : List.of(first, broken, untouched))
            assertEquals(Set.copyOf(java.util.EnumSet.allOf(ConfigurationPublicationCoordinator.CacheLayer.class)), node.invalidated);
        assertTrue(coordinator.audits().stream().anyMatch(audit -> audit.action() == ConfigurationPublicationCoordinator.AuditAction.FAILED_ROLLED_BACK));
    }

    @Test void prepareFailureNeverMovesIndexAndRollbackRequiresCompleteLiveDependencies() {
        Instant now = Instant.parse("2026-08-24T02:00:00Z");
        MutableClock clock = new MutableClock(now);
        AtomicConfigurationReleaseStore store = new AtomicConfigurationReleaseStore();
        ConfigurationRelease old = release("release-20", "play-v1", "components-v1", "protocol-v1", "client-v1", "old", "schema-v1", now);
        store.publish(old, "");
        TestNode ok = new TestNode("node-a", old.releaseId());
        TestNode broken = new TestNode("node-b", old.releaseId()); broken.failPrepare = true;
        ConfigurationPublicationCoordinator coordinator = new ConfigurationPublicationCoordinator(store, List.of(ok, broken), clock, Duration.ZERO, KEY);
        var content = validated("draft-21", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2");
        ConfigurationRelease next = release("release-21", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2", now);
        PlayAvailabilityPolicy availability = availability(100);
        var plan = coordinator.schedule(new ConfigurationPublicationCoordinator.ReleaseRequest(next, content,
                SignedReleaseApproval.approvePublication(content, availability, now, 7001, clock, KEY), old.releaseId(), 8001,
                "预热故障", now, availability, "新房"));
        coordinator.tick();
        assertEquals(ConfigurationPublicationCoordinator.PlanState.FAILED, plan.state());
        assertEquals(old.releaseId(), store.active().orElseThrow().releaseId());
        assertEquals(0, ok.activateCalls);

        AtomicConfigurationReleaseStore rollbackStore = new AtomicConfigurationReleaseStore();
        ConfigurationRelease target = release("release-30", "play-v1", "components-v1", "protocol-v1", "client-v1", "old", "schema-v1", now);
        ConfigurationRelease active = release("release-31", "play-v2", "components-v2", "protocol-v2", "client-v2", "all", "schema-v2", now.plusSeconds(1));
        rollbackStore.publish(target, ""); rollbackStore.publish(active, target.releaseId());
        TestNode rollbackNode = new TestNode("node-r", active.releaseId());
        ConfigurationPublicationCoordinator rollbackCoordinator = new ConfigurationPublicationCoordinator(
                rollbackStore, List.of(rollbackNode), clock, Duration.ZERO, KEY);
        assertEquals(target, rollbackCoordinator.rollback(target.releaseId(), active.releaseId(), 8001, "业务回滚", "新房目录"));
        assertEquals(target.releaseId(), rollbackNode.activeReleaseId());
        rollbackNode.supports = false;
        assertThrows(IllegalStateException.class, () -> rollbackCoordinator.rollback(active.releaseId(), target.releaseId(), 8001, "依赖已下线", "新房目录"));

        AtomicConfigurationReleaseStore incompleteStore = new AtomicConfigurationReleaseStore();
        ConfigurationRelease incomplete = ConfigurationRelease.create("release-x", Map.of("rules", "v1"), now);
        incompleteStore.publish(incomplete, ""); incompleteStore.publish(active, incomplete.releaseId());
        ConfigurationPublicationCoordinator incompleteCoordinator = new ConfigurationPublicationCoordinator(
                incompleteStore, List.of(new TestNode("node-x", active.releaseId())), clock, Duration.ZERO, KEY);
        assertThrows(IllegalArgumentException.class, () -> incompleteCoordinator.rollback(incomplete.releaseId(), active.releaseId(), 8001, "不完整快照", "全部"));
    }

    @Test void deprecationMustStopDrainArchiveAndRetireInOrder() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        PlayDeprecationWorkflow workflow = new PlayDeprecationWorkflow(clock, 2);
        assertThrows(IllegalStateException.class, () -> workflow.archive(1, "越级", "archive://x"));
        workflow.stopNewRooms(1, "停止新建");
        workflow.beginDrain(1, "排空存量");
        assertThrows(IllegalStateException.class, () -> workflow.archive(1, "仍有房间", "archive://x"));
        workflow.updateActiveRooms(0);
        workflow.archive(1, "归档", "archive://play-v1");
        assertThrows(IllegalStateException.class, () -> workflow.retireComponents(1, "下线", true));
        workflow.retireComponents(1, "依赖清零后下线", false);
        assertEquals(PlayDeprecationWorkflow.State.COMPONENTS_RETIRED, workflow.state());
        assertEquals(4, workflow.events().size());
    }

    private static ConfigurationRelease release(String id, String rules, String components, String protocol,
            String ui, String switches, String regions, Instant at) {
        return ConfigurationRelease.create(id, Map.of("rules", rules, "components", components, "protocol", protocol,
                "ui", ui, "switches", switches, "regions", regions), at);
    }

    private static GameConfigurationDraftCompiler.ValidatedDraft validated(String draftId, String playVersion,
            String componentVersion, String protocolVersion, String clientVersion, String rollout, String schemaVersion) {
        RuleSchemaRegistry registry = new RuleSchemaRegistry();
        registry.register(new RuleSchema("poker.release", schemaVersion, List.of(new RuleFieldDefinition("playerCount", Set.of(),
                RuleFieldDefinition.ValueType.INTEGER, "人", true, null, BigDecimal.valueOf(2), BigDecimal.valueOf(4),
                Set.of(), "人数", "参与人数")), List.of(), List.of()));
        List<ComponentCapability> components = List.of(
                component("rules", ComponentCapability.Kind.RULE, Set.of("playerCount"), Set.of("rule.ok"), Map.of()),
                component("flow", ComponentCapability.Kind.FLOW_ACTION, Set.of("rule.ok"), Set.of("flow.ok"), Map.of("rules", range())),
                component("score", ComponentCapability.Kind.SCORING, Set.of("flow.ok"), Set.of("score.total"), Map.of("flow", range())),
                component("ui", ComponentCapability.Kind.UI, Set.of("score.total"), Set.of("ui.summary"), Map.of("score", range())));
        Map<String, Object> rules = Map.of("playerCount", 3);
        GameConfigurationDraft draft = new GameConfigurationDraft(draftId, 62, "poker.release", playVersion, schemaVersion,
                rules, List.of(), components,
                new ReleaseManifest(62, playVersion, protocolVersion, componentVersion, clientVersion, Instant.EPOCH,
                        Map.of("rules", HASH, "components", HASH, "ui", HASH)),
                new OperationSwitches(true, true, false, clientVersion, rollout, playVersion), Set.of(rules), Set.of(rules),
                "房主支付", Instant.EPOCH);
        return new GameConfigurationDraftCompiler(registry).compile(draft);
    }

    private static ComponentCapability component(String id, ComponentCapability.Kind kind, Set<String> in,
            Set<String> out, Map<String, ComponentCapability.VersionRange> dependencies) {
        return new ComponentCapability(id, "1.0.0", kind, in, out, dependencies, Set.of(), Set.of(), Set.of("poker.release"));
    }
    private static ComponentCapability.VersionRange range() { return new ComponentCapability.VersionRange("1.0.0", "2.0.0"); }
    private static PlayAvailabilityPolicy availability(int percent) {
        return new PlayAvailabilityPolicy(true, false, percent, Set.of("APP"), Set.of(), "all");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void set(Instant instant) { this.instant = instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    private static final class TestNode implements ConfigurationPublicationCoordinator.PublicationNode {
        final String id;
        String active;
        boolean failPrepare;
        boolean failActivate;
        boolean supports = true;
        int activateCalls;
        Set<ConfigurationPublicationCoordinator.CacheLayer> invalidated = new HashSet<>();
        TestNode(String id, String active) { this.id = id; this.active = active; }
        @Override public String nodeId() { return id; }
        @Override public void prepare(ConfigurationRelease release) { if (failPrepare) throw new IllegalStateException("prepare injection"); }
        @Override public void activate(ConfigurationRelease release) {
            activateCalls++;
            if (failActivate) throw new IllegalStateException("activate injection");
            active = release.releaseId();
        }
        @Override public void restore(ConfigurationRelease release) { active = release.releaseId(); }
        @Override public void abort(String releaseId) { }
        @Override public void invalidateCaches(Set<ConfigurationPublicationCoordinator.CacheLayer> layers) { invalidated.addAll(layers); }
        @Override public boolean supports(ConfigurationRelease release) { return supports; }
        @Override public String activeReleaseId() { return active; }
    }
}
