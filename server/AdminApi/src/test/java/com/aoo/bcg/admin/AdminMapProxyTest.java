package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AdminMapProxyTest {
    @Test void whitelistsInputCachesAndNeverReturnsSecret() throws Exception {
        String secret="provider-secret-only-on-the-server";
        AtomicInteger calls=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v3/ip", exchange -> {
            calls.incrementAndGet();
            assertTrue(exchange.getRequestURI().getRawQuery().contains("key="));
            byte[] body="{\"status\":\"1\",\"province\":\"Zhejiang\",\"city\":\"Hangzhou\"}".getBytes();
            exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var audit=new ArrayList<AdminMapProxy.AuditEvent>();
            var proxy=new AdminMapProxy(secret, URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v3/ip"),
                    HttpClient.newHttpClient(),new ObjectMapper(),Clock.systemUTC(),Duration.ofSeconds(1),2,
                    Duration.ofMinutes(1),audit::add);
            Map<String,String> first=proxy.locate(Map.of("ip","203.0.113.7"),9,"req-1");
            Map<String,String> second=proxy.locate(Map.of("ip","203.0.113.7"),9,"req-2");
            assertEquals(first,second); assertEquals(1,calls.get()); assertEquals(2,audit.size());
            assertFalse(new ObjectMapper().writeValueAsString(first).contains(secret));
            assertThrows(AdminMapProxy.ProxyException.class,
                    () -> proxy.locate(Map.of("ip","203.0.113.8","key","attacker"),9,"req-3"));
        } finally { server.stop(0); }
    }

    @Test void rejectsInvalidIpAndEnforcesPerOperatorRateLimit() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v3/ip", exchange -> { byte[] body="{\"status\":\"1\"}".getBytes(); exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close(); });
        server.start();
        try {
            var proxy=new AdminMapProxy("provider-secret-only-on-the-server",URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v3/ip"),
                    HttpClient.newHttpClient(),new ObjectMapper(),Clock.systemUTC(),Duration.ofSeconds(1),1,Duration.ofSeconds(1),event -> {});
            assertEquals(400,assertThrows(AdminMapProxy.ProxyException.class,
                    () -> proxy.locate(Map.of("ip","example.com"),7,"bad")).status());
            proxy.locate(Map.of("ip","203.0.113.1"),7,"one");
            assertEquals(429,assertThrows(AdminMapProxy.ProxyException.class,
                    () -> proxy.locate(Map.of("ip","203.0.113.2"),7,"two")).status());
        } finally { server.stop(0); }
    }
}
