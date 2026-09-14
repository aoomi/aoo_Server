package com.aoo.bcg.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Internal-only, retrying lifecycle bridge from Gateway authority to Hall persistence. */
final class HallRoomLifecycleClient {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final URI base;private final String token;private final ObjectMapper json;
    HallRoomLifecycleClient(URI base,String token,ObjectMapper json){this.base=Objects.requireNonNull(base);this.token=required(token,"hall internal token");if(this.token.length()<32)throw new IllegalArgumentException("hall internal token must contain at least 32 characters");this.json=Objects.requireNonNull(json);}
    void close(long roomId,String requestId,String traceId,String reason){post("/internal/v1/hall/rooms/"+roomId+"/close",Map.of("requestId",required(requestId,"requestId"),"reason",required(reason,"reason")),traceId,"close");}
    void memberLeft(long roomId,long accountId,String requestId,String traceId){if(accountId<=0)throw new IllegalArgumentException("accountId is required");post("/internal/v1/hall/rooms/"+roomId+"/members/"+accountId+"/leave",Map.of("requestId",required(requestId,"requestId")),traceId,"member leave");}
    private void post(String path,Map<String,Object> payload,String traceId,String operation){RuntimeException last=null;URI endpoint=base.resolve(path);for(int attempt=1;attempt<=3;attempt++)try{byte[]body=json.writeValueAsBytes(payload);HttpRequest request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(4)).header("Content-Type","application/json").header("X-Aoo-Internal-Token",token).header("X-Trace-Id",required(traceId,"traceId")).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();HttpResponse<String>response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(response.statusCode()/100==2)return;last=new IllegalStateException("Hall lifecycle "+operation+" rejected with HTTP "+response.statusCode());}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Hall lifecycle "+operation+" interrupted",interrupted);}catch(Exception failure){last=failure instanceof RuntimeException runtime?runtime:new IllegalStateException("Hall lifecycle "+operation+" unavailable",failure);}throw last==null?new IllegalStateException("Hall lifecycle "+operation+" unavailable"):last;}
    private static String required(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" is required");return value.strip();}
}
