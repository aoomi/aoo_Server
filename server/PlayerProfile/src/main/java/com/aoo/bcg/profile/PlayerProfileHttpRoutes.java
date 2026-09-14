package com.aoo.bcg.profile;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.aoo.bcg.profile.PlayerProfileModels.*;

public final class PlayerProfileHttpRoutes {
    @FunctionalInterface public interface Authenticator { long authenticate(String bearer,String deviceId,String channel,String version,String ip); }
    private final PlayerProfileRepository repository;private final Authenticator auth;private final ObjectMapper json;
    public PlayerProfileHttpRoutes(PlayerProfileRepository repository,Authenticator auth,ObjectMapper json){this.repository=Objects.requireNonNull(repository);this.auth=Objects.requireNonNull(auth);this.json=Objects.requireNonNull(json);}
    public void mount(HttpServer server){server.createContext("/health/player-profile",e->reply(e,200,"{\"status\":\"UP\",\"persistence\":\"jdbc\"}"));server.createContext("/v1/player-profile",this::handle);}
    private void handle(HttpExchange e)throws IOException{try{
        long caller=caller(e);String path=e.getRequestURI().getPath().substring("/v1/player-profile".length());Object out;
        if("/me".equals(path)&&"GET".equals(e.getRequestMethod()))out=repository.own(caller);
        else if("/view".equals(path)&&"GET".equals(e.getRequestMethod()))out=repository.visible(caller,Long.parseLong(query(e).getOrDefault("playerId","")));
        else if("/profile".equals(path)&&"PUT".equals(e.getRequestMethod())){JsonNode n=body(e);forbidIdentityRegion(n);out=repository.updateProfile(caller,new ProfilePatch(requiredLong(n,"expectedVersion"),text(n,"nickname"),longValue(n,"avatarAssetId"),gender(n)));}
        else if("/preferences".equals(path)&&"PUT".equals(e.getRequestMethod())){JsonNode n=body(e);out=repository.updatePreferences(caller,new PreferencePatch(requiredLong(n,"expectedVersion"),text(n,"language"),bool(n,"soundEnabled"),bool(n,"musicEnabled"),bool(n,"vibrationEnabled")));}
        else if("/privacy".equals(path)&&"PUT".equals(e.getRequestMethod())){JsonNode n=body(e);forbidIdentityRegion(n);out=repository.updatePrivacy(caller,new PrivacyPatch(requiredLong(n,"expectedVersion"),bool(n,"showGender")));}
        else {reply(e,404,"{\"error\":\"not found\"}");return;}reply(e,200,json.writeValueAsString(out));
    }catch(PlayerProfileRepository.NotFound x){reply(e,404,error(x));}catch(PlayerProfileRepository.Conflict x){reply(e,409,error(x));}catch(PlayerProfileRepository.TooFrequent x){e.getResponseHeaders().set("Retry-After",String.valueOf(x.retryAfterSeconds()));reply(e,429,error(x));}catch(SecurityException x){reply(e,401,error(x));}catch(IllegalArgumentException|JsonProcessingException x){reply(e,400,error(x));}catch(IllegalStateException x){reply(e,503,error(x));}}
    private long caller(HttpExchange e){String h=e.getRequestHeaders().getFirst("Authorization");if(h==null||!h.startsWith("Bearer "))throw new SecurityException("bearer token required");String ip=e.getRemoteAddress()==null?"unknown":e.getRemoteAddress().getAddress().getHostAddress();return auth.authenticate(h.substring(7),header(e,"X-Device-Id"),header(e,"X-Client-Channel"),header(e,"X-Client-Version"),ip);}
    private static String header(HttpExchange e,String name){String value=e.getRequestHeaders().getFirst(name);if(value==null||value.isBlank())throw new IllegalArgumentException(name+" required");return value;}
    private JsonNode body(HttpExchange e)throws IOException{if(!"application/json".equalsIgnoreCase(Optional.ofNullable(e.getRequestHeaders().getFirst("Content-Type")).orElse("").split(";",2)[0].strip()))throw new IllegalArgumentException("application/json required");return json.readTree(e.getRequestBody());}
    private static void forbidIdentityRegion(JsonNode n){if(n!=null&&(n.has("region")||n.has("regionCode")||n.has("showRegion")))throw new IllegalArgumentException("region is a gameplay catalog filter and is forbidden in player identity profiles");}
    private static long requiredLong(JsonNode n,String name){if(n==null||!n.has(name)||!n.get(name).canConvertToLong())throw new IllegalArgumentException(name+" required");return n.get(name).longValue();}private static String text(JsonNode n,String name){return n!=null&&n.hasNonNull(name)?n.get(name).asText():null;}private static Long longValue(JsonNode n,String name){if(n==null||!n.hasNonNull(name))return null;if(!n.get(name).canConvertToLong())throw new IllegalArgumentException(name+" must be integer");return n.get(name).longValue();}private static Boolean bool(JsonNode n,String name){if(n==null||!n.hasNonNull(name))return null;if(!n.get(name).isBoolean())throw new IllegalArgumentException(name+" must be boolean");return n.get(name).booleanValue();}private static Gender gender(JsonNode n){String value=text(n,"gender");if(value==null)return null;try{return Gender.valueOf(value.toUpperCase(Locale.ROOT));}catch(Exception x){throw new IllegalArgumentException("invalid gender");}}
    private static Map<String,String> query(HttpExchange e){Map<String,String> out=new HashMap<>();String q=e.getRequestURI().getRawQuery();if(q==null)return out;for(String pair:q.split("&")){int i=pair.indexOf('=');if(i>0)out.put(URLDecoder.decode(pair.substring(0,i),StandardCharsets.UTF_8),URLDecoder.decode(pair.substring(i+1),StandardCharsets.UTF_8));}return out;}
    private static String error(Exception x){return "{\"error\":"+quote(Optional.ofNullable(x.getMessage()).orElse("request failed"))+"}";}private static String quote(String v){return "\""+v.replace("\\","\\\\").replace("\"","\\\"")+"\"";}private static void reply(HttpExchange e,int status,String body)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);e.close();}
}
