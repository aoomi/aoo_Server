package com.aoo.bcg.gamespi;

import java.util.Objects;
import java.util.Map;
import java.util.Set;

/**
 * Passive authority boundary for the room lifecycle.
 *
 * <p>The production adapter currently owns create, recover, leave and destroy only. The ready contract is
 * deliberately present as a fail-closed preparation boundary until a separately verified Provider wiring
 * batch supplies the same room authority. An implementation must reject stale or fenced commands rather
 * than delegate to a second room authority.</p>
 */
public interface RoomOrchestrator {
    CreateResult create(CreateCommand command);

    ExecuteResult execute(ExecuteCommand command);

    /** Public ready mutation boundary; production wiring remains fail-closed until a later batch. */
    ReadyResult ready(ReadyCommand command);

    ReconnectResult reconnect(ReconnectCommand command);

    RecoverResult recover(RecoverCommand command);

    HandoffResult handoff(HandoffCommand command);

    /** Membership leave is a fenced authority operation, not a gameplay execute fallback. */
    LeaveResult leave(LeaveCommand command);

    /** Starts a durable dissolve vote; transport wiring is intentionally a later batch. */
    DissolveResult dissolveApply(DissolveApplyCommand command);

    /** Records one durable vote against the exact room generation and state version. */
    DissolveResult dissolveVote(DissolveVoteCommand command);

    /** Reads the durable aggregate/receipt/tombstone without using process-local state. */
    DissolveResult dissolveRecover(DissolveRecoverCommand command);

    DestroyResult destroy(DestroyCommand command);

    /** Stable room identity plus the authority fence and state revision returned by an operation. */
    record RoomIdentity(long roomId, String roomInstanceId, int gameId, String gameCode,
                        String playVersion, long lifecycleEpoch, long fencingToken,
                        long stateVersion) {
        public RoomIdentity {
            if (roomId <= 0 || roomInstanceId == null || roomInstanceId.isBlank()
                    || gameId <= 0 || gameCode == null || gameCode.isBlank()
                    || playVersion == null || playVersion.isBlank() || lifecycleEpoch <= 0
                    || fencingToken <= 0 || stateVersion < 0) {
                throw new IllegalArgumentException("invalid room authority identity");
            }
        }
    }

    /** Request to create a room; the authority assigns the room instance and fencing values. */
    record CreateCommand(String requestId, RoomCreationContext context, String gameCode,
                         String playVersion, long fencingToken, String traceId) {
        /** Source-compatible constructor; production authority rejects missing hints. */
        public CreateCommand(String requestId, RoomCreationContext context, String gameCode,
                             String playVersion) {
            this(requestId, context, gameCode, playVersion, 0L, null);
        }

        public CreateCommand {
            requireText(requestId, "requestId");
            Objects.requireNonNull(context, "context");
            requireText(gameCode, "gameCode");
            requireText(playVersion, "playVersion");
            if (fencingToken < 0) throw new IllegalArgumentException("invalid fencingToken");
        }

        public long requireFencingToken() {
            if (fencingToken <= 0) throw new IllegalStateException("ROOM_ORCHESTRATOR_FENCE_REQUIRED");
            return fencingToken;
        }

        public String requireTraceId() {
            if (traceId == null || traceId.isBlank()) {
                throw new IllegalStateException("ROOM_ORCHESTRATOR_TRACE_REQUIRED");
            }
            return traceId;
        }
    }

    /** Command execution is fenced by the identity observed by the caller. */
    record ExecuteCommand(RoomIdentity expectedIdentity, String sessionId,
                          GameCommandRequest request) {
        public ExecuteCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            requireText(sessionId, "sessionId");
            Objects.requireNonNull(request, "request");
            if (request.roomId() != expectedIdentity.roomId()) {
                throw new IllegalArgumentException("command room does not match authority identity");
            }
        }
    }

    /** Static identity supplied alongside the runtime identity and checked for exact agreement. */
    record RoomStaticIdentity(long roomId, int gameId, String gameCode, String playVersion) {
        public RoomStaticIdentity {
            if (roomId <= 0 || gameId <= 0 || gameCode == null || gameCode.isBlank()
                    || playVersion == null || playVersion.isBlank()) {
                throw new IllegalArgumentException("invalid room static identity");
            }
        }
    }

    /** Typed ready command; an explicit empty body is valid, a missing body is not. */
    record ReadyCommand(RoomIdentity expectedIdentity, RoomStaticIdentity staticIdentity,
                        long accountId, int seatNo, ReadyPermission permission,
                        RoomState roomState, String commandId, CommandPayload body) {
        public ReadyCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            Objects.requireNonNull(staticIdentity, "staticIdentity");
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(roomState, "roomState");
            Objects.requireNonNull(body, "body");
            if (accountId <= 0 || seatNo < 0) throw new IllegalArgumentException("invalid ready player");
            requireText(commandId, "commandId");
        }
    }

    enum ReadyPermission { SEATED_MEMBER, DENIED }

    enum ReadyStatus { ACCEPTED, ALREADY_READY }

    /** Typed ready outcome. The next state version is an expectation, not a committed mutation. */
    record ReadyResult(RoomIdentity identity, long accountId, int seatNo, ReadyStatus status,
                       String commandId, long expectedNextStateVersion) implements AuthorityResult {
        public ReadyResult {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(status, "status");
            if (accountId <= 0 || seatNo < 0) throw new IllegalArgumentException("invalid ready result");
            requireText(commandId, "commandId");
            long expected;
            try {
                expected = Math.addExact(identity.stateVersion(), 1L);
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("ready state version overflow", overflow);
            }
            if (expectedNextStateVersion != expected) {
                throw new IllegalArgumentException("ready next state version must advance exactly once");
            }
        }
    }

    /** Reconnect request carrying the authenticated viewer and the complete locked metadata projection. */
    record ReconnectCommand(RoomIdentity expectedIdentity, String sessionId, String requestId,
                            long accountId, int seatNo, String reconnectToken,
                            long lastKnownStateVersion, RoomSnapshotMetadataSource metadata,
                            long eventSequence, String eventId) {
        /** Source-compatible constructor for callers that predate the event cursor contract. */
        public ReconnectCommand(RoomIdentity expectedIdentity, String sessionId, String requestId,
                                long accountId, int seatNo, String reconnectToken,
                                long lastKnownStateVersion, RoomSnapshotMetadataSource metadata) {
            this(expectedIdentity, sessionId, requestId, accountId, seatNo, reconnectToken,
                    lastKnownStateVersion, metadata, lastKnownStateVersion, requestId);
        }

        public ReconnectCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            requireText(sessionId, "sessionId");
            requireText(requestId, "requestId");
            requireText(reconnectToken, "reconnectToken");
            Objects.requireNonNull(metadata, "metadata");
            if (accountId <= 0 || seatNo < 0 || lastKnownStateVersion < 0) {
                throw new IllegalArgumentException("invalid last known state version");
            }
            if (eventSequence < 0) throw new IllegalArgumentException("invalid event sequence");
            requireText(eventId, "eventId");
        }
    }

    /** Recovery request carrying an authoritative snapshot; implementations must validate its identity. */
    record RecoverCommand(RoomIdentity expectedIdentity, String sessionId, String requestId,
                          StatePayload snapshot, long ownerId) {
        /** Source-compatible constructor; production recovery rejects missing owner identity. */
        public RecoverCommand(RoomIdentity expectedIdentity, String sessionId, String requestId,
                              StatePayload snapshot) {
            this(expectedIdentity, sessionId, requestId, snapshot, 0L);
        }

        public RecoverCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            requireText(sessionId, "sessionId");
            requireText(requestId, "requestId");
            Objects.requireNonNull(snapshot, "snapshot");
            if (ownerId < 0) throw new IllegalArgumentException("invalid ownerId");
        }

        public long requireOwnerId() {
            if (ownerId <= 0) throw new IllegalStateException("ROOM_ORCHESTRATOR_OWNER_REQUIRED");
            return ownerId;
        }
    }

    /** Handoff is a fenced session transfer and never a second room mount. */
    record HandoffCommand(RoomIdentity expectedIdentity, long ownerId, String fromSessionId,
                          String toSessionId, long fromConnectionGeneration,
                          long toConnectionGeneration, String requestId,
                          long expectedStateVersion, RoomSnapshotMetadataSource metadata,
                          long eventSequence, String eventId) {
        /** Source-compatible constructor for callers that predate the event cursor contract. */
        public HandoffCommand(RoomIdentity expectedIdentity, long ownerId, String fromSessionId,
                              String toSessionId, long fromConnectionGeneration,
                              long toConnectionGeneration, String requestId,
                              long expectedStateVersion, RoomSnapshotMetadataSource metadata) {
            this(expectedIdentity, ownerId, fromSessionId, toSessionId, fromConnectionGeneration,
                    toConnectionGeneration, requestId, expectedStateVersion, metadata,
                    expectedStateVersion, requestId);
        }

        public HandoffCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            Objects.requireNonNull(metadata, "metadata");
            if (ownerId <= 0 || fromConnectionGeneration <= 0 || toConnectionGeneration <= 0) {
                throw new IllegalArgumentException("invalid handoff authority identity");
            }
            requireText(fromSessionId, "fromSessionId");
            requireText(toSessionId, "toSessionId");
            requireText(requestId, "requestId");
            if (fromSessionId.equals(toSessionId) || toConnectionGeneration <= fromConnectionGeneration
                    || expectedStateVersion < 0) {
                throw new IllegalArgumentException("invalid handoff command");
            }
            if (eventSequence < 0) throw new IllegalArgumentException("invalid event sequence");
            requireText(eventId, "eventId");
        }
    }

    /** Destruction is also fenced so a late old-session request cannot destroy a newer room instance. */
    record DestroyCommand(RoomIdentity expectedIdentity, String sessionId, String requestId,
                          String reason) {
        public DestroyCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            requireText(sessionId, "sessionId");
            requireText(requestId, "requestId");
            requireText(reason, "reason");
        }
    }

    /** A single member leave command carrying the complete room generation and idempotency key. */
    record LeaveCommand(RoomIdentity expectedIdentity, long accountId, String commandId,
                        String traceId, long eventSequence, String eventId) {
        /** Source-compatible constructor for callers that predate the event cursor contract. */
        public LeaveCommand(RoomIdentity expectedIdentity, long accountId, String commandId,
                            String traceId) {
            this(expectedIdentity, accountId, commandId, traceId,
                    expectedIdentity == null ? 0L : expectedIdentity.stateVersion(), commandId);
        }

        public LeaveCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            if (accountId <= 0) throw new IllegalArgumentException("invalid accountId");
            requireText(commandId, "commandId");
            requireText(traceId, "traceId");
            if (eventSequence < 0) throw new IllegalArgumentException("invalid event sequence");
            requireText(eventId, "eventId");
        }
    }

    enum LeaveStatus { LEFT, ALREADY_LEFT }

    /** Typed membership result; no room identity is removed or gameplay transition is implied. */
    record LeaveResult(RoomIdentity identity, long accountId, LeaveStatus status, int seatNo,
                       String commandId, long eventSequence, String eventId) implements AuthorityResult {
        /** Source-compatible constructor for authorities that predate event-cursor results. */
        public LeaveResult(RoomIdentity identity, long accountId, LeaveStatus status, int seatNo,
                           String commandId) {
            this(identity, accountId, status, seatNo, commandId,
                    identity == null ? 0L : identity.stateVersion(), commandId);
        }

        public LeaveResult {
            Objects.requireNonNull(identity, "identity");
            if (accountId <= 0) throw new IllegalArgumentException("invalid accountId");
            Objects.requireNonNull(status, "status");
            if (seatNo < -1) throw new IllegalArgumentException("invalid seatNo");
            requireText(commandId, "commandId");
            if (eventSequence < 0) throw new IllegalArgumentException("invalid event sequence");
            requireText(eventId, "eventId");
        }
    }

    enum DissolvePermission { APPLICANT, ELIGIBLE_MEMBER, DENIED }

    enum DissolveStatus { APPLIED, VOTED, RECOVERED }

    enum DissolveDecision { VOTING, APPROVED, REJECTED }

    /** Complete durable input for creating one dissolve generation. */
    record DissolveApplyCommand(RoomIdentity expectedIdentity, RoomStaticIdentity staticIdentity,
                                String sessionId, long applicantAccountId,
                                DissolvePermission permission, RoomState roomState,
                                String voteId, Set<Long> eligibleMemberIds,
                                java.time.Instant deadline, boolean approveOnTimeout,
                                boolean applicantAgrees, long expectedStateVersion,
                                String commandId, String idempotencyKey,
                                RoomSnapshotMetadataSource metadata) {
        public DissolveApplyCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            Objects.requireNonNull(staticIdentity, "staticIdentity");
            requireText(sessionId, "sessionId");
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(roomState, "roomState");
            requireText(voteId, "voteId");
            Objects.requireNonNull(eligibleMemberIds, "eligibleMemberIds");
            eligibleMemberIds = normalizeMembers(eligibleMemberIds);
            if (applicantAccountId <= 0 || !eligibleMemberIds.contains(applicantAccountId)) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_APPLICANT_NOT_ELIGIBLE");
            }
            Objects.requireNonNull(deadline, "deadline");
            if (expectedStateVersion < 0
                    || expectedStateVersion != expectedIdentity.stateVersion()) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_EXPECTED_STATE_VERSION_INVALID");
            }
            requireText(commandId, "commandId");
            requireText(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(metadata, "metadata");
            requireStaticIdentity(expectedIdentity, staticIdentity);
        }
    }

    /** Complete durable input for one eligible member vote. */
    record DissolveVoteCommand(RoomIdentity expectedIdentity, RoomStaticIdentity staticIdentity,
                               String sessionId, long voterAccountId,
                               DissolvePermission permission, RoomState roomState,
                               String voteId, boolean agree, long expectedStateVersion,
                               String commandId, String idempotencyKey,
                               RoomSnapshotMetadataSource metadata) {
        public DissolveVoteCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            Objects.requireNonNull(staticIdentity, "staticIdentity");
            requireText(sessionId, "sessionId");
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(roomState, "roomState");
            requireText(voteId, "voteId");
            if (voterAccountId <= 0 || expectedStateVersion < 0
                    || expectedStateVersion != expectedIdentity.stateVersion()) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_VOTE_INPUT_INVALID");
            }
            requireText(commandId, "commandId");
            requireText(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(metadata, "metadata");
            requireStaticIdentity(expectedIdentity, staticIdentity);
        }
    }

    /** Read-only recovery of the durable aggregate for the exact static/runtime identity. */
    record DissolveRecoverCommand(RoomIdentity expectedIdentity, RoomStaticIdentity staticIdentity,
                                  String voteId, RoomSnapshotMetadataSource metadata) {
        public DissolveRecoverCommand {
            Objects.requireNonNull(expectedIdentity, "expectedIdentity");
            Objects.requireNonNull(staticIdentity, "staticIdentity");
            requireText(voteId, "voteId");
            Objects.requireNonNull(metadata, "metadata");
            requireStaticIdentity(expectedIdentity, staticIdentity);
        }
    }

    /** Durable dissolve result; command receipts replay this exact projection. */
    record DissolveResult(RoomIdentity identity, String voteId, long applicantAccountId,
                          Set<Long> eligibleMemberIds, java.time.Instant deadline,
                          boolean approveOnTimeout, Map<Long, Boolean> votes,
                          DissolveDecision decision, boolean terminalTombstone,
                          DissolveStatus status, String commandId, String idempotencyKey)
            implements AuthorityResult {
        public DissolveResult {
            Objects.requireNonNull(identity, "identity");
            requireText(voteId, "voteId");
            if (applicantAccountId <= 0) throw new IllegalArgumentException("invalid applicant");
            Objects.requireNonNull(eligibleMemberIds, "eligibleMemberIds");
            eligibleMemberIds = normalizeMembers(eligibleMemberIds);
            if (!eligibleMemberIds.contains(applicantAccountId)) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_RESULT_APPLICANT_NOT_ELIGIBLE");
            }
            Objects.requireNonNull(deadline, "deadline");
            Objects.requireNonNull(votes, "votes");
            votes = Map.copyOf(votes);
            if (!eligibleMemberIds.containsAll(votes.keySet())) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_RESULT_INELIGIBLE_VOTE");
            }
            Objects.requireNonNull(decision, "decision");
            Objects.requireNonNull(status, "status");
            if (terminalTombstone != (decision != DissolveDecision.VOTING)) {
                throw new IllegalArgumentException("ROOM_DISSOLVE_TERMINAL_DECISION_CONFLICT");
            }
            if (status == DissolveStatus.RECOVERED) {
                if (commandId != null || idempotencyKey != null) {
                    throw new IllegalArgumentException("ROOM_DISSOLVE_RECOVERY_RECEIPT_CONFLICT");
                }
            } else {
                requireText(commandId, "commandId");
                requireText(idempotencyKey, "idempotencyKey");
            }
        }
    }

    interface AuthorityResult {
        RoomIdentity identity();
    }

    record CreateResult(RoomIdentity identity, StatePayload state) implements AuthorityResult {
        public CreateResult {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(state, "state");
        }
    }

    record ExecuteResult(RoomIdentity identity, GameCommandResult commandResult)
            implements AuthorityResult {
        public ExecuteResult {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(commandResult, "commandResult");
        }
    }

    record ReconnectResult(RoomIdentity identity, StatePayload state) implements AuthorityResult {
        public ReconnectResult {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(state, "state");
        }
    }

    record RecoverResult(RoomIdentity identity, StatePayload state) implements AuthorityResult {
        public RecoverResult {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(state, "state");
        }
    }

    record HandoffResult(RoomIdentity identity, String sessionId, long connectionGeneration)
            implements AuthorityResult {
        public HandoffResult {
            Objects.requireNonNull(identity, "identity");
            requireText(sessionId, "sessionId");
            if (connectionGeneration <= 0) {
                throw new IllegalArgumentException("invalid handoff connection generation");
            }
        }
    }

    record DestroyResult(RoomIdentity identity, boolean destroyed, String reason)
            implements AuthorityResult {
        public DestroyResult {
            Objects.requireNonNull(identity, "identity");
            requireText(reason, "reason");
        }
    }

    private static void requireStaticIdentity(RoomIdentity runtime, RoomStaticIdentity statik) {
        if (runtime.roomId() != statik.roomId() || runtime.gameId() != statik.gameId()
                || !runtime.gameCode().equals(statik.gameCode())
                || !runtime.playVersion().equals(statik.playVersion())) {
            throw new IllegalArgumentException("ROOM_DISSOLVE_STATIC_IDENTITY_CONFLICT");
        }
    }

    private static Set<Long> normalizeMembers(Set<Long> members) {
        if (members.isEmpty() || members.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException("ROOM_DISSOLVE_ELIGIBLE_MEMBERS_INVALID");
        }
        return Set.copyOf(members);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("invalid " + field);
        }
    }
}
