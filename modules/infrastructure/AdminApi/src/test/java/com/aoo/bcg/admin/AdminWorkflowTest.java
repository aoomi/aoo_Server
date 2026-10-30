package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminWorkflowTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-24T11:00:00Z"), ZoneOffset.UTC);

    @Test void gameProfileTraversesValidatedFourEyesCanaryActivationAndRollback() {
        List<String> publications = new ArrayList<>();
        var workflow = new GameProfileReleaseWorkflow(
                (gameId, profile) -> profile.containsKey("rounds") ? List.of() : List.of("rounds required"),
                new GameProfileReleaseWorkflow.Publisher() {
                    @Override public void activate(GameProfileReleaseWorkflow.Release release, String requestId,
                            long operatorId, String reason) { publications.add("activate:" + release.version()); }
                    @Override public void rollback(long gameId, String previousVersion, String requestId,
                            long operatorId, String reason) { publications.add("rollback:" + previousVersion); }
                }, CLOCK);

        workflow.draft("r1", "release-1", 516, "v2", Map.of("rounds", 16), "v1", 10, "draft");
        workflow.validate("r2", "release-1", 10, "validation complete");
        workflow.submit("r3", "release-1", 10, "submit");
        assertThrows(IllegalArgumentException.class,
                () -> workflow.approve("r4", "release-1", 10, "self approve"));
        workflow.approve("r5", "release-1", 11, "reviewed");
        workflow.deployCanary("r6", "release-1", 5, 10, "five percent");
        assertEquals(GameProfileReleaseWorkflow.State.ACTIVE,
                workflow.activate("r7", "release-1", 10, "canary healthy").state());
        assertEquals(GameProfileReleaseWorkflow.State.ROLLED_BACK,
                workflow.rollback("r8", "release-1", 10, "rollback drill").state());
        assertEquals(List.of("activate:v2", "rollback:v1"), publications);
        assertEquals(7, workflow.history().size());
    }

    @Test void allUserAndClubMutationsCarryOperatorReasonBeforeAfterAndTamperEvidence() {
        var store = new AdminChangeJournal.InMemoryStore();
        var journal = new AdminChangeJournal(store, CLOCK, new ObjectMapper());
        var service = new ManagedEntityService(journal);

        service.change("change-1", AdminChangeJournal.EntityType.USER, "7", 10,
                "risk lock", Map.of("status", "LOCKED"));
        service.change("change-2", AdminChangeJournal.EntityType.CLUB, "99", 11,
                "rename", Map.of("displayName", "New Club"));

        assertTrue(journal.verify());
        assertEquals(Map.of("status", "LOCKED"), store.all().getFirst().after());
        assertEquals(Map.of(), store.all().getFirst().before());
        assertThrows(IllegalArgumentException.class, () -> service.change("change-3",
                AdminChangeJournal.EntityType.USER, "7", 10, "illegal asset path", Map.of("balance", 999)));
    }
}
