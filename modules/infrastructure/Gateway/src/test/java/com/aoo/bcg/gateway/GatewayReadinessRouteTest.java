package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GatewayReadinessRouteTest {
    @Test
    void publicReadinessIsExactStableAndFailClosed() throws Exception {
        FakeAuthority authority = new FakeAuthority(true);
        GatewayApplication.Ingress ingress = ingress(authority);
        EmbeddedChannel channel = new EmbeddedChannel(ingress);
        FullHttpResponse ready = request(channel, HttpMethod.GET, "/health/gateway", null);
        assertEquals(HttpResponseStatus.OK, ready.status());
        assertEquals("no-store", ready.headers().get(HttpHeaderNames.CACHE_CONTROL));
        assertEquals("nosniff", ready.headers().get("X-Content-Type-Options"));
        assertEquals(Map.of("status", "ready", "code", "GATEWAY_READY"),
                new ObjectMapper().readValue(io.netty.buffer.ByteBufUtil.getBytes(ready.content()), Map.class));
        ready.release();
        channel.finishAndReleaseAll();

        authority.ready.set(false);
        EmbeddedChannel notReadyChannel = new EmbeddedChannel(ingress(authority));
        FullHttpResponse notReady = request(notReadyChannel, HttpMethod.GET, "/health/gateway", null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, notReady.status());
        notReady.release();
        notReadyChannel.finishAndReleaseAll();
    }

    @Test
    void publicReadinessRejectsMutationQueryAndBodyWithoutEnteringApiAllowlist() {
        FakeAuthority authority = new FakeAuthority(true);
        assertTrue(HttpRoutePolicy.isGatewayReadinessRoute("/health/gateway"));
        assertFalse(HttpRoutePolicy.isProductionRoute("/health/gateway"));
        assertFalse(HttpRoutePolicy.isGatewayReadinessRoute("/health/gateway/"));
        EmbeddedChannel channel = new EmbeddedChannel(ingress(authority));
        FullHttpResponse method = request(channel, HttpMethod.POST, "/health/gateway", null);
        assertEquals(HttpResponseStatus.METHOD_NOT_ALLOWED, method.status());
        method.release();
        channel.finishAndReleaseAll();
        channel = new EmbeddedChannel(ingress(authority));
        FullHttpResponse query = request(channel, HttpMethod.GET, "/health/gateway?probe=1", null);
        assertEquals(HttpResponseStatus.BAD_REQUEST, query.status());
        query.release();
        channel.finishAndReleaseAll();
        channel = new EmbeddedChannel(ingress(authority));
        FullHttpResponse body = request(channel, HttpMethod.GET, "/health/gateway", "{}".getBytes());
        assertEquals(HttpResponseStatus.BAD_REQUEST, body.status());
        body.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void authenticatedInternalReadinessUsesRealAuthorityAndRejectsNegativeShapes() throws Exception {
        FakeAuthority authority = new FakeAuthority(true);
        var server = GatewayApplication.startInternalAuthority(0, authority, authority, "test-token", new ObjectMapper());
        try {
            HttpClient client = HttpClient.newHttpClient();
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpResponse<String> ready = send(client, base.resolve("/internal/v1/health/room-authority"), "test-token", HttpRequest.BodyPublishers.noBody());
            assertEquals(200, ready.statusCode());
            assertEquals("no-store", ready.headers().firstValue("Cache-Control").orElseThrow());
            assertTrue(ready.body().contains("ROOM_AUTHORITY_READY"));
            HttpResponse<String> denied = send(client, base.resolve("/internal/v1/health/room-authority"), "wrong", HttpRequest.BodyPublishers.noBody());
            assertEquals(403, denied.statusCode());
            HttpResponse<String> query = send(client, base.resolve("/internal/v1/health/room-authority?probe=1"), "test-token", HttpRequest.BodyPublishers.noBody());
            assertEquals(400, query.statusCode());
            HttpResponse<String> body = send(client, base.resolve("/internal/v1/health/room-authority"), "test-token", HttpRequest.BodyPublishers.ofString("{}"));
            assertEquals(400, body.statusCode());
            HttpRequest wrongMethod = HttpRequest.newBuilder(base.resolve("/internal/v1/health/room-authority"))
                    .header("X-Aoo-Internal-Token", "test-token").POST(HttpRequest.BodyPublishers.noBody()).build();
            assertEquals(405, client.send(wrongMethod, HttpResponse.BodyHandlers.ofString()).statusCode());
            authority.ready.set(false);
            HttpResponse<String> unavailable = send(client, base.resolve("/internal/v1/health/room-authority"), "test-token", HttpRequest.BodyPublishers.noBody());
            assertEquals(503, unavailable.statusCode());
        } finally {
            server.stop(0);
        }
    }

    private static HttpResponse<String> send(HttpClient client, URI uri, String token,
                                             HttpRequest.BodyPublisher body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("X-Aoo-Internal-Token", token)
                .method("GET", body).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static GatewayApplication.Ingress ingress(GatewayRuntimeProvider.RoomAuthority authority) {
        return new GatewayApplication.Ingress(null, new ObjectMapper(), Set.of("http://127.0.0.1:7458"),
                Runnable::run, () -> identity -> null, HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1:8095"), URI.create("http://127.0.0.1:8096"),
                URI.create("http://127.0.0.1:8093"), URI.create("http://127.0.0.1:8097"), false, authority);
    }

    private static FullHttpResponse request(EmbeddedChannel channel, HttpMethod method, String uri, byte[] body) {
        DefaultFullHttpRequest request = body == null
                ? new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri)
                : new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri,
                io.netty.buffer.Unpooled.wrappedBuffer(body));
        request.headers().set(HttpHeaderNames.HOST, "127.0.0.1:8080");
        channel.writeInbound(request);
        channel.runPendingTasks();
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        return response;
    }

    private static final class FakeAuthority implements GatewayRuntimeProvider.RoomAuthority {
        private final AtomicBoolean ready;
        private FakeAuthority(boolean value) { ready = new AtomicBoolean(value); }
        @Override public Map<String, Object> create(Map<String, Object> command) { return Map.of(); }
        @Override public Map<String, Object> recover(Map<String, Object> command) { return Map.of(); }
        @Override public Map<String, Object> join(Map<String, Object> command) { return Map.of(); }
        @Override public Map<String, Object> leave(Map<String, Object> command) { return Map.of(); }
        @Override public void remove(long roomId, long fencingToken) { }
        @Override public GatewayRuntimeProvider.Readiness readiness() {
            return ready.get() ? GatewayRuntimeProvider.Readiness.readyResult() : GatewayRuntimeProvider.Readiness.notReady();
        }
    }
}
