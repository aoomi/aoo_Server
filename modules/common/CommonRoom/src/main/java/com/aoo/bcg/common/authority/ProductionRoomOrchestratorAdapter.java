package com.aoo.bcg.common.authority;

import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.StatePayload;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single production lifecycle boundary for the room authority.
 *
 * <p>The port is deliberately typed: Bootstrap owns the durable room-lock,
 * runtime identity store, Registry and CAS implementation, while this class
 * prevents a caller from silently accepting a result from another generation
 * or from an unimplemented lifecycle operation.  It does not provide a second
 * room implementation and it never manufactures identity values.</p>
 */
public final class ProductionRoomOrchestratorAdapter implements RoomOrchestrator {
    /** Bootstrap authority port; all durable and Registry work remains there. */
    public interface AuthorityPort {
        CreateResult create(CreateCommand command);

        RecoverResult recover(RecoverCommand command);

        /** Validate static room gameplay identity against the durable room-lock before destroy. */
        void validateDestroyIdentity(RoomIdentity expectedIdentity);

        LeaveResult leave(LeaveCommand command);

        DestroyResult destroy(DestroyCommand command);

        /** Execute is the only gameplay mutation crossing this authority boundary. */
        default ExecuteResult execute(ExecuteCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_NOT_WIRED");
        }

        /** Reconnect must be served by the same durable room authority as create/execute. */
        default ReconnectResult reconnect(ReconnectCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_NOT_WIRED");
        }

        /** Handoff is a typed room-owner CAS; Gateway transport is wired only in a later batch. */
        default HandoffResult handoff(HandoffCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_NOT_WIRED");
        }

        /** Provider wiring is intentionally absent in A1; callers fail closed until it is supplied. */
        default ReadyResult ready(ReadyCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_NOT_WIRED");
        }

        /** Durable dissolve apply/vote are wired by the v2.64 authority batch only. */
        default DissolveResult dissolveApply(DissolveApplyCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DISSOLVE_APPLY_NOT_WIRED");
        }

        default DissolveResult dissolveVote(DissolveVoteCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DISSOLVE_VOTE_NOT_WIRED");
        }

        default DissolveResult dissolveRecover(DissolveRecoverCommand command) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DISSOLVE_RECOVER_NOT_WIRED");
        }
    }

    private final AuthorityPort authority;
    private final ConcurrentHashMap<Long, Object> readyLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, java.util.Map<String, ReadyReceipt>> readyReceipts =
            new ConcurrentHashMap<>();

    public ProductionRoomOrchestratorAdapter(AuthorityPort authority) {
        this.authority = Objects.requireNonNull(authority, "authority");
    }

    @Override
    public CreateResult create(CreateCommand command) {
        Objects.requireNonNull(command, "command");
        CreateResult result = requireResult(authority.create(command), "CREATE");
        RoomIdentity identity = result.identity();
        if (identity.roomId() != command.context().roomId()
                || !identity.gameCode().equals(command.gameCode())
                || !identity.playVersion().equals(command.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_CREATE_IDENTITY_CONFLICT");
        }
        requireStateVersion(result.state(), identity, "CREATE");
        return result;
    }

    @Override
    public RecoverResult recover(RecoverCommand command) {
        Objects.requireNonNull(command, "command");
        RecoverResult result = requireResult(authority.recover(command), "RECOVER");
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() < command.expectedIdentity().stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_RECOVER_IDENTITY_CONFLICT");
        }
        requireStateVersion(result.state(), result.identity(), "RECOVER");
        return result;
    }

    /**
     * Serializes ready commands while the same authority owns the actual state-version CAS.
     * Receipts belong to a command, not to the room's first ready operation. A completed
     * request may be retried after its authoritative revision and phase have advanced.
     */
    @Override
    public ReadyResult ready(ReadyCommand command) {
        Objects.requireNonNull(command, "command");
        long roomId = command.expectedIdentity().roomId();
        synchronized (readyLocks.computeIfAbsent(roomId, ignored -> new Object())) {
            var receipts = readyReceipts.computeIfAbsent(roomId, ignored -> new java.util.HashMap<>());
            ReadyReceipt previous = receipts.get(command.commandId());
            if (previous != null) {
                if (command.permission() != ReadyPermission.SEATED_MEMBER) {
                    throw new SecurityException("ROOM_ORCHESTRATOR_READY_PERMISSION_DENIED");
                }
                if (!sameReadyCommand(previous.command(), command)
                        || command.expectedIdentity().stateVersion()
                        < previous.command().expectedIdentity().stateVersion()) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_CAS_CONFLICT");
                }
                return previous.result();
            }
            // Only an already completed request bypasses the fresh WAITING-state gate.
            validateReadyCommand(command);
            for (ReadyReceipt receipt : receipts.values()) {
                if (sameGeneration(receipt.command().expectedIdentity(), command.expectedIdentity())
                        && command.expectedIdentity().stateVersion()
                        <= receipt.command().expectedIdentity().stateVersion()) {
                    throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_CAS_CONFLICT");
                }
            }
            ReadyResult result = requireResult(authority.ready(command), "READY");
            validateReadyResult(command, result);
            receipts.put(command.commandId(), new ReadyReceipt(command, result));
            return result;
        }
    }

    @Override
    public DestroyResult destroy(DestroyCommand command) {
        Objects.requireNonNull(command, "command");
        authority.validateDestroyIdentity(command.expectedIdentity());
        DestroyResult result = requireResult(authority.destroy(command), "DESTROY");
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() != command.expectedIdentity().stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_IDENTITY_CONFLICT");
        }
        if (!result.destroyed() && !"ALREADY_DESTROYED".equals(result.reason())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DESTROY_NOT_CONFIRMED:" + result.reason());
        }
        return result;
    }

    @Override
    public LeaveResult leave(LeaveCommand command) {
        Objects.requireNonNull(command, "command");
        LeaveResult result = requireResult(authority.leave(command), "LEAVE");
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() != command.expectedIdentity().stateVersion()
                || result.accountId() != command.accountId()
                || !result.commandId().equals(command.commandId())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_CONFLICT");
        }
        if (result.status() == RoomOrchestrator.LeaveStatus.LEFT && result.seatNo() < 0) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_RESULT_CONFLICT");
        }
        if (result.status() == RoomOrchestrator.LeaveStatus.ALREADY_LEFT
                && result.seatNo() != -1) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_RESULT_CONFLICT");
        }
        return result;
    }

    @Override
    public DissolveResult dissolveApply(DissolveApplyCommand command) {
        Objects.requireNonNull(command, "command");
        DissolveResult result = requireResult(authority.dissolveApply(command), "DISSOLVE_APPLY");
        validateDissolveResult(command.expectedIdentity(), command.voteId(),
                command.expectedStateVersion(), result, DissolveStatus.APPLIED, command.commandId(),
                command.idempotencyKey(), true);
        return result;
    }

    @Override
    public DissolveResult dissolveVote(DissolveVoteCommand command) {
        Objects.requireNonNull(command, "command");
        DissolveResult result = requireResult(authority.dissolveVote(command), "DISSOLVE_VOTE");
        validateDissolveResult(command.expectedIdentity(), command.voteId(),
                command.expectedStateVersion(), result, DissolveStatus.VOTED, command.commandId(),
                command.idempotencyKey(), true);
        return result;
    }

    @Override
    public DissolveResult dissolveRecover(DissolveRecoverCommand command) {
        Objects.requireNonNull(command, "command");
        DissolveResult result = requireResult(authority.dissolveRecover(command), "DISSOLVE_RECOVER");
        validateDissolveResult(command.expectedIdentity(), command.voteId(),
                command.expectedIdentity().stateVersion(), result, DissolveStatus.RECOVERED,
                null, null, false);
        if (result.identity().stateVersion() < command.expectedIdentity().stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DISSOLVE_RECOVER_STATE_VERSION_CONFLICT");
        }
        return result;
    }

    @Override
    public ExecuteResult execute(ExecuteCommand command) {
        if (command == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_COMMAND_REQUIRED");
        }
        ExecuteResult result = requireResult(authority.execute(command), "EXECUTE");
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() != Math.addExact(
                        command.expectedIdentity().stateVersion(), 1L)
                || !result.commandResult().requestId().equals(command.request().requestId())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_EXECUTE_IDENTITY_CONFLICT");
        }
        return result;
    }

    @Override
    public ReconnectResult reconnect(ReconnectCommand command) {
        if (command == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_COMMAND_REQUIRED");
        }
        ReconnectResult result = requireResult(authority.reconnect(command), "RECONNECT");
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() < command.expectedIdentity().stateVersion()
                || result.identity().stateVersion() < command.lastKnownStateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_RECONNECT_IDENTITY_CONFLICT");
        }
        requireStateVersion(result.state(), result.identity(), "RECONNECT");
        return result;
    }

    @Override
    public HandoffResult handoff(HandoffCommand command) {
        if (command == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_COMMAND_REQUIRED");
        }
        HandoffResult result = requireResult(authority.handoff(command), "HANDOFF");
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() != command.expectedStateVersion()
                || !result.sessionId().equals(command.toSessionId())
                || result.connectionGeneration() != command.toConnectionGeneration()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_HANDOFF_IDENTITY_CONFLICT");
        }
        return result;
    }

    private static <T extends AuthorityResult> T requireResult(T result, String operation) {
        if (result == null || result.identity() == null) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_" + operation + "_IDENTITY_REQUIRED");
        }
        return result;
    }

    private static void requireStateVersion(StatePayload state, RoomIdentity identity,
                                            String operation) {
        Object raw = state.asMap().get("stateVersion");
        if (!(raw instanceof Number number) || number.longValue() < 0
                || number.longValue() != identity.stateVersion()) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_" + operation
                    + "_STATE_VERSION_CONFLICT");
        }
    }

    private static boolean sameGeneration(RoomIdentity expected, RoomIdentity actual) {
        return expected.roomId() == actual.roomId()
                && expected.gameId() == actual.gameId()
                && expected.gameCode().equals(actual.gameCode())
                && expected.playVersion().equals(actual.playVersion())
                && expected.roomInstanceId().equals(actual.roomInstanceId())
                && expected.lifecycleEpoch() == actual.lifecycleEpoch()
                && expected.fencingToken() == actual.fencingToken();
    }

    private static void validateReadyCommand(ReadyCommand command) {
        RoomIdentity identity = command.expectedIdentity();
        RoomStaticIdentity staticIdentity = command.staticIdentity();
        if (identity.roomId() != staticIdentity.roomId()
                || identity.gameId() != staticIdentity.gameId()
                || !identity.gameCode().equals(staticIdentity.gameCode())
                || !identity.playVersion().equals(staticIdentity.playVersion())) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_STATIC_IDENTITY_CONFLICT");
        }
        if (command.permission() != ReadyPermission.SEATED_MEMBER) {
            throw new SecurityException("ROOM_ORCHESTRATOR_READY_PERMISSION_DENIED");
        }
        if (command.roomState() != com.aoo.bcg.gamespi.RoomState.WAITING) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_ROOM_STATE_REJECTED");
        }
        try {
            Math.addExact(identity.stateVersion(), 1L);
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_STATE_VERSION_OVERFLOW", overflow);
        }
    }

    private static void validateReadyResult(ReadyCommand command, ReadyResult result) {
        if (!sameGeneration(command.expectedIdentity(), result.identity())
                || result.identity().stateVersion() != command.expectedIdentity().stateVersion()
                || result.accountId() != command.accountId()
                || result.seatNo() != command.seatNo()
                || !result.commandId().equals(command.commandId())
                || result.expectedNextStateVersion() != command.expectedIdentity().stateVersion() + 1L) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_READY_IDENTITY_CONFLICT");
        }
    }

    private static void validateDissolveResult(RoomIdentity expected, String voteId,
                                               long expectedStateVersion, DissolveResult result,
                                               DissolveStatus expectedStatus, String commandId,
                                               String idempotencyKey, boolean advanceState) {
        if (!sameGeneration(expected, result.identity())
                || !voteId.equals(result.voteId())
                || result.status() != expectedStatus
                || (advanceState && result.identity().stateVersion() != Math.addExact(
                        expectedStateVersion, 1L))) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DISSOLVE_IDENTITY_CONFLICT");
        }
        if (expectedStatus != DissolveStatus.RECOVERED
                && (!commandId.equals(result.commandId()) || !idempotencyKey.equals(result.idempotencyKey()))) {
            throw new IllegalStateException("ROOM_ORCHESTRATOR_DISSOLVE_RECEIPT_CONFLICT");
        }
    }

    private static boolean sameReadyCommand(ReadyCommand expected, ReadyCommand actual) {
        return expected.accountId() == actual.accountId()
                && expected.seatNo() == actual.seatNo()
                && expected.commandId().equals(actual.commandId())
                && sameGeneration(expected.expectedIdentity(), actual.expectedIdentity())
                && expected.staticIdentity().equals(actual.staticIdentity())
                && expected.permission() == actual.permission()
                && expected.body().equals(actual.body());
    }

    private record ReadyReceipt(ReadyCommand command, ReadyResult result) { }
}
