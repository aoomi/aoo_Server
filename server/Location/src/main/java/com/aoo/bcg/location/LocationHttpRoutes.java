package com.aoo.bcg.location;

import static com.aoo.bcg.location.LocationModels.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Production HTTP adapter. Mount on an externally managed HttpServer and executor. */
public final class LocationHttpRoutes {
    private static final int MAX_BODY_BYTES=64*1024;
    private final LocationRiskService service; private final LocationRequestAuthenticator authenticator; private final ObjectMapper json;
    public LocationHttpRoutes(LocationRiskService service,LocationRequestAuthenticator authenticator,ObjectMapper mapper){this.service=Objects.requireNonNull(service);this.authenticator=Objects.requireNonNull(authenticator);this.json=mapper.copy().findAndRegisterModules().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);}
    public void mount(HttpServer server){server.createContext("/health/location",e->reply(e,200,Map.of("status","UP","apiVersion",1,"persistence","jdbc")));server.createContext("/api/v1/location/authorization",this::authorization);server.createContext("/api/v1/location/signals",this::signals);server.createContext("/api/v1/location/table-risk",this::tableRisk);}
    private void authorization(HttpExchange e)throws IOException{handle(e,()->{var d=read(e,AuthorizationDto.class);return Map.of("eventId",service.reportAuthorization(version(e),key(e),authenticator.authenticate(e.getRequestHeaders()),new AuthorizationReport(d.eventId,d.playerId,d.status,d.occurredAt,d.platform,d.platformDetail)),"apiVersion",1);});}
    private void signals(HttpExchange e)throws IOException{handle(e,()->{var d=read(e,NetworkDto.class);return Map.of("eventId",service.reportNetworkRisk(version(e),key(e),authenticator.authenticate(e.getRequestHeaders()),new NetworkRiskReport(d.eventId,d.playerId,d.observedAt,d.ipSignals,d.deviceSignals)),"apiVersion",1);});}
    private void tableRisk(HttpExchange e)throws IOException{handle(e,()->{var d=read(e,TableDto.class);return service.assessTable(version(e),key(e),authenticator.authenticate(e.getRequestHeaders()),new TableRiskRequest(d.tableId,d.participantIds,d.locations,d.evaluatedAt));});}
    private void handle(HttpExchange e,Action action)throws IOException{try{if(!"POST".equals(e.getRequestMethod()))throw new HttpFailure(405,"METHOD_NOT_ALLOWED","POST required");Object result=action.run();reply(e,200,result);}catch(HttpFailure x){reply(e,x.status,error(x.code,x.getMessage()));}catch(LocationServiceException x){reply(e,status(x.code()),error(x.code().name(),x.getMessage()));}catch(JsonProcessingException|IllegalArgumentException x){reply(e,400,error(LocationErrorCode.INVALID_ARGUMENT.name(),"invalid JSON request"));}catch(Exception x){reply(e,500,error(LocationErrorCode.PERSISTENCE_FAILURE.name(),"location service unavailable"));}}
    private <T>T read(HttpExchange e,Class<T> type)throws IOException{String ct=e.getRequestHeaders().getFirst("Content-Type");if(ct==null||!ct.toLowerCase(Locale.ROOT).startsWith("application/json"))throw new HttpFailure(415,"UNSUPPORTED_MEDIA_TYPE","application/json required");long length=parseLength(e.getRequestHeaders().getFirst("Content-Length"));if(length>MAX_BODY_BYTES)throw new HttpFailure(413,"PAYLOAD_TOO_LARGE","request body exceeds 65536 bytes");byte[] body=e.getRequestBody().readNBytes(MAX_BODY_BYTES+1);if(body.length>MAX_BODY_BYTES)throw new HttpFailure(413,"PAYLOAD_TOO_LARGE","request body exceeds 65536 bytes");return json.readValue(body,type);}
    private static long parseLength(String v){try{return v==null?-1:Long.parseLong(v);}catch(NumberFormatException x){throw new HttpFailure(400,"INVALID_CONTENT_LENGTH","invalid Content-Length");}}
    private static int version(HttpExchange e){String v=e.getRequestHeaders().getFirst("X-Api-Version");if(v==null)return 1;try{return Integer.parseInt(v);}catch(NumberFormatException x){throw new LocationServiceException(LocationErrorCode.UNSUPPORTED_API_VERSION,"X-Api-Version must be 1");}}
    private static String key(HttpExchange e){String k=e.getRequestHeaders().getFirst("Idempotency-Key");if(k==null)throw new LocationServiceException(LocationErrorCode.INVALID_ARGUMENT,"Idempotency-Key is required");return k;}
    private static int status(LocationErrorCode c){return switch(c){case UNAUTHENTICATED->401;case FORBIDDEN->403;case UNSUPPORTED_API_VERSION->426;case IDEMPOTENCY_KEY_REUSED,REQUEST_IN_PROGRESS,STALE_EVENT->409;case PARTICIPANT_NOT_FOUND,INVALID_ARGUMENT->400;case PERSISTENCE_FAILURE->503;};}
    private static Map<String,Object> error(String code,String message){return Map.of("apiVersion",1,"error",Map.of("code",code,"message",message));}
    private void reply(HttpExchange e,int status,Object value)throws IOException{byte[] b=json.writeValueAsBytes(value);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,b.length);try(OutputStream out=e.getResponseBody()){out.write(b);}finally{e.close();}}
    @FunctionalInterface private interface Action{Object run()throws Exception;}
    private static final class HttpFailure extends RuntimeException{final int status;final String code;HttpFailure(int status,String code,String message){super(message);this.status=status;this.code=code;}}
    private record AuthorizationDto(String eventId,long playerId,AuthorizationStatus status,Instant occurredAt,String platform,String platformDetail){}
    private record NetworkDto(String eventId,long playerId,Instant observedAt,List<RiskSignal> ipSignals,List<RiskSignal> deviceSignals){}
    private record TableDto(String tableId,List<Long> participantIds,List<ParticipantLocation> locations,Instant evaluatedAt){}
}
