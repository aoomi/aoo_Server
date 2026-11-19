package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract for the bounded Gateway -> RoomOrchestrator execute transport. */
final class JdbcGatewayRoomAuthorityExecuteTransportTest {
    @Test
    void currentGameplayBridgeBuildsTypedCommandAndKeepsCanonicalMetadataFailClosed() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("executeGameplayThroughRoomOrchestrator"));
        assertTrue(source.contains("new RoomOrchestrator.ExecuteCommand(expected, session.connectionId(), request)"));
        assertTrue(source.contains("executeThroughRoomOrchestrator("));
        assertTrue(source.contains("GameIdentityPolicy.requireCurrentPublished"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_EXECUTE_COMPATIBILITY_ORIGIN_REJECTED"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_EXECUTE_RUNTIME_IDENTITY_REQUIRED"));
        assertTrue(source.contains("requireCanonicalProjection"));
        assertTrue(source.contains("requireExecuteIdentity"));
        assertTrue(source.contains("executeIdempotency.acquire"));
        assertTrue(source.contains("stateStore.findCommitted"));
        assertTrue(source.contains("GameCommandResult committed = stateStore.findCommitted"));
        assertTrue(source.contains("return new RoomOrchestrator.ExecuteResult("));
        assertTrue(source.contains("stateStore.commitResult"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_EXECUTE_STATE_VERSION_CONFLICT"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_EXECUTE_PERMISSION_DENIED"));
    }

    @Test
    void providerWiresOnlyTheTypedExecuteAuthority() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertTrue(source.contains("configureExecuteAuthority(authority::executeGameplayThroughRoomOrchestrator)"));
    }
}
