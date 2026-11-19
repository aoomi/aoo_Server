package com.aoo.bcg.external;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExternalPlatformClientTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void sendsRealProtocolAndRetriesTransientResponseWithSameIdempotencyKey() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/orders", exchange -> {
            assertEquals("Bearer test-secret", exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("order-42", exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            assertEquals("{\"amount\":12}", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int attempt = calls.incrementAndGet();
            byte[] body = (attempt == 1 ? "busy" : "{\"accepted\":true}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(attempt == 1 ? 503 : 201, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ExternalPlatformClient client = client(3, 5, Duration.ofMillis(300));
        ExternalResponse response = client.execute(ExternalRequest.json("POST", "/v1/orders", "{\"amount\":12}", "order-42"));

        assertEquals(201, response.statusCode());
        assertEquals("{\"accepted\":true}", response.utf8Body());
        assertEquals(2, calls.get());
    }

    @Test void doesNotRetryNonIdempotentPostOrPretendHttpFailureSucceeded() throws Exception {
        AtomicInteger calls = serveConstant(503, "unavailable");
        ExternalPlatformClient client = client(3, 5, Duration.ofMillis(300));

        ExternalPlatformException.Protocol error = assertThrows(ExternalPlatformException.Protocol.class,
                () -> client.execute(ExternalRequest.json("POST", "/failure", "{}", null)));

        assertEquals(503, error.statusCode());
        assertEquals(1, calls.get());
    }

    @Test void requestTimeoutIsBoundedAndCircuitFailsFastAfterThreshold() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/slow", exchange -> {
            calls.incrementAndGet();
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            try { exchange.sendResponseHeaders(200, 0); exchange.close(); } catch (Exception ignored) { }
        });
        server.start();
        ExternalPlatformClient client = client(1, 2, Duration.ofMillis(40));

        assertThrows(ExternalPlatformException.Transport.class,
                () -> client.execute(new ExternalRequest("GET", "/slow", Map.of(), null, null)));
        assertThrows(ExternalPlatformException.Transport.class,
                () -> client.execute(new ExternalRequest("GET", "/slow", Map.of(), null, null)));
        assertThrows(ExternalPlatformException.CircuitOpen.class,
                () -> client.execute(new ExternalRequest("GET", "/slow", Map.of(), null, null)));
        assertEquals(2, calls.get());
    }

    @Test void rejectsCredentialOverrideAndInsecureRemoteEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalRequest("GET", "/", Map.of("Authorization", "leak"), null, null));
        assertThrows(IllegalArgumentException.class, () -> new ExternalPlatformConfig(URI.create("http://example.com"),
                Duration.ofSeconds(1), Duration.ofSeconds(1), 1, Duration.ofMillis(1), 1, Duration.ofSeconds(1)));
        assertFalse(SecretProvider.environment("SOME_SECRET").toString().contains("SOME_SECRET"));
    }

    private AtomicInteger serveConstant(int status, String text) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/failure", exchange -> {
            calls.incrementAndGet();
            byte[] body = text.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return calls;
    }

    private ExternalPlatformClient client(int attempts, int threshold, Duration requestTimeout) {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        ExternalPlatformConfig config = new ExternalPlatformConfig(base, Duration.ofMillis(100), requestTimeout,
                attempts, Duration.ofMillis(1), threshold, Duration.ofSeconds(30));
        return new ExternalPlatformClient(config, () -> "test-secret");
    }
}
