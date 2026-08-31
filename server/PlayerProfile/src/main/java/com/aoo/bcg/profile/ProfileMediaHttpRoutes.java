package com.aoo.bcg.profile;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

/** Bearer-authenticated avatar composition. It accepts neither arbitrary URLs nor unverified profile references. */
public final class ProfileMediaHttpRoutes {
    private static final int MAX_JSON=32*1024,MAX_PART=5*1024*1024;
    private final ProfileMediaOrchestrator flow;private final PlayerProfileHttpRoutes.Authenticator auth;private final ObjectMapper json;
    public ProfileMediaHttpRoutes(ProfileMediaOrchestrator flow,PlayerProfileHttpRoutes.Authenticator auth,ObjectMapper json){this.flow=Objects.requireNonNull(flow);this.auth=Objects.requireNonNull(auth);this.json=Objects.requireNonNull(json);}
    public void mount(HttpServer server){server.createContext("/v1/player-profile/avatar",this::handle);}
    private void handle(HttpExchange e)throws IOException{try{requireVersion(e);long owner=caller(e);String tail=e.getRequestURI().getPath().substring("/v1/player-profile/avatar".length());
        if("/uploads".equals(tail)&&"POST".equals(e.getRequestMethod())){JsonNode n=json.readTree(read(e,MAX_JSON));var out=flow.initiate(owner,text(n,"mimeType"),number(n,"byteSize"),text(n,"sha256"));reply(e,out.deduplicated()?200:201,Map.of("code","OK","data",out));return;}
        String[] p=tail.startsWith("/")?tail.substring(1).split("/"):new String[0];
        if(p.length==4&&"uploads".equals(p[0])&&"parts".equals(p[2])&&"PUT".equals(e.getRequestMethod())){flow.upload(owner,p[1],Integer.parseInt(p[3]),read(e,MAX_PART));reply(e,200,Map.of("code","OK","data",Map.of("partNumber",Integer.parseInt(p[3]))));return;}
        if("/complete".equals(tail)&&"POST".equals(e.getRequestMethod())){JsonNode n=json.readTree(read(e,MAX_JSON));String ticket=n.hasNonNull("ticketId")?n.get("ticketId").asText():null;Long asset=n.hasNonNull("assetId")?number(n,"assetId"):null;reply(e,200,Map.of("code","OK","data",flow.complete(owner,ticket,asset,number(n,"expectedVersion"))));return;}
        fail(404,"NOT_FOUND","profile media route not found");
    }catch(PlayerProfileRepository.Conflict x){error(e,409,"PROFILE_VERSION_CONFLICT",x);}catch(PlayerProfileRepository.TooFrequent x){e.getResponseHeaders().set("Retry-After",String.valueOf(x.retryAfterSeconds()));error(e,429,"PROFILE_RATE_LIMITED",x);}catch(SecurityException x){error(e,403,"FORBIDDEN",x);}catch(IllegalArgumentException x){error(e,400,"INVALID_REQUEST",x);}catch(IllegalStateException x){error(e,409,"UPLOAD_STATE_CONFLICT",x);}catch(Api x){error(e,x.status,x.code,x);}}
    private long caller(HttpExchange e){String h=e.getRequestHeaders().getFirst("Authorization");if(h==null||!h.startsWith("Bearer "))throw new SecurityException("bearer token required");String ip=e.getRemoteAddress()==null?"unknown":e.getRemoteAddress().getAddress().getHostAddress();return auth.authenticate(h.substring(7),header(e,"X-Device-Id"),header(e,"X-Client-Channel"),header(e,"X-Client-Version"),ip);}
    private static String header(HttpExchange e,String k){String v=e.getRequestHeaders().getFirst(k);if(v==null||v.isBlank())throw new IllegalArgumentException(k+" required");return v;}private static void requireVersion(HttpExchange e){if(!"1".equals(e.getRequestHeaders().getFirst("X-API-Version")))fail(426,"API_VERSION_REQUIRED","X-API-Version: 1 required");}
    private static byte[] read(HttpExchange e,int max)throws IOException{try(var in=e.getRequestBody();var out=new ByteArrayOutputStream()){byte[] b=new byte[8192];for(int n;(n=in.read(b))>=0;){if(out.size()+n>max)fail(413,"PAYLOAD_TOO_LARGE","request body too large");out.write(b,0,n);}return out.toByteArray();}}
    private static long number(JsonNode n,String k){if(n==null||!n.has(k)||!n.get(k).canConvertToLong())throw new IllegalArgumentException(k+" required");return n.get(k).longValue();}private static String text(JsonNode n,String k){if(n==null||!n.hasNonNull(k)||n.get(k).asText().isBlank())throw new IllegalArgumentException(k+" required");return n.get(k).asText();}
    private void error(HttpExchange e,int status,String code,Exception x)throws IOException{reply(e,status,Map.of("code",code,"message",Optional.ofNullable(x.getMessage()).orElse("request failed")));}private void reply(HttpExchange e,int status,Object value)throws IOException{byte[] b=json.writeValueAsBytes(value);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,b.length);try(var out=e.getResponseBody()){out.write(b);}}private static void fail(int s,String c,String m){throw new Api(s,c,m);}private static final class Api extends RuntimeException{final int status;final String code;Api(int s,String c,String m){super(m);status=s;code=c;}}
}
