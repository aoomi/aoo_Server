package com.aoo.bcg.external;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public record ExternalRequest(String method, String relativePath, Map<String, String> headers,
                              byte[] body, String idempotencyKey) {
    public ExternalRequest {
        method = Objects.requireNonNull(method, "method").toUpperCase(java.util.Locale.ROOT);
        if (!method.matches("GET|POST|PUT|PATCH|DELETE")) throw new IllegalArgumentException("unsupported method");
        URI path = URI.create(Objects.requireNonNull(relativePath, "relativePath"));
        if (path.isAbsolute() || relativePath.startsWith("//")) throw new IllegalArgumentException("relativePath must stay on configured host");
        headers = Map.copyOf(headers == null ? Map.of() : headers);
        if (headers.keySet().stream().anyMatch(h -> h.equalsIgnoreCase("authorization") || h.equalsIgnoreCase("x-api-key"))) {
            throw new IllegalArgumentException("credential headers are transport-owned");
        }
        body = body == null ? new byte[0] : body.clone();
    }

    @Override public byte[] body() { return body.clone(); }

    public static ExternalRequest json(String method, String path, String json, String idempotencyKey) {
        return new ExternalRequest(method, path, Map.of("Content-Type", "application/json; charset=utf-8"),
                Objects.requireNonNull(json).getBytes(StandardCharsets.UTF_8), idempotencyKey);
    }

    boolean retrySafe() {
        return method.equals("GET") || method.equals("PUT") || method.equals("DELETE")
                || (idempotencyKey != null && !idempotencyKey.isBlank());
    }

    HttpRequest.BodyPublisher publisher() {
        return body.length == 0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body);
    }
}
