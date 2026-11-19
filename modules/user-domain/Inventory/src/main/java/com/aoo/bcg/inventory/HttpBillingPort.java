package com.aoo.bcg.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.*;

/** Production Billing adapter targeting the already-mounted /v1/billing route. */
public final class HttpBillingPort implements BillingPort {
 private final URI endpoint;private final String token;private final HttpClient client;private final ObjectMapper json;
 public HttpBillingPort(URI base,String token,ObjectMapper json){this.endpoint=base.resolve("/v1/billing");this.token=Objects.requireNonNull(token);this.json=Objects.requireNonNull(json);this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();if(token.length()<32)throw new IllegalArgumentException("billing token must be at least 32 characters");}
 public void debit(String key,long player,String currency,long amount,String reason){invoke("debit",key,player,currency,amount,reason);}
 public void refund(String key,long player,String currency,long amount,String reason){invoke("refund",key,player,currency,amount,reason);}
 private void invoke(String action,String key,long player,String currency,long amount,String reason){try{byte[] body=json.writeValueAsBytes(Map.of("action",action,"playerId",player,"currency",currency,"scopeId",0,"amount",amount,"reasonCode",reason));var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8)).header("Authorization","Bearer "+token).header("X-API-Version","1").header("Idempotency-Key",key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();var response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(response.statusCode()/100!=2)throw new IllegalStateException("billing rejected request: HTTP "+response.statusCode());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("billing interrupted",e);}catch(Exception e){if(e instanceof IllegalStateException x)throw x;throw new IllegalStateException("billing unavailable",e);}}
}
