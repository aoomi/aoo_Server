package com.aoo.bcg.luckdraw;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.*;

/** Production reward adapter. It can only invoke the Billing and Inventory authorities. */
public final class HttpRewardPort implements LuckDrawPorts.RewardPort {
    private final HttpClient client;private final URI billing;private final URI inventory;private final String billingToken;private final String inventoryToken;private final ObjectMapper json;
    public HttpRewardPort(URI billing,String billingToken,URI inventory,String inventoryToken,ObjectMapper json){this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),billing,billingToken,inventory,inventoryToken,json);}
    HttpRewardPort(HttpClient client,URI billing,String billingToken,URI inventory,String inventoryToken,ObjectMapper json){this.client=Objects.requireNonNull(client);this.billing=Objects.requireNonNull(billing);this.inventory=Objects.requireNonNull(inventory);this.billingToken=requireToken(billingToken);this.inventoryToken=requireToken(inventoryToken);this.json=Objects.requireNonNull(json);}
    @Override public void creditCurrency(String requestId,long playerId,String currencyCode,long amount){post(billing.resolve("/v1/billing"),billingToken,requestId,Map.of("action","credit","playerId",playerId,"currency",currencyCode,"scopeId",0,"amount",amount,"reasonCode","LUCK_DRAW"));}
    @Override public void grantItem(String requestId,long playerId,String itemCode,long quantity){post(inventory.resolve("/internal/v1/inventory"),inventoryToken,requestId,Map.of("action","grant","playerId",playerId,"itemCode",itemCode,"quantity",quantity,"source","LUCK_DRAW"));}
    private void post(URI uri,String token,String key,Object body){try{var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).header("Authorization","Bearer "+token).header("X-API-Version","1").header("Idempotency-Key",key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body))).build();var response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(response.statusCode()/100!=2)throw new IllegalStateException("reward authority rejected request: "+response.statusCode());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("reward authority interrupted",e);}catch(Exception e){throw e instanceof RuntimeException r?r:new IllegalStateException("reward authority unavailable",e);}}
    private static String requireToken(String value){if(value==null||value.length()<32)throw new IllegalArgumentException("reward authority token must be at least 32 characters");return value;}
}
