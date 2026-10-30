package com.aoo.bcg.gamespi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class RoomOrchestratorContractTest {
    private static final RoomSnapshotMetadataSource METADATA = new RoomSnapshotMetadataSource(
            19L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "card-v1", "events-v1", "protocol-v2", "snapshot-v1", 4L, "CN",
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");

    @Test
    void exposesOnlyThePhaseAAuthorityLifecycleSurface() throws NoSuchMethodException {
        Set<String> lifecycle = Set.of("create", "execute", "ready", "reconnect", "recover", "handoff", "leave", "destroy",
                "dissolveApply", "dissolveVote", "dissolveRecover");
        for (String methodName : lifecycle) {
            Method method = RoomOrchestrator.class.getMethod(methodName,
                    switch (methodName) {
                        case "create" -> RoomOrchestrator.CreateCommand.class;
                        case "execute" -> RoomOrchestrator.ExecuteCommand.class;
                        case "ready" -> RoomOrchestrator.ReadyCommand.class;
                        case "reconnect" -> RoomOrchestrator.ReconnectCommand.class;
                        case "recover" -> RoomOrchestrator.RecoverCommand.class;
                        case "handoff" -> RoomOrchestrator.HandoffCommand.class;
                        case "leave" -> RoomOrchestrator.LeaveCommand.class;
                        case "destroy" -> RoomOrchestrator.DestroyCommand.class;
                        case "dissolveApply" -> RoomOrchestrator.DissolveApplyCommand.class;
                        case "dissolveVote" -> RoomOrchestrator.DissolveVoteCommand.class;
                        case "dissolveRecover" -> RoomOrchestrator.DissolveRecoverCommand.class;
                        default -> throw new AssertionError(methodName);
                    });
            assertTrue(Modifier.isAbstract(method.getModifiers()), methodName + " must remain an unimplemented boundary");
            assertTrue(RoomOrchestrator.AuthorityResult.class.isAssignableFrom(method.getReturnType()),
                    methodName + " must return fenced authority metadata");
        }
        Set<String> abstractMethods = Arrays.stream(RoomOrchestrator.class.getDeclaredMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(lifecycle, abstractMethods);
    }

    @Test
    void resultCarriesRoomIdentityFenceAndStateVersionWithoutRuntimeWiring() {
        var identity = new RoomOrchestrator.RoomIdentity(
                1001L, "room-instance-1", 1001, "chengdu1001", "chengdu1001-v1.0.0",
                1L, 7L, 3L);
        var result = new RoomOrchestrator.HandoffResult(identity, "session-2", 8L);

        assertNotNull(result.identity());
        assertEquals(7L, result.identity().fencingToken());
        assertEquals(3L, result.identity().stateVersion());
        assertEquals("room-instance-1", result.identity().roomInstanceId());
        assertEquals("session-2", result.sessionId());
        assertEquals(8L, result.connectionGeneration());
    }

    @Test
    void handoffCommandCarriesOwnerGenerationsAndCanonicalMetadata() {
        var identity = new RoomOrchestrator.RoomIdentity(
                1001L, "room-instance-1", 1001, "chengdu1001", "chengdu1001-v1.0.0",
                1L, 7L, 3L);
        var command = new RoomOrchestrator.HandoffCommand(identity, 77L, "session-1", "session-2",
                7L, 8L, "handoff-77", 3L, METADATA);

        assertEquals(77L, command.ownerId());
        assertEquals(7L, command.fromConnectionGeneration());
        assertEquals(8L, command.toConnectionGeneration());
        assertEquals(METADATA, command.metadata());
        assertThrows(IllegalArgumentException.class, () -> new RoomOrchestrator.HandoffCommand(
                identity, 77L, "session-1", "session-2", 8L, 8L,
                "handoff-conflict", 3L, METADATA));
    }

    @Test
    void readyContractRequiresCompleteStaticAndRuntimeIdentity() {
        var identity = new RoomOrchestrator.RoomIdentity(
                1001L, "room-instance-1", 1001, "chengdu1001", "chengdu1001-v1.0.0",
                4L, 8L, 12L);
        var staticIdentity = new RoomOrchestrator.RoomStaticIdentity(
                1001L, 1001, "chengdu1001", "chengdu1001-v1.0.0");
        var command = new RoomOrchestrator.ReadyCommand(identity, staticIdentity, 77L, 2,
                RoomOrchestrator.ReadyPermission.SEATED_MEMBER, RoomState.WAITING,
                "ready-77", CommandPayload.empty());
        var result = new RoomOrchestrator.ReadyResult(identity, 77L, 2,
                RoomOrchestrator.ReadyStatus.ACCEPTED, "ready-77", 13L);

        assertEquals(identity, command.expectedIdentity());
        assertEquals(staticIdentity, command.staticIdentity());
        assertEquals(13L, result.expectedNextStateVersion());
    }
}
