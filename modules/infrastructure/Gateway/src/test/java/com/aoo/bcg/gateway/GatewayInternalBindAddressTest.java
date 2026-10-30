package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GatewayInternalBindAddressTest {
    @Test void defaultsToNumericLoopbackOnlyWhenConfigurationIsAbsent() throws Exception {
        assertEquals("127.0.0.1", GatewayApplication.internalBindAddress(null, null).getHostAddress());
        assertEquals("127.0.0.7", GatewayApplication.internalBindAddress("127.0.0.7").getHostAddress());
    }

    @Test void propertyTakesPrecedenceWithoutSilentlyDiscardingInvalidValues() throws Exception {
        assertEquals("10.0.0.7", GatewayApplication.internalBindAddress("10.0.0.7", "192.168.1.7").getHostAddress());
        assertEquals("172.23.0.5", GatewayApplication.internalBindAddress(null, "172.23.0.5").getHostAddress());
        assertThrows(IllegalArgumentException.class, () -> GatewayApplication.internalBindAddress("", "10.0.0.7"));
        assertThrows(IllegalArgumentException.class, () -> GatewayApplication.internalBindAddress("8.8.8.8", "10.0.0.7"));
        assertThrows(IllegalArgumentException.class, () -> GatewayApplication.internalBindAddress(null, ""));
    }

    @Test void acceptsRfc1918RangesAndRejectsTheirPublicNeighbours() throws Exception {
        for (String address : new String[]{"10.0.0.0", "10.255.255.255", "172.16.0.0", "172.31.255.255", "192.168.0.0", "192.168.255.255"})
            assertEquals(address, GatewayApplication.internalBindAddress(address).getHostAddress());
        for (String address : new String[]{"9.255.255.255", "11.0.0.0", "172.15.255.255", "172.32.0.0", "192.167.255.255", "192.169.0.0"})
            assertThrows(IllegalArgumentException.class, () -> GatewayApplication.internalBindAddress(address));
    }

    @Test void rejectsWildcardsPublicAddressesNamesAndMalformedLiteralsWithoutDns() {
        for (String address : new String[]{"0.0.0.0", "8.8.8.8", "169.254.1.1", "224.0.0.1", "255.255.255.255", "localhost", "gateway", "::", "::1", "", " ", "127.0.0.1 ", "127.0.0", "127.0.0.1.2", "127.0.0.256", "127.-1.0.1", "127.00.0.1", "127.0.0.1\n"})
            assertThrows(IllegalArgumentException.class, () -> GatewayApplication.internalBindAddress(address));
    }

    @Test void explicitBindOverloadRejectsWildcardAndNullBeforeStartingAListener() throws Exception {
        var authority = new FakeAuthority();
        assertThrows(IllegalArgumentException.class, () -> GatewayApplication.startInternalAuthority(
                0, authority, authority, "fixture-internal", new ObjectMapper(), InetAddress.getByAddress(new byte[4])));
        assertThrows(NullPointerException.class, () -> GatewayApplication.startInternalAuthority(
                0, authority, authority, "fixture-internal", new ObjectMapper(), null));
    }

    @Test void explicitLoopbackListenerPreservesSessionReplacementAuthenticationAndValidation() throws Exception {
        var authority = new FakeAuthority();
        var server = GatewayApplication.startInternalAuthority(0, authority, authority, "fixture-internal",
                new ObjectMapper(), GatewayApplication.internalBindAddress("127.0.0.1"));
        try {
            assertEquals("127.0.0.1", server.getAddress().getAddress().getHostAddress());
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/internal/v1/session/replace");
            String validBody = "{\"accountId\":42,\"sessionId\":\"fixture-session\",\"authGeneration\":1}";
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            assertEquals(403, send(http, uri, null, "POST", validBody).statusCode());
            assertEquals(403, send(http, uri, "wrong", "POST", validBody).statusCode());
            assertEquals(403, send(http, uri, "fixture-internal", "GET", validBody).statusCode());
            assertEquals(400, send(http, uri, "fixture-internal", "POST", "{\"accountId\":0,\"sessionId\":\"fixture-session\",\"authGeneration\":1}").statusCode());
            HttpResponse<String> accepted = send(http, uri, "fixture-internal", "POST", validBody);
            assertEquals(200, accepted.statusCode());
            assertEquals(0, new ObjectMapper().readTree(accepted.body()).path("closed").asInt(-1));
        } finally { server.stop(0); }
    }

    @Test void legacyOverloadStillBindsLoopbackAndRetainsAuthenticatedReadiness() throws Exception {
        var authority = new FakeAuthority();
        var server = GatewayApplication.startInternalAuthority(0, authority, "fixture-internal", new ObjectMapper());
        try {
            assertTrue(server.getAddress().getAddress().isLoopbackAddress());
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/internal/v1/health/room-authority");
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            assertEquals(403, send(http, uri, null, "GET", "").statusCode());
            HttpResponse<String> ready = send(http, uri, "fixture-internal", "GET", "");
            assertEquals(200, ready.statusCode());
            assertTrue(ready.body().contains("ROOM_AUTHORITY_READY"));
        } finally { server.stop(0); }
    }

    private static HttpResponse<String> send(HttpClient http, URI uri, String token, String method, String body) throws Exception {
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3))
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("X-Aoo-Internal-Token", token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final class FakeAuthority implements GatewayRuntimeProvider.RoomAuthority {
        @Override public Map<String, Object> create(Map<String, Object> command) { return Map.of(); }
        @Override public Map<String, Object> recover(Map<String, Object> command) { return Map.of(); }
        @Override public Map<String, Object> join(Map<String, Object> command) { return Map.of(); }
        @Override public Map<String, Object> leave(Map<String, Object> command) { return Map.of(); }
        @Override public void remove(long roomId, long fencingToken) { }
        @Override public GatewayRuntimeProvider.Readiness readiness() { return GatewayRuntimeProvider.Readiness.readyResult(); }
    }
}
