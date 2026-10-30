package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contract for the bounded create/recover/leave/destroy adapter wiring. */
final class RoomOrchestratorAuthorityAdapterTest {
    @Test
    void gatewayCreateAndRecoverEnterOnlyThroughTheProductionAdapter() throws Exception {
        String source = source();
        assertTrue(source.contains("ProductionRoomOrchestratorAdapter.AuthorityPort"));
        assertTrue(source.contains("private final ProductionRoomOrchestratorAdapter roomOrchestrator"));
        assertTrue(source.contains("roomOrchestrator.create(toCreateCommand(command))"));
        assertTrue(source.contains("roomOrchestrator.recover(toRecoverCommand(command))"));
        assertTrue(source.contains("createGateway(createCommandMap(command))"));
        assertTrue(source.contains("recoverGateway(recoverCommandMap(command))"));
        assertTrue(source.contains("roomIdentityStore.require(route.roomId())"));
        assertTrue(source.contains("requireActiveRuntimeIdentity(room)"));
        assertTrue(source.contains("roomOrchestrator.leave(toLeaveCommand(command))"));
        assertTrue(source.contains("roomOrchestrator.execute(command)"));
        assertTrue(source.contains("executeThroughRoomOrchestrator"));
        assertTrue(source.contains("executeIdempotency.acquire"));
        assertTrue(source.contains("stateStore.commitResult"));
        assertTrue(source.contains("requireExecuteIdentity"));
        assertTrue(source.contains("requireExecutePermission"));
        assertTrue(source.contains("new RoomOrchestrator.LeaveCommand(identity, accountId"));
        assertTrue(source.contains("requireLeaveIdentityFields(command)"));
        assertTrue(source.contains("requireLeaveStaticIdentity(command, identity)"));
        assertTrue(source.contains("RoomOrchestrator.LeaveStatus.ALREADY_LEFT"));
        assertTrue(source.contains("leaveReceipts"));
    }

    @Test
    void typedDestroyUsesTheExpectedFullIdentityAndCannotFallBackToLegacyRemoval() throws Exception {
        String source = source();
        assertTrue(source.contains("destroy(command.expectedIdentity().roomId()"));
        assertTrue(source.contains("new RoomRuntimeIdentity(command.expectedIdentity().roomInstanceId()"));
        assertTrue(source.contains("validateDestroyIdentity(command.expectedIdentity())"));
        assertTrue(source.contains("roomOrchestrator.destroy(command)"));
        assertTrue(source.contains("roomMetadata.requireProjection(route.roomId())"));
        assertTrue(source.contains("requireDestroyCanonical(route)"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_DESTROY_CAS_CONFLICT_STATIC_IDENTITY"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_DESTROY_"));
        assertTrue(source.contains("roomIdentityStore.destroy(roomId, stored.identity())"));
        assertTrue(source.contains("rooms.remove(roomId)"));
        assertTrue(source.contains("transition(roomId, fencingToken, \"REMOVED\")"));
    }

    @Test
    void providerStillUsesOneDurableIdentityStoreAndCarriesRecoverySessionContext() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertEquals(1, occurrences(source, "new RoomRuntimeIdentityStore(source)"));
        assertTrue(source.contains("hallLifecycle, idempotencyRetention, roomMetadata, roomIdentityStore"));
        assertTrue(source.contains("value.put(\"sessionId\",command.requestId())"));
    }

    @Test
    void providerLeaveHasOneOrchestratorBridgeAndDoesNotCallRawAuthorityDirectly() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertTrue(source.contains("authority.leaveThroughRoomOrchestrator(command)"));
        assertTrue(source.contains("managedAuthority.leave(Map.copyOf(value))"));
        assertTrue(source.contains("RoomMembershipAuthorityPort"));
        assertTrue(!source.contains("authority.leave(command)"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }
}
