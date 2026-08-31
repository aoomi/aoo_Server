package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned, fail-closed adapter for the sole Admin map operation. */
public final class AdminMapProxy {
    static final URI ENDPOINT = URI.create("https://restapi.amap.com/v3/ip");
    private static final Set<String> ALLOWED_PARAMETERS = Set.of("ip");
    private final String providerKey;
    private final URI endpoint;
    private final HttpClient client;
    private final ObjectMapper json;
    private final Clock clock;
    private final Duration timeout;
    private final int ratePerMinute;
    private final Duration cacheTtl;
    private final Audit audit;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<Long, RateWindow> rates = new ConcurrentHashMap<>();

    public record AuditEvent(String requestId, long operatorId, String outcome, boolean cached, Instant occurredAt) {}
    @FunctionalInterface public interface Audit { void record(AuditEvent event); }
    private record CacheEntry(Map<String, String> value, Instant expiresAt) {}
    private static final class RateWindow { Instant start; int count; RateWindow(Instant start) { this.start = start; } }
    public static final class ProxyException extends RuntimeException {
        private final int status; private final int code;
        ProxyException(int status, int code, String message) { super(message); this.status=status; this.code=code; }
        public int status() { return status; } public int code() { return code; }
    }

    static AdminMapProxy production(String key, Clock clock, Duration timeout, int rate, Duration ttl, Audit audit) {
        return new AdminMapProxy(key, ENDPOINT, HttpClient.newBuilder().connectTimeout(timeout).build(),
                new ObjectMapper(), clock, timeout, rate, ttl, audit);
    }

    AdminMapProxy(String key, URI endpoint, HttpClient client, ObjectMapper json, Clock clock,
            Duration timeout, int ratePerMinute, Duration cacheTtl, Audit audit) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("map provider secret is required");
        this.providerKey=key; this.endpoint=endpoint; this.client=client; this.json=json; this.clock=clock; this.timeout=timeout;
        this.ratePerMinute=ratePerMinute; this.cacheTtl=cacheTtl; this.audit=audit;
    }

    public Map<String, String> locate(Map<String, String> parameters, long operatorId, String requestId) {
        if (!parameters.keySet().equals(ALLOWED_PARAMETERS)) throw new ProxyException(400, 1002, "invalid_request");
        String ip = parameters.get("ip");
        if (ip == null || ip.length() > 45 || !ip.matches("[0-9A-Fa-f:.]{2,45}"))
            throw new ProxyException(400, 1002, "invalid_request");
        rateLimit(operatorId);
        Instant now = clock.instant();
        CacheEntry cached = cache.get(ip);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            audit.record(new AuditEvent(requestId, operatorId, "success", true, now));
            return cached.value();
        }
        try {
            String query = "ip=" + encode(ip) + "&key=" + encode(providerKey);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "?" + query))
                    .timeout(timeout).header("Accept", "application/json").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable(requestId, operatorId);
            JsonNode body = json.readTree(response.body());
            if (!"1".equals(body.path("status").asText())) throw unavailable(requestId, operatorId);
            Map<String, String> result = Map.of("status", "1",
                    "province", safe(body.path("province").asText()),
                    "city", safe(body.path("city").asText()));
            cache.put(ip, new CacheEntry(result, now.plus(cacheTtl)));
            audit.record(new AuditEvent(requestId, operatorId, "success", false, now));
            return result;
        } catch (ProxyException error) { throw error; }
        catch (java.net.http.HttpTimeoutException error) {
            audit.record(new AuditEvent(requestId, operatorId, "timeout", false, now));
            throw new ProxyException(504, 3002, "upstream_timeout");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt(); throw unavailable(requestId, operatorId);
        } catch (Exception error) { throw unavailable(requestId, operatorId); }
    }

    private void rateLimit(long operatorId) {
        Instant now=clock.instant();
        RateWindow window=rates.computeIfAbsent(operatorId, ignored -> new RateWindow(now));
        synchronized (window) {
            if (!window.start.plusSeconds(60).isAfter(now)) { window.start=now; window.count=0; }
            if (++window.count > ratePerMinute) throw new ProxyException(429, 3001, "rate_limited");
        }
    }
    private ProxyException unavailable(String requestId, long operatorId) {
        audit.record(new AuditEvent(requestId, operatorId, "unavailable", false, clock.instant()));
        return new ProxyException(502, 3003, "upstream_unavailable");
    }
    private static String safe(String value) { return value == null || value.length() > 80 ? "" : value; }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    public static Map<String, String> parseQuery(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        Map<String,String> values=new java.util.LinkedHashMap<>();
        for (String pair : raw.split("&")) {
            String[] parts=pair.split("=",2);
            if (parts.length != 2) throw new ProxyException(400,1002,"invalid_request");
            String name=URLDecoder.decode(parts[0],StandardCharsets.UTF_8);
            if (values.put(name,URLDecoder.decode(parts[1],StandardCharsets.UTF_8)) != null)
                throw new ProxyException(400,1002,"invalid_request");
        }
        return Map.copyOf(values);
    }
}
