package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused source contract for the bounded, transport-independent handoff authority. */
final class RoomOrchestratorHandoffAuthorityTest {
    @Test
    void handoffUsesCanonicalMetadataAndOneAuthorityLocalCompareAndSet() throws Exception {
        String source = source("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java");
        int start = source.indexOf("public RoomOrchestrator.HandoffResult handoff(");
        int end = source.indexOf("/**\n     * Registers the transport-owned handoff lease", start);
        assertTrue(start >= 0 && end > start, "handoff authority method must remain bounded");
        String handoff = source.substring(start, end);
        assertTrue(handoff.contains("requireCanonicalRoomLock(route)"));
        assertTrue(handoff.contains("requireHandoffIdentity(command, current, canonical.source(), provider)"));
        assertTrue(handoff.contains("handoffReceipts.computeIfAbsent"));
        assertTrue(handoff.contains("receipts.putIfAbsent(command.requestId(), inserted)"));
        assertTrue(handoff.contains("handoffLeases.put(roomId"));
        assertTrue(handoff.contains("ROOM_ORCHESTRATOR_HANDOFF_CAS_CONFLICT"));
        assertFalse(handoff.contains("GatewaySessionRegistry"));
        assertFalse(handoff.contains("RoomOwnershipRegistry"));
    }

    @Test
    void productionGatewayWiresTypedOwnerRegistrationWithoutStaticSuccess() throws Exception {
        String authority = source("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java");
        String provider = source("src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java");
        String gateway = source("../Gateway/src/main/java/com/aoo/bcg/gateway/GatewayWebSocketFrameHandler.java");
        assertTrue(provider.contains("HandoffOwnerRegistrar handoffOwnerRegistrar"));
        assertTrue(provider.contains("authority.registerHandoffOwner(expectedIdentity, ownerId"));
        assertTrue(provider.contains("ROOM_ORCHESTRATOR_HANDOFF_RUNTIME_IDENTITY_REQUIRED"));
        assertTrue(gateway.contains("handoffOwnerRegistrar.register(identity,resolved)"));
        assertTrue(authority.contains("prior.connectionGeneration()"));
        assertTrue(authority.contains("replacement.connectionGeneration() <= prior.connectionGeneration()"));
    }

    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
