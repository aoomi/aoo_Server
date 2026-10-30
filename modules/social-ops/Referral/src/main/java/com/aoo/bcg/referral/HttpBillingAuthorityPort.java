package com.aoo.bcg.referral;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Calls the versioned Billing service; this module never writes wallet or ledger tables. */
public final class HttpBillingAuthorityPort implements BillingAuthorityPort {
    private final HttpClient client; private final URI endpoint; private final String token; private final ObjectMapper json;
    public HttpBillingAuthorityPort(HttpClient client,URI base,String token,ObjectMapper json){this.client=Objects.requireNonNull(client);this.endpoint=base.resolve("/v1/billing");if(token==null||token.length()<32)throw new IllegalArgumentException("billing token must be at least 32 characters");this.token=token;this.json=Objects.requireNonNull(json);}
    @Override public void credit(String key,long player,long scope,String currency,BigDecimal amount,String reason)throws Exception{
        byte[] body=json.writeValueAsBytes(Map.of("action","credit","playerId",player,"scopeId",scope,"currency",currency,"amount",amount.longValueExact(),"reasonCode",reason));
        HttpRequest request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("X-API-Version","1").header("Authorization","Bearer "+token).header("Idempotency-Key",key).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(response.statusCode()/100!=2)throw new IllegalStateException("billing rejected commission settlement: HTTP "+response.statusCode());
    }
}
