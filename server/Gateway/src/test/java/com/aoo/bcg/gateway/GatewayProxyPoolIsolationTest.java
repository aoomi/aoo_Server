package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GatewayProxyPoolIsolationTest {
    @Test void slowUpstreamsDoNotStarveAuthorityWorkers() throws Exception {
        HttpServer upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 8);
        CountDownLatch received = new CountDownLatch(8);
        CountDownLatch release = new CountDownLatch(1);
        upstream.createContext("/api/v2/hall/slow", exchange -> {
            received.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            byte[] body = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        upstream.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        upstream.start();
        ThreadPoolExecutor workers = new ThreadPoolExecutor(8, 8, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(32));
        try {
            URI base = URI.create("http://127.0.0.1:" + upstream.getAddress().getPort());
            for (int index = 0; index < 8; index++) {
                var ingress = new GatewayApplication.Ingress(null, new ObjectMapper(), Set.of("http://127.0.0.1:7456"),
                        workers, () -> null, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                        base, base, base, base, true);
                EmbeddedChannel channel = new EmbeddedChannel(ingress);
                var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/v2/hall/slow");
                request.headers().set(HttpHeaderNames.ORIGIN, "http://127.0.0.1:7456")
                        .set(HttpHeaderNames.HOST, "127.0.0.1")
                        .set("X-Aoo-Api-Version", "1");
                channel.writeInbound(request);
            }
            assertTrue(received.await(2, TimeUnit.SECONDS));
            CountDownLatch authorityRan = new CountDownLatch(1);
            workers.execute(authorityRan::countDown);
            assertTrue(authorityRan.await(300, TimeUnit.MILLISECONDS),
                    "slow upstream requests must not retain every authority worker");
        } finally {
            release.countDown();
            workers.shutdownNow();
            upstream.stop(0);
        }
    }
}
