package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomOrchestratorDissolveContractTest {
    private static final RoomOrchestrator.RoomIdentity IDENTITY =
            new RoomOrchestrator.RoomIdentity(42L, "instance-42", 2003,
                    "liangshan2003", "liangshan2003-v1", 7L, 11L, 3L);
    private static final RoomOrchestrator.RoomStaticIdentity STATIC =
            new RoomOrchestrator.RoomStaticIdentity(42L, 2003,
                    "liangshan2003", "liangshan2003-v1");
    private static final RoomSnapshotMetadataSource METADATA = new RoomSnapshotMetadataSource(
            19L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "card-v1", "events-v1", "protocol-v2", "snapshot-v1", 4L, "LS",
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");

    @Test
    void applyCarriesCompleteStaticRuntimeAndVoteIdentity() {
        var command = new RoomOrchestrator.DissolveApplyCommand(IDENTITY, STATIC, "session-42",
                17L, RoomOrchestrator.DissolvePermission.APPLICANT, RoomState.PLAYING,
                "vote-42", Set.of(17L, 18L), Instant.ofEpochSecond(100), true,
                true, 3L, "command-42", "idem-42", METADATA);
        assertEquals(3L, command.expectedStateVersion());
        assertEquals(Set.of(17L, 18L), command.eligibleMemberIds());
        assertEquals(METADATA, command.metadata());
    }

    @Test
    void voteRejectsStaticOrRuntimeStateDriftAtConstruction() {
        var wrongStatic = new RoomOrchestrator.RoomStaticIdentity(42L, 2003,
                "other-game", "liangshan2003-v1");
        assertThrows(IllegalArgumentException.class, () -> new RoomOrchestrator.DissolveVoteCommand(
                IDENTITY, wrongStatic, "session-42", 18L,
                RoomOrchestrator.DissolvePermission.ELIGIBLE_MEMBER, RoomState.PLAYING,
                "vote-42", true, 3L, "command-43", "idem-43", METADATA));
        assertThrows(IllegalArgumentException.class, () -> new RoomOrchestrator.DissolveVoteCommand(
                IDENTITY, STATIC, "session-42", 18L,
                RoomOrchestrator.DissolvePermission.ELIGIBLE_MEMBER, RoomState.PLAYING,
                "vote-42", true, 2L, "command-44", "idem-44", METADATA));
    }

    @Test
    void resultCannotClaimNonTerminalVoteAsTerminalOrUseRecoveryReceipt() {
        assertThrows(IllegalArgumentException.class, () -> new RoomOrchestrator.DissolveResult(
                IDENTITY, "vote-42", 17L, Set.of(17L, 18L), Instant.ofEpochSecond(100),
                true, Map.of(17L, true), RoomOrchestrator.DissolveDecision.VOTING,
                true, RoomOrchestrator.DissolveStatus.APPLIED, "command-42", "idem-42"));
        assertThrows(IllegalArgumentException.class, () -> new RoomOrchestrator.DissolveResult(
                IDENTITY, "vote-42", 17L, Set.of(17L, 18L), Instant.ofEpochSecond(100),
                true, Map.of(17L, true), RoomOrchestrator.DissolveDecision.VOTING,
                false, RoomOrchestrator.DissolveStatus.RECOVERED, "command-42", null));
    }
}
