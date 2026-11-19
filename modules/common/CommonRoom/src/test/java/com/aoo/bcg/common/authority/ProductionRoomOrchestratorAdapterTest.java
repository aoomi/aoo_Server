package com.aoo.bcg.common.authority;

import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gamespi.StatePayload;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionRoomOrchestratorAdapterTest {
    private static final RoomOrchestrator.RoomIdentity IDENTITY =
            new RoomOrchestrator.RoomIdentity(42, "instance-42", 2003, "liangshan2003",
                    "liangshan2003-v1", 7, 11, 3);
    private static final RoomSnapshotMetadataSource METADATA = new RoomSnapshotMetadataSource(
            19L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "card-v1", "events-v1", "protocol-v2", "snapshot-v1", 4L, "LS",
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");

    @Test
    void createAndRecoverPreserveTheSameGenerationAndStateVersion() {
        RoomOrchestrator.CreateResult created = new RoomOrchestrator.CreateResult(
                IDENTITY, state(3));
        RoomOrchestrator.RoomIdentity recoveredIdentity = new RoomOrchestrator.RoomIdentity(
                42, "instance-42", 2003, "liangshan2003", "liangshan2003-v1", 7, 11, 4);
        RoomOrchestrator.RecoverResult recovered = new RoomOrchestrator.RecoverResult(
                recoveredIdentity, state(4));
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return created;
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return recovered;
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(recoveredIdentity, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(recoveredIdentity, true, "DESTROYED");
            }
        });
        var context = new RoomCreationContext(42, 99, Map.of("playerCount", 3));
        var createCommand = new RoomOrchestrator.CreateCommand("create-42", context,
                "liangshan2003", "liangshan2003-v1", 11, "trace-42");
        assertEquals(created, adapter.create(createCommand));
        var recoverCommand = new RoomOrchestrator.RecoverCommand(IDENTITY, "session-42",
                "recover-42", state(3), 99);
        assertEquals(recovered, adapter.recover(recoverCommand));
    }

    @Test
    void executeUsesTheTypedAuthorityResultAndAdvancesExactlyOneGenerationRevision() {
        GameCommandRequest request = new GameCommandRequest(
                "msg-42", "request-42", 1, IDENTITY.roomId(), 0,
                IDENTITY.playVersion(), "17", 1, Map.of("action", "play"));
        GameCommandResult commandResult = new GameCommandResult(
                request.msgId(), request.requestId(), Map.of("accepted", true));
        RoomOrchestrator.RoomIdentity advanced = new RoomOrchestrator.RoomIdentity(
                IDENTITY.roomId(), IDENTITY.roomInstanceId(), IDENTITY.gameId(),
                IDENTITY.gameCode(), IDENTITY.playVersion(), IDENTITY.lifecycleEpoch(),
                IDENTITY.fencingToken(), IDENTITY.stateVersion() + 1);
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(IDENTITY.stateVersion()));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(IDENTITY.stateVersion()));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.ExecuteResult execute(RoomOrchestrator.ExecuteCommand command) {
                calls.incrementAndGet();
                assertEquals(IDENTITY, command.expectedIdentity());
                assertEquals("session-42", command.sessionId());
                assertEquals(request, command.request());
                return new RoomOrchestrator.ExecuteResult(advanced, commandResult);
            }
        });

        RoomOrchestrator.ExecuteResult result = adapter.execute(
                new RoomOrchestrator.ExecuteCommand(IDENTITY, "session-42", request));
        assertEquals(advanced, result.identity());
        assertEquals(commandResult, result.commandResult());
        assertEquals(1, calls.get());
    }

    @Test
    void executeRejectsAResultFromAnotherRoomGeneration() {
        GameCommandRequest request = new GameCommandRequest(
                "msg-43", "request-43", 1, IDENTITY.roomId(), 0,
                IDENTITY.playVersion(), "17", 1, Map.of());
        RoomOrchestrator.RoomIdentity wrongGeneration = new RoomOrchestrator.RoomIdentity(
                IDENTITY.roomId(), "other-instance", IDENTITY.gameId(), IDENTITY.gameCode(),
                IDENTITY.playVersion(), IDENTITY.lifecycleEpoch(), IDENTITY.fencingToken(),
                IDENTITY.stateVersion() + 1);
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(IDENTITY.stateVersion()));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(IDENTITY.stateVersion()));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.ExecuteResult execute(RoomOrchestrator.ExecuteCommand command) {
                return new RoomOrchestrator.ExecuteResult(wrongGeneration,
                        new GameCommandResult(request.msgId(), request.requestId(), Map.of()));
            }
        });
        assertThrows(IllegalStateException.class, () -> adapter.execute(
                new RoomOrchestrator.ExecuteCommand(IDENTITY, "session-43", request)));
    }

    @Test
    void reconnectReturnsOnlyAnEqualGenerationOrNewerStateSnapshot() {
        RoomOrchestrator.RoomIdentity current = new RoomOrchestrator.RoomIdentity(
                IDENTITY.roomId(), IDENTITY.roomInstanceId(), IDENTITY.gameId(), IDENTITY.gameCode(),
                IDENTITY.playVersion(), IDENTITY.lifecycleEpoch(), IDENTITY.fencingToken(), 4);
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(3));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.ReconnectResult reconnect(RoomOrchestrator.ReconnectCommand command) {
                calls.incrementAndGet();
                return new RoomOrchestrator.ReconnectResult(current, state(4));
            }
        });
        RoomOrchestrator.ReconnectCommand command = new RoomOrchestrator.ReconnectCommand(
                IDENTITY, "connection-42", "reconnect-42", 17L, 1, "token-42", 3L, METADATA);
        assertEquals(current, adapter.reconnect(command).identity());
        assertEquals(1, calls.get());
    }

    @Test
    void reconnectRejectsAnAuthoritySnapshotOlderThanTheObservedState() {
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(3));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.ReconnectResult reconnect(RoomOrchestrator.ReconnectCommand command) {
                return new RoomOrchestrator.ReconnectResult(IDENTITY, state(2));
            }
        });
        RoomOrchestrator.ReconnectCommand command = new RoomOrchestrator.ReconnectCommand(
                IDENTITY, "connection-42", "reconnect-43", 17L, 1, "token-42", 3L, METADATA);
        assertThrows(IllegalStateException.class, () -> adapter.reconnect(command));
    }

    @Test
    void handoffAcceptsOnlyTheAuthorityConfirmedTargetSessionAndGeneration() {
        RoomOrchestrator.HandoffCommand command = new RoomOrchestrator.HandoffCommand(
                IDENTITY, 17L, "connection-old", "connection-new", 7L, 8L,
                "handoff-42", IDENTITY.stateVersion(), METADATA);
        RoomOrchestrator.HandoffResult accepted = new RoomOrchestrator.HandoffResult(
                IDENTITY, "connection-new", 8L);
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand value) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(IDENTITY.stateVersion()));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand value) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(IDENTITY.stateVersion()));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity value) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand value) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, value.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, value.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand value) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.HandoffResult handoff(RoomOrchestrator.HandoffCommand value) {
                assertEquals(command, value);
                return accepted;
            }
        });

        assertEquals(accepted, adapter.handoff(command));
        var wrongTarget = new RoomOrchestrator.HandoffResult(IDENTITY, "connection-other", 8L);
        var wrongAdapter = new ProductionRoomOrchestratorAdapter(
                new ProductionRoomOrchestratorAdapter.AuthorityPort() {
                    @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand value) {
                        return new RoomOrchestrator.CreateResult(IDENTITY, state(IDENTITY.stateVersion()));
                    }
                    @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand value) {
                        return new RoomOrchestrator.RecoverResult(IDENTITY, state(IDENTITY.stateVersion()));
                    }
                    @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity value) { }
                    @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand value) {
                        return new RoomOrchestrator.LeaveResult(IDENTITY, value.accountId(),
                                RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, value.commandId());
                    }
                    @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand value) {
                        return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
                    }
                    @Override public RoomOrchestrator.HandoffResult handoff(RoomOrchestrator.HandoffCommand value) {
                        return wrongTarget;
                    }
                });
        assertThrows(IllegalStateException.class, () -> wrongAdapter.handoff(command));
    }

    @Test
    void rejectsMissingOrConflictingStateAndGeneration() {
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, StatePayload.empty());
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                RoomOrchestrator.RoomIdentity stale = new RoomOrchestrator.RoomIdentity(
                        42, "other", 2003, "liangshan2003", "liangshan2003-v1", 7, 11, 3);
                return new RoomOrchestrator.RecoverResult(stale, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "CAS_CONFLICT");
            }
        });
        var context = new RoomCreationContext(42, 99, Map.of());
        assertThrows(IllegalStateException.class, () -> adapter.create(
                new RoomOrchestrator.CreateCommand("create-42", context,
                        "liangshan2003", "liangshan2003-v1", 11, "trace-42")));
        assertThrows(IllegalStateException.class, () -> adapter.recover(
                new RoomOrchestrator.RecoverCommand(IDENTITY, "session-42", "recover-42",
                        state(3), 99)));
        assertThrows(IllegalStateException.class, () -> adapter.destroy(
                new RoomOrchestrator.DestroyCommand(IDENTITY, "session-42", "destroy-42", "TEST")));
    }

    @Test
    void destroyAllowsOnlyDestroyedOrIdempotentTombstoneAndOtherOperationsFailClosed() {
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(3));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
        });
        var destroy = adapter.destroy(new RoomOrchestrator.DestroyCommand(IDENTITY, "session-42",
                "destroy-42", "TEST"));
        assertEquals("ALREADY_DESTROYED", destroy.reason());
        assertThrows(IllegalStateException.class, () -> adapter.execute(null));
        assertThrows(IllegalStateException.class, () -> adapter.ready(readyCommand("ready-17")));
        assertThrows(IllegalStateException.class, () -> adapter.reconnect(null));
        assertThrows(IllegalStateException.class, () -> adapter.handoff(null));
    }

    @Test
    void destroyValidatesStaticIdentityBeforeCallingAuthority() {
        boolean[] validated = {false};
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(3));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) {
                assertEquals(IDENTITY, expectedIdentity);
                validated[0] = true;
            }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                assertTrue(validated[0]);
                return new RoomOrchestrator.DestroyResult(IDENTITY, true, "DESTROYED");
            }
        });
        assertEquals("DESTROYED", adapter.destroy(new RoomOrchestrator.DestroyCommand(IDENTITY,
                "session-42", "destroy-42", "TEST")).reason());
    }

    @Test
    void leaveCarriesTheFullIdentityAndAcceptsOnlyTypedMembershipOutcomes() {
        boolean[] called = {false};
        var adapter = new ProductionRoomOrchestratorAdapter(new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(3));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                assertEquals(IDENTITY, command.expectedIdentity());
                assertEquals(17L, command.accountId());
                assertEquals("leave-17", command.commandId());
                called[0] = true;
                return new RoomOrchestrator.LeaveResult(IDENTITY, 17L,
                        RoomOrchestrator.LeaveStatus.LEFT, 1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
        });
        RoomOrchestrator.LeaveResult result = adapter.leave(new RoomOrchestrator.LeaveCommand(
                IDENTITY, 17L, "leave-17", "trace-17"));
        assertTrue(called[0]);
        assertEquals(RoomOrchestrator.LeaveStatus.LEFT, result.status());
        assertEquals(IDENTITY, result.identity());
    }

    @Test
    void readyRejectsStaticIdentityPermissionAndRoomStateBeforeAuthorityCall() {
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls,
                command -> readyResult(command)));

        var mismatchedStatic = new RoomOrchestrator.RoomStaticIdentity(
                IDENTITY.roomId(), IDENTITY.gameId(), "wrong-code", IDENTITY.playVersion());
        assertThrows(IllegalStateException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                IDENTITY, mismatchedStatic, 17L, 1, RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                com.aoo.bcg.gamespi.RoomState.WAITING, "ready-17", body())));
        assertThrows(SecurityException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                IDENTITY, staticIdentity(), 17L, 1, RoomOrchestrator.ReadyPermission.DENIED,
                com.aoo.bcg.gamespi.RoomState.WAITING, "ready-17", body())));
        assertThrows(IllegalStateException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                IDENTITY, staticIdentity(), 17L, 1, RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                com.aoo.bcg.gamespi.RoomState.PLAYING, "ready-17", body())));
        assertEquals(0, calls.get());
    }

    @Test
    void readyReturnsTypedExpectationOnceAndRejectsConflictingReplay() {
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls,
                ProductionRoomOrchestratorAdapterTest::readyResult));

        var command = readyCommand("ready-17");
        var first = adapter.ready(command);
        var replay = adapter.ready(command);
        assertEquals(first, replay);
        assertEquals(RoomOrchestrator.ReadyStatus.ACCEPTED, first.status());
        assertEquals(IDENTITY.stateVersion() + 1L, first.expectedNextStateVersion());
        assertEquals(1, calls.get());

        var conflictingBody = new RoomOrchestrator.ReadyCommand(
                IDENTITY, staticIdentity(), 17L, 1, RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                com.aoo.bcg.gamespi.RoomState.WAITING, "ready-17",
                com.aoo.bcg.gamespi.CommandPayload.copyOf(Map.of("action", "different")));
        assertThrows(IllegalStateException.class, () -> adapter.ready(conflictingBody));
        assertThrows(IllegalStateException.class, () -> adapter.ready(readyCommand("ready-18")));
        assertEquals(1, calls.get());
    }

    @Test
    void readyRejectsWrongAuthorityGenerationWithoutCreatingReceipt() {
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls, command -> {
            RoomOrchestrator.RoomIdentity wrong = new RoomOrchestrator.RoomIdentity(
                    IDENTITY.roomId(), "other-instance", IDENTITY.gameId(), IDENTITY.gameCode(),
                    IDENTITY.playVersion(), IDENTITY.lifecycleEpoch(), IDENTITY.fencingToken(),
                    IDENTITY.stateVersion());
            return new RoomOrchestrator.ReadyResult(wrong, command.accountId(), command.seatNo(),
                    RoomOrchestrator.ReadyStatus.ACCEPTED, command.commandId(),
                    wrong.stateVersion() + 1L);
        }));
        assertThrows(IllegalStateException.class, () -> adapter.ready(readyCommand("ready-17")));
        assertEquals(1, calls.get());
    }

    @Test
    void readyAllowsLaterPlayersAndCommandsAndReplaysOriginalRequestAfterProgress() {
        AtomicInteger calls = new AtomicInteger();
        var currentVersion = new java.util.concurrent.atomic.AtomicLong(3L);
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls, command -> {
            if (command.expectedIdentity().stateVersion() != currentVersion.get()) {
                throw new IllegalStateException("test authority CAS conflict");
            }
            var result = readyResult(command);
            currentVersion.incrementAndGet();
            return result;
        }));
        var first = adapter.ready(readyAt("first", 17L, 1, 3L,
                com.aoo.bcg.gamespi.RoomState.WAITING));
        var second = adapter.ready(readyAt("second", 18L, 2, 4L,
                com.aoo.bcg.gamespi.RoomState.WAITING));
        var next = adapter.ready(readyAt("next-round", 17L, 1, 5L,
                com.aoo.bcg.gamespi.RoomState.WAITING));
        assertEquals(5L, second.expectedNextStateVersion());
        assertEquals(6L, next.expectedNextStateVersion());
        assertEquals(first, adapter.ready(readyAt("first", 17L, 1, 6L,
                com.aoo.bcg.gamespi.RoomState.WAITING)));
        assertEquals(3, calls.get());
    }

    @Test
    void readyReplaysLastAcceptedRequestAfterStateBecomesPlaying() {
        AtomicInteger calls = new AtomicInteger();
        var currentState = new java.util.concurrent.atomic.AtomicReference<>(
                com.aoo.bcg.gamespi.RoomState.WAITING);
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls, command -> {
            assertEquals(currentState.get(), command.roomState());
            var result = readyResult(command);
            currentState.set(com.aoo.bcg.gamespi.RoomState.PLAYING);
            return result;
        }));
        var first = adapter.ready(readyAt("last-ready", 17L, 1, 3L, currentState.get()));
        assertEquals(first, adapter.ready(readyAt("last-ready", 17L, 1, 4L, currentState.get())));
        assertEquals(1, calls.get());
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("new-playing-request", 18L, 2, 4L, currentState.get())));
        assertEquals(1, calls.get());
    }

    @Test
    void readyReplayRejectsDeniedChangedIdentityBodyAccountSeatAndOlderRevision() {
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls,
                ProductionRoomOrchestratorAdapterTest::readyResult));
        adapter.ready(readyAt("stable", 17L, 1, 3L, com.aoo.bcg.gamespi.RoomState.WAITING));
        var later = readyAt("stable", 17L, 1, 4L, com.aoo.bcg.gamespi.RoomState.PLAYING);
        assertThrows(SecurityException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                later.expectedIdentity(), staticIdentity(), 17L, 1,
                RoomOrchestrator.ReadyPermission.DENIED, later.roomState(), "stable", body())));
        var otherGeneration = new RoomOrchestrator.RoomIdentity(42, "different-instance", 2003,
                "liangshan2003", "liangshan2003-v1", 8, 12, 4);
        assertThrows(IllegalStateException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                otherGeneration, staticIdentity(), 17L, 1, RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                later.roomState(), "stable", body())));
        assertThrows(IllegalStateException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                later.expectedIdentity(), new RoomOrchestrator.RoomStaticIdentity(42, 2003,
                "wrong-game", "liangshan2003-v1"), 17L, 1, RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                later.roomState(), "stable", body())));
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("stable", 18L, 1, 4L, later.roomState())));
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("stable", 17L, 2, 4L, later.roomState())));
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("stable", 17L, 1, 2L, later.roomState())));
        assertThrows(IllegalStateException.class, () -> adapter.ready(new RoomOrchestrator.ReadyCommand(
                later.expectedIdentity(), staticIdentity(), 17L, 1,
                RoomOrchestrator.ReadyPermission.SEATED_MEMBER, later.roomState(), "stable",
                com.aoo.bcg.gamespi.CommandPayload.copyOf(Map.of("action", "different")))));
        assertEquals(1, calls.get());
    }

    @Test
    void readyRejectsConsumedVersionAndAuthorityRejectsFutureVersionWithoutCaching() {
        AtomicInteger calls = new AtomicInteger();
        var currentVersion = new java.util.concurrent.atomic.AtomicLong(3L);
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls, command -> {
            if (command.expectedIdentity().stateVersion() != currentVersion.get()) {
                throw new IllegalStateException("test authority CAS conflict");
            }
            var result = readyResult(command);
            currentVersion.incrementAndGet();
            return result;
        }));
        adapter.ready(readyAt("first", 17L, 1, 3L, com.aoo.bcg.gamespi.RoomState.WAITING));
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("same-revision", 18L, 2, 3L, com.aoo.bcg.gamespi.RoomState.WAITING)));
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("older-revision", 18L, 2, 2L, com.aoo.bcg.gamespi.RoomState.WAITING)));
        assertEquals(1, calls.get());
        assertThrows(IllegalStateException.class, () -> adapter.ready(
                readyAt("future", 18L, 2, 5L, com.aoo.bcg.gamespi.RoomState.WAITING)));
        var result = adapter.ready(readyAt("future", 18L, 2, 4L,
                com.aoo.bcg.gamespi.RoomState.WAITING));
        assertEquals(5L, result.expectedNextStateVersion());
        assertEquals(3, calls.get());
    }

    @Test
    void readyFailureAndWrongAuthorityReceiptNeverEnterTheLedger() {
        AtomicInteger calls = new AtomicInteger();
        var adapter = new ProductionRoomOrchestratorAdapter(readyAuthority(calls, command -> {
            if (calls.get() == 1) throw new IllegalStateException("test authority unavailable");
            if (calls.get() == 2) {
                return new RoomOrchestrator.ReadyResult(command.expectedIdentity(), 18L,
                        command.seatNo(), RoomOrchestrator.ReadyStatus.ACCEPTED,
                        command.commandId(), command.expectedIdentity().stateVersion() + 1L);
            }
            return readyResult(command);
        }));
        var command = readyAt("retry", 17L, 1, 3L, com.aoo.bcg.gamespi.RoomState.WAITING);
        assertThrows(IllegalStateException.class, () -> adapter.ready(command));
        assertThrows(IllegalStateException.class, () -> adapter.ready(command));
        var accepted = adapter.ready(command);
        assertEquals(accepted, adapter.ready(readyAt("retry", 17L, 1, 4L,
                com.aoo.bcg.gamespi.RoomState.PLAYING)));
        assertEquals(3, calls.get());
    }

    private static RoomOrchestrator.ReadyCommand readyAt(String commandId, long accountId,
            int seatNo, long stateVersion, com.aoo.bcg.gamespi.RoomState roomState) {
        var identity = new RoomOrchestrator.RoomIdentity(IDENTITY.roomId(), IDENTITY.roomInstanceId(),
                IDENTITY.gameId(), IDENTITY.gameCode(), IDENTITY.playVersion(),
                IDENTITY.lifecycleEpoch(), IDENTITY.fencingToken(), stateVersion);
        return new RoomOrchestrator.ReadyCommand(identity, staticIdentity(), accountId, seatNo,
                RoomOrchestrator.ReadyPermission.SEATED_MEMBER, roomState, commandId, body());
    }

    private static ProductionRoomOrchestratorAdapter.AuthorityPort readyAuthority(
            AtomicInteger calls, Function<RoomOrchestrator.ReadyCommand, RoomOrchestrator.ReadyResult> ready) {
        return new ProductionRoomOrchestratorAdapter.AuthorityPort() {
            @Override public RoomOrchestrator.CreateResult create(RoomOrchestrator.CreateCommand command) {
                return new RoomOrchestrator.CreateResult(IDENTITY, state(3));
            }
            @Override public RoomOrchestrator.RecoverResult recover(RoomOrchestrator.RecoverCommand command) {
                return new RoomOrchestrator.RecoverResult(IDENTITY, state(3));
            }
            @Override public void validateDestroyIdentity(RoomOrchestrator.RoomIdentity expectedIdentity) { }
            @Override public RoomOrchestrator.LeaveResult leave(RoomOrchestrator.LeaveCommand command) {
                return new RoomOrchestrator.LeaveResult(IDENTITY, command.accountId(),
                        RoomOrchestrator.LeaveStatus.ALREADY_LEFT, -1, command.commandId());
            }
            @Override public RoomOrchestrator.DestroyResult destroy(RoomOrchestrator.DestroyCommand command) {
                return new RoomOrchestrator.DestroyResult(IDENTITY, false, "ALREADY_DESTROYED");
            }
            @Override public RoomOrchestrator.ReadyResult ready(RoomOrchestrator.ReadyCommand command) {
                calls.incrementAndGet();
                return ready.apply(command);
            }
        };
    }

    private static RoomOrchestrator.ReadyResult readyResult(RoomOrchestrator.ReadyCommand command) {
        return new RoomOrchestrator.ReadyResult(command.expectedIdentity(), command.accountId(),
                command.seatNo(), RoomOrchestrator.ReadyStatus.ACCEPTED, command.commandId(),
                command.expectedIdentity().stateVersion() + 1L);
    }

    private static RoomOrchestrator.ReadyCommand readyCommand(String commandId) {
        return new RoomOrchestrator.ReadyCommand(IDENTITY, staticIdentity(), 17L, 1,
                RoomOrchestrator.ReadyPermission.SEATED_MEMBER,
                com.aoo.bcg.gamespi.RoomState.WAITING, commandId, body());
    }

    private static RoomOrchestrator.RoomStaticIdentity staticIdentity() {
        return new RoomOrchestrator.RoomStaticIdentity(IDENTITY.roomId(), IDENTITY.gameId(),
                IDENTITY.gameCode(), IDENTITY.playVersion());
    }

    private static com.aoo.bcg.gamespi.CommandPayload body() {
        return com.aoo.bcg.gamespi.CommandPayload.copyOf(Map.of("action", "ready"));
    }

    private static StatePayload state(long stateVersion) {
        return StatePayload.copyOf(Map.of("stateVersion", stateVersion));
    }
}
