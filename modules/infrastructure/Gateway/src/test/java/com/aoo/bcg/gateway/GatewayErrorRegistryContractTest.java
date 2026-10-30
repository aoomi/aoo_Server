package com.aoo.bcg.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayErrorRegistryContractTest {
    @Test void everyGatewayTransportCodeHasACanonicalRegistryEntry() {
        for (GatewayErrorCode code : GatewayErrorCode.values()) {
            assertNotNull(code.registryCode());
            assertEquals(code.registryCode(),
                    com.aoo.bcg.common.error.ErrorCodeCatalog.standard().requireSymbolic(code.registryCode()).code());
        }
    }

    @Test void theRoomStateNumberDoesNotDefineClubEmptyList() {
        var catalog = com.aoo.bcg.common.error.ErrorCodeCatalog.standard();
        assertEquals("ROOM_STATE_CONFLICT", catalog.canonicalSymbolicCode("GATEWAY_ROOM_STATE_CONFLICT"));
        assertEquals("CLUB_LIST_EMPTY", catalog.canonicalSymbolicCode("CLUB_LEGACY_3008_EMPTY_LIST"));
    }

    @Test void canonicalCategoriesMapToExistingNumericTransportSemantics() {
        assertEquals(GatewayErrorCode.ROUTE_NOT_FOUND, GatewayErrorCode.fromRegistryCode("IDENTITY_NOT_FOUND"));
        assertEquals(GatewayErrorCode.ROOM_AUTHORITY_UNAVAILABLE, GatewayErrorCode.fromRegistryCode("PROVIDER_MISSING"));
        assertEquals(GatewayErrorCode.ROOM_STATE_CONFLICT, GatewayErrorCode.fromRegistryCode("PRICING_POLICY_MISSING"));
        assertEquals(GatewayErrorCode.ROOM_STATE_CONFLICT, GatewayErrorCode.fromRegistryCode("SNAPSHOT_MISSING"));
        assertEquals(GatewayErrorCode.VERSION_INCOMPATIBLE, GatewayErrorCode.fromRegistryCode("VERSION_UNSUPPORTED"));
        assertEquals(GatewayErrorCode.INVALID_ENVELOPE, GatewayErrorCode.fromRegistryCode("INVALID_ENVELOPE"));
        assertEquals(GatewayErrorCode.ROOM_ADMIN_FORBIDDEN, GatewayErrorCode.fromRegistryCode("PERMISSION_DENIED"));
    }

    @Test void genericGatewayFailureDoesNotPretendToBeBackpressure() {
        assertEquals(GatewayErrorCode.ROOM_AUTHORITY_UNAVAILABLE,
                GatewayErrorCode.fromFailure(new IllegalStateException("INTERNAL_AUTHORITY_FAILED")));
    }

    @Test void websocketClassificationUsesTheSharedRegistryAndKeepsUnknownCodesFailClosed() {
        WebSocketFrame request = new WebSocketFrame("common.room.state_req", "error-1", 1,
                "100", 1, "v1", System.currentTimeMillis(), java.util.Map.of());
        assertEquals(GatewayErrorCode.ROOM_AUTHORITY_UNAVAILABLE,
                GatewayWebSocketFrameHandler.classifyFailure(
                        new IllegalStateException("HALL_GAME_PROVIDER_MISSING"), request));
        assertEquals(GatewayErrorCode.ROOM_STATE_CONFLICT,
                GatewayWebSocketFrameHandler.classifyFailure(
                        new IllegalStateException("516"), request));
    }
}
