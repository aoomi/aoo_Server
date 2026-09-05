package com.aoo.bcg.account;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Synchronous loopback adapter. A successful login is not returned before Gateway accepts the replacement event. */
public final class HttpGatewaySessionReplacementNotifier implements SessionReplacementNotifier {
    private final URI endpoint;
    private final String token;
    private final ObjectMapper json;
    private final HttpClient http;

    public HttpGatewaySessionReplacementNotifier(URI gatewayInternalBase, String token, ObjectMapper json) {
        URI base=Objects.requireNonNull(gatewayInternalBase,"gatewayInternalBase");
        this.endpoint=base.resolve(base.getPath().endsWith("/")?"internal/v1/session/replace":"/internal/v1/session/replace");
        this.token=Objects.requireNonNull(token,"token");
        this.json=Objects.requireNonNull(json,"json");
        this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Override public void replaced(long accountId, String currentSessionId, long authGeneration) {
        try {
            byte[] body=json.writeValueAsBytes(Map.of(
                    "accountId",accountId,
                    "sessionId",currentSessionId,
                    "authGeneration",authGeneration));
            HttpRequest request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(3))
                    .header("Content-Type","application/json")
                    .header("X-Aoo-Internal-Token",token)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            HttpResponse<Void> response=http.send(request,HttpResponse.BodyHandlers.discarding());
            if(response.statusCode()!=200)throw new IllegalStateException("gateway session replacement rejected: HTTP "+response.statusCode());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("gateway session replacement interrupted",interrupted);
        } catch (Exception failure) {
            if(failure instanceof IllegalStateException state)throw state;
            throw new IllegalStateException("gateway session replacement unavailable",failure);
        }
    }
}
