package com.aoo.bcg.external;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public record ExternalResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {
    public ExternalResponse {
        headers = Map.copyOf(headers);
        body = body.clone();
    }
    @Override public byte[] body() { return body.clone(); }
    public String utf8Body() { return new String(body, StandardCharsets.UTF_8); }
    public boolean successful() { return statusCode >= 200 && statusCode < 300; }
}
