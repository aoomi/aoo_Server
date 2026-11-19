package com.aoo.bcg.common.authority;

import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gamespi.RoomState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionRoomOrchestratorAdapterDissolveTest {
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
    void applyAcceptsOnlyAuthorityResultFromTheNextStateVersion() {
        var command = applyCommand("command-42", "idem-42");
        var next = new RoomOrchestrator.RoomIdentity(42L, "instance-42", 2003,
                "liangshan2003", "liangshan2003-v1", 7L, 11L, 4L);
        var expected = result(next, RoomOrchestrator.DissolveStatus.APPLIED,
                "command-42", "idem-42");
        var adapter = new ProductionRoomOrchestratorAdapter(port(expected, null, null));
        assertEquals(expected, adapter.dissolveApply(command));
    }

    @Test
    void applyRejectsAuthorityResultFromTheWrongStateVersion() {
        var command = applyCommand("command-43", "idem-43");
        var wrong = result(IDENTITY, RoomOrchestrator.DissolveStatus.APPLIED,
                "command-43", "idem-43");
        var adapter = new ProductionRoomOrchestratorAdapter(port(wrong, null, null));
        assertThrows(IllegalStateException.class, () -> adapter.dissolveApply(command));
    }

    @Test
    void recoverDoesNotAcceptACommandReceipt() {
        assertThrows(IllegalArgumentException.class, () -> new RoomOrchestrator.DissolveResult(
                IDENTITY, "vote-42", 17L, Set.of(17L, 18L), Instant.ofEpochSecond(100),
                true, Map.of(17L, true), RoomOrchestrator.DissolveDecision.VOTING,
                false, RoomOrchestrator.DissolveStatus.RECOVERED, "command-42", null));
    }

    private static RoomOrchestrator.DissolveApplyCommand applyCommand(String commandId,
                                                                        String idempotencyKey) {
        return new RoomOrchestrator.DissolveApplyCommand(IDENTITY, STATIC, "session-42", 17L,
                RoomOrchestrator.DissolvePermission.APPLICANT, RoomState.PLAYING, "vote-42",
                Set.of(17L, 18L), Instant.ofEpochSecond(100), true, true,
                3L, commandId, idempotencyKey, METADATA);
    }

    private static RoomOrchestrator.DissolveResult result(RoomOrchestrator.RoomIdentity identity,
                                                           RoomOrchestrator.DissolveStatus status,
                                                           String commandId, String idempotencyKey) {
        return new RoomOrchestrator.DissolveResult(identity, "vote-42", 17L, Set.of(17L, 18L),
                Instant.ofEpochSecond(100), true, Map.of(17L, true),
                RoomOrchestrator.DissolveDecision.VOTING, false, status, commandId, idempotencyKey);
    }

    private static ProductionRoomOrchestratorAdapter.AuthorityPort port(
            RoomOrchestrator.DissolveResult apply, RoomOrchestrator.DissolveResult vote,
            RoomOrchestrator.DissolveResult recover) {
        return new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY,
                        com.aoo.bcg.gamespi.StatePayload.copyOf(Map.of("stateVersion", IDENTITY.stateVersion())));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY,
                        com.aoo.bcg.gamespi.StatePayload.copyOf(Map.of("stateVersion", IDENTITY.stateVersion())));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.DissolveResult dissolveApply(
                    RoomOrchestrator.DissolveApplyCommand command) { return apply; }
            @Override public RoomOrchestrator.DissolveResult dissolveVote(
                    RoomOrchestrator.DissolveVoteCommand command) { return vote; }
            @Override public RoomOrchestrator.DissolveResult dissolveRecover(
                    RoomOrchestrator.DissolveRecoverCommand command) { return recover; }
        };
    }
}
