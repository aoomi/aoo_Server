package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class HttpGatewayContractTest {
    @Test void enforcesApiPrefixTlsAndNormalizedPaths() {
        assertEquals("/api/v2/room/create", HttpRoutePolicy.requireApiV1("/api/v2/room/create"));
        assertThrows(IllegalArgumentException.class, () -> HttpRoutePolicy.requireApiV1("/room/create"));
        assertThrows(IllegalArgumentException.class, () -> HttpRoutePolicy.requireApiV1("/api/v10/room"));
        assertThrows(IllegalArgumentException.class, () -> HttpRoutePolicy.requireApiV1("/api/v2//room"));
        assertThrows(SecurityException.class, () -> HttpRoutePolicy.requireHttps(false));
        assertDoesNotThrow(() -> HttpRoutePolicy.requireVersion("1"));
        assertThrows(HttpRoutePolicy.UnsupportedApiVersionException.class, () -> HttpRoutePolicy.requireVersion(null));
        assertTrue(HttpRoutePolicy.isRetiredEntry("/v1/history"));
        assertTrue(HttpRoutePolicy.isRetiredEntry("/api/v1/account/dispatch"));
        assertTrue(HttpRoutePolicy.isRetiredEntry("/ClientPack"));
        assertTrue(HttpRoutePolicy.isRetiredEntry("/JavaServerPack"));
        assertTrue(HttpRoutePolicy.isRetiredEntry("/login.php"));
        assertTrue(HttpRoutePolicy.isRetiredEntry("/WEBSOCKET"));
        assertFalse(HttpRoutePolicy.isRetiredEntry("/api/v2/history"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/gateway/ws"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/gateway/ws_ticket"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/version/check"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/account/guest/login"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/player-profile/me"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/hall/rooms"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/social"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/notifications"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/mail"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/notices"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/red-dots"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/gifts"));
        assertTrue(HttpRoutePolicy.isProductionRoute("/api/v2/gifts/inbox"));
        assertFalse(HttpRoutePolicy.isProductionRoute("/api/v2/gateway/ws/duplicate"));
        assertFalse(HttpRoutePolicy.isProductionRoute("/api/v2/history"));
    }

    @Test void responseAlwaysCarriesTraceTimestampAndNonNullData() {
        AuthoritativeTimeSource time = new AuthoritativeTimeSource(Clock.fixed(Instant.ofEpochMilli(1234L), ZoneOffset.UTC));
        HttpResult<Map<String, Object>> result = HttpResult.success(Map.of(), "trace-1", time);
        assertEquals(0, result.code());
        assertEquals(1234L, result.timestamp());
        assertNotNull(result.data());
        assertThrows(NullPointerException.class, () -> HttpResult.success(null, "trace-1", time));
    }

    @Test void gatewayErrorCodesAreUniqueAndStayInTheirDeclaredDomain() {
        var codes = new java.util.HashSet<Integer>();
        for (GatewayErrorCode code : GatewayErrorCode.values()) {
            assertTrue(codes.add(code.code()), () -> "duplicate gateway error code: " + code.code());
            assertTrue((code.code() >= 1000 && code.code() <= 1999)
                    || (code.code() >= 3000 && code.code() <= 3999));
        }
    }

    @Test void browserCorsAndProxyPreserveTheCompletePlayerIdentityHeaders() throws Exception {
        assertTrue(GatewayApplication.CORS_ALLOW_HEADERS.contains("X-Player-Id"));
        assertTrue(GatewayApplication.CORS_ALLOW_HEADERS.contains("X-Aoo-Api-Version"));
        assertTrue(GatewayApplication.CORS_ALLOW_HEADERS.contains("Idempotency-Key"));
        assertTrue(GatewayApplication.PROXY_IDENTITY_HEADERS.contains("X-Player-Id"));
        assertTrue(GatewayApplication.PROXY_IDENTITY_HEADERS.contains("Authorization"));
        assertTrue(GatewayApplication.PROXY_IDENTITY_HEADERS.contains("Idempotency-Key"));
        assertTrue(java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/aoo/bcg/gateway/GatewayApplication.java"))
                .contains("jdk.httpclient.keepalive.timeout"));
    }

}
