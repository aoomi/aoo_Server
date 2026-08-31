package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.common.config.RoomRuleSnapshot;
import com.aoo.bcg.common.operations.OperationSwitches;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomIsolationAndApprovalTest {
    private static final String HASH = "b".repeat(64);
    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes();

    @Test void approvalSignatureRejectsTamperingAndConcurrentDraftMutation() {
        GameConfigurationDraftCompiler.ValidatedDraft approvedContent = validated(3);
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC);
        SignedReleaseApproval approval = SignedReleaseApproval.approve(approvedContent, 9001, clock, KEY);
        approval.verify(approvedContent, KEY);
        assertThrows(IllegalStateException.class, () -> approval.verify(validated(4), KEY));
        SignedReleaseApproval forged = new SignedReleaseApproval(approval.draftId(), approval.contentHash(),
                approval.approverId(), approval.approvedAt(), Base64Helper.flip(approval.signature()));
        assertThrows(IllegalStateException.class, () -> forged.verify(approvedContent, KEY));
    }

    @Test void publicationApprovalAlsoBindsRolloutChannelsRegionsAndActivationTime() {
        GameConfigurationDraftCompiler.ValidatedDraft content = validated(3);
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC);
        Instant activateAt = Instant.parse("2026-08-25T00:00:00Z");
        PlayAvailabilityPolicy policy = new PlayAvailabilityPolicy(true, false, 10, Set.of("APP"), Set.of(), "all");
        SignedReleaseApproval approval = SignedReleaseApproval.approvePublication(content, policy, activateAt, 9001, clock, KEY);
        assertDoesNotThrow(() -> approval.verifyPublication(content, policy, activateAt, KEY));
        assertThrows(IllegalStateException.class, () -> approval.verifyPublication(content,
                new PlayAvailabilityPolicy(true, false, 20, Set.of("APP"), Set.of(), "all"), activateAt, KEY));
        assertThrows(IllegalStateException.class, () -> approval.verifyPublication(content, policy, activateAt.plusSeconds(1), KEY));
    }

    private static PublishedGameConfiguration<Object> configuration(long configId, String version, int rounds, Instant time) {
        RoomRuleSnapshot snapshot = new RoomRuleSnapshot(configId, 62, version, "components-v1", "room-v1",
                "flow-v1", "score-v1", "ui-v1", time, Map.of("roundCount", rounds));
        return new PublishedGameConfiguration<>(snapshot,
                new ReleaseManifest(62, version, "protocol-v2", "components-v1", "client-v1", time, Map.of()), null);
    }

    private static GameConfigurationDraftCompiler.ValidatedDraft validated(int playerCount) {
        RuleSchemaRegistry registry = new RuleSchemaRegistry();
        registry.register(new RuleSchema("poker.test", "schema-v1", List.of(new RuleFieldDefinition("playerCount", Set.of(),
                RuleFieldDefinition.ValueType.INTEGER, "人", true, null, BigDecimal.valueOf(2), BigDecimal.valueOf(4),
                Set.of(), "人数", "参与人数")), List.of(), List.of()));
        List<ComponentCapability> components = List.of(
                component("rules", ComponentCapability.Kind.RULE, Set.of("playerCount"), Set.of("rule.ok"), Map.of()),
                component("flow", ComponentCapability.Kind.FLOW_ACTION, Set.of("rule.ok"), Set.of("flow.ok"), Map.of("rules", range())),
                component("score", ComponentCapability.Kind.SCORING, Set.of("flow.ok"), Set.of("score.total"), Map.of("flow", range())),
                component("ui", ComponentCapability.Kind.UI, Set.of("score.total"), Set.of("ui.summary"), Map.of("score", range())));
        Map<String, Object> rules = Map.of("playerCount", playerCount);
        GameConfigurationDraft draft = new GameConfigurationDraft("draft-approval", 62, "poker.test", "play-v2", "schema-v1",
                rules, List.of(), components,
                new ReleaseManifest(62, "play-v2", "protocol-v2", "components-v2", "client-v2", Instant.EPOCH,
                        Map.of("rules", HASH, "components", HASH, "ui", HASH)),
                new OperationSwitches(true, true, false, "client-v2", "all", "play-v2"), Set.of(rules), Set.of(rules),
                "房主支付", Instant.parse("2026-08-25T00:00:00Z"));
        return new GameConfigurationDraftCompiler(registry).compile(draft);
    }

    private static ComponentCapability component(String id, ComponentCapability.Kind kind, Set<String> in,
            Set<String> out, Map<String, ComponentCapability.VersionRange> dependencies) {
        return new ComponentCapability(id, "1.0.0", kind, in, out, dependencies, Set.of(), Set.of(), Set.of("poker.test"));
    }
    private static ComponentCapability.VersionRange range() { return new ComponentCapability.VersionRange("1.0.0", "2.0.0"); }

    private static final class Base64Helper {
        static String flip(String source) {
            byte[] bytes = java.util.Base64.getDecoder().decode(source); bytes[0] ^= 1;
            return java.util.Base64.getEncoder().encodeToString(bytes);
        }
    }
}
