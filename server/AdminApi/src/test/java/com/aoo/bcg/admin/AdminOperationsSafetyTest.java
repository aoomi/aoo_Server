package com.aoo.bcg.admin;

import org.junit.jupiter.api.Test;
import com.aoo.bcg.billing.InMemoryBillingService;
import com.aoo.bcg.billing.InMemoryLedgerRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AdminOperationsSafetyTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);

    @Test void assetAdjustmentRequiresTwoPeopleLimitsImmutableLedgerAndReconciliation() {
        var balances = new AssetAdjustmentService.InMemoryBalancePort();
        var service = new AssetAdjustmentService(new AssetAdjustmentService.Policy(
                new BigDecimal("100"), new BigDecimal("150")), balances, CLOCK);
        service.request("r1", "a1", 7, "GOLD", new BigDecimal("80"), 10, "support correction");
        assertThrows(IllegalArgumentException.class, () -> service.approve("r2", "a1", 10, "self"));
        service.approve("r3", "a1", 11, "verified ticket");
        assertEquals(AssetAdjustmentService.State.APPLIED,
                service.apply("r4", "a1", 10, "execute approved adjustment").state());
        assertEquals(new BigDecimal("80"), balances.current(7, "GOLD"));
        assertTrue(service.immutableLedger().getFirst().reconciled());
        assertTrue(service.verifyLedger());
        assertThrows(IllegalArgumentException.class, () -> service.request("r5", "a2", 7, "GOLD",
                new BigDecimal("80"), 10, "would exceed daily limit"));
    }

    @Test void productionAdapterUsesAuthoritativeBillingLedgerInsteadOfEditingBalanceDirectly() {
        var billing = new InMemoryBillingService(new InMemoryLedgerRepository(), CLOCK, Map.of(7L, 20L), "GOLD");
        var adapter = new BillingAssetBalancePort(billing, billing::balance);
        var service = new AssetAdjustmentService(new AssetAdjustmentService.Policy(
                new BigDecimal("100"), new BigDecimal("100")), adapter, CLOCK);
        service.request("r1", "billing-a1", 7, "GOLD", new BigDecimal("5"), 10, "ticket");
        service.approve("r2", "billing-a1", 11, "verified");
        assertEquals(AssetAdjustmentService.State.APPLIED,
                service.apply("r3", "billing-a1", 10, "apply").state());
        assertEquals(25L, billing.balance(7, "GOLD"));
    }

    @Test void investigationOnlyReadsBoundedSnapshotEventsAndReplay() {
        var repository = new GameInvestigationService.ReadOnlyRepository() {
            @Override public GameInvestigationService.Snapshot snapshot(long roomId) {
                return new GameInvestigationService.Snapshot(roomId, 516, "v1", 3, 2, CLOCK.instant(), Map.of());
            }
            @Override public List<GameInvestigationService.Event> events(long roomId, long from, long to) {
                return List.of(new GameInvestigationService.Event(2, "SETTLED", Map.of("winner", 7), CLOCK.instant()));
            }
            @Override public List<GameInvestigationService.ReplayFrame> replay(long roomId, int setId) {
                return List.of(new GameInvestigationService.ReplayFrame(2, "PUBLIC", 0, "m2", 1, "v1", new byte[]{1}));
            }
        };
        var service = new GameInvestigationService(repository, CLOCK);
        var evidence = service.investigate(new GameInvestigationService.Query(9, 1, 10, 1), 10, "appeal review");
        assertEquals(1, evidence.events().size());
        assertEquals(64, evidence.evidenceHash().length());
        assertThrows(IllegalArgumentException.class,
                () -> new GameInvestigationService.Query(9, 0, 100_000, 1));
        assertTrue(java.util.Arrays.stream(GameInvestigationService.class.getMethods())
                .noneMatch(method -> Set.of("deal", "shuffle", "control", "replaceCard").contains(method.getName())));
    }

    @Test void sensitiveExportIsScopedApprovedMaskedWatermarkedAndAudited() {
        var definition = new SensitiveExportService.Definition("user-support", Set.of("CLUB:99"), 2,
                Map.of("phone", SensitiveExportService.Mask.PHONE,
                        "email", SensitiveExportService.Mask.EMAIL,
                        "token", SensitiveExportService.Mask.TOKEN));
        var service = new SensitiveExportService(List.of(definition), (ignored, scope, limit) -> List.of(
                Map.of("phone", "13800138000", "email", "user@example.com", "token", "secret")), CLOCK);
        service.request("r1", "e1", "user-support", "CLUB:99", 1, 10, "support case");
        assertThrows(IllegalArgumentException.class, () -> service.approve("r2", "e1", 10, "self"));
        service.approve("r3", "e1", 11, "scope reviewed");
        var artifact = service.export("r4", "e1", 10, "download once");
        assertEquals("138****8000", artifact.rows().getFirst().get("phone"));
        assertEquals("[REDACTED]", artifact.rows().getFirst().get("token"));
        assertTrue(artifact.rows().getFirst().get("_watermark").contains("e1"));
        assertEquals(artifact.contentHash(), service.download("e1", 11).contentHash());
        assertThrows(IllegalArgumentException.class, () -> service.download("e1", 12));
        assertEquals(List.of("REQUESTED", "APPROVED", "EXPORTED", "DOWNLOADED"),
                service.audit().stream().map(SensitiveExportService.Audit::action).toList());
    }

    @Test void jobsUseShardLeaseFenceRetryIdempotencyAndApprovedManualRerun() {
        var store = new AdminJobCoordinator.InMemoryStore();
        var coordinator = new AdminJobCoordinator(store, CLOCK, Duration.ofSeconds(30), Duration.ofSeconds(1));
        var key = new AdminJobCoordinator.JobKey("reconciliation", "2026-08-24", 0, 4);
        coordinator.schedule("run-1", key, 2, 10, "daily reconciliation");
        AtomicLong fence = new AtomicLong();
        var success = coordinator.execute("run-1", "worker-a", (job, token) -> {
            fence.set(token); return AdminJobCoordinator.Outcome.success("balanced");
        });
        assertEquals(AdminJobCoordinator.State.SUCCEEDED, success.state());
        assertTrue(fence.get() > 0);
        assertEquals(success, coordinator.execute("run-1", "worker-b",
                (job, token) -> fail("completed job must be idempotent")));
        assertThrows(IllegalArgumentException.class,
                () -> coordinator.approvedManualRerun("run-2", "run-1", 10, 10, "self approval"));
        assertEquals("run-1", coordinator.approvedManualRerun("run-2", "run-1", 10, 11,
                "verified rerun").rerunOf());
    }

    @Test void resourceWritesRequireIfMatchAndRejectStaleVersions() {
        var store = new AdminResourceStore(CLOCK);
        var created = store.execute("operation-switches", "maintenance", 10,
                Map.of("requestId", "create-1", "reason", "scheduled", "enabled", false), null, "*");
        String etag = String.valueOf(created.get("_etag"));
        assertThrows(AdminPreconditionException.class, () -> store.execute("operation-switches", "maintenance", 10,
                Map.of("requestId", "change-stale", "reason", "stale", "enabled", true), null, "\"stale\""));
        var changed = store.execute("operation-switches", "maintenance", 10,
                Map.of("requestId", "change-1", "reason", "approved", "enabled", true), null, etag);
        assertNotEquals(etag, changed.get("_etag"));
        assertThrows(AdminPreconditionException.class, () -> store.execute("operation-switches", "maintenance", 10,
                Map.of("requestId", "change-2", "reason", "lost update", "enabled", false), null, etag));
    }
}
