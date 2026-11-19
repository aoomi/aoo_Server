package com.aoo.bcg.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Authenticated v1 routes; cursored feeds provide durable offline catch-up. */
public final class SocialHttpRoutes {
    private final SocialService service;private final SocialRequestAuthenticator auth;private final ObjectMapper json;
    public SocialHttpRoutes(SocialService service,SocialRequestAuthenticator auth,ObjectMapper json){this.service=Objects.requireNonNull(service);this.auth=Objects.requireNonNull(auth);this.json=Objects.requireNonNull(json);}
    public void mount(HttpServer server){server.createContext("/health/social",e->reply(e,200,Map.of("status","UP","persistence","jdbc")));server.createContext("/v1/social",this::social);server.createContext("/v1/notifications",this::notifications);server.createContext("/v1/mail",this::mail);server.createContext("/v1/notices",this::notices);server.createContext("/v1/red-dots",this::redDots);}
    private void social(HttpExchange e)throws IOException{handle(e,(p,f)->switch(text(f,"action")){
        case "friendRequest"->service.request(p.accountId(),number(f,"recipientId"),idempotency(e));
        case "friendAccept"->service.decide(p.accountId(),number(f,"requestId"),true,idempotency(e));
        case "friendReject"->service.decide(p.accountId(),number(f,"requestId"),false,idempotency(e));
        case "friendDelete"->{service.delete(p.accountId(),number(f,"accountId"));yield Map.of("deleted",true);}
        case "friendList"->service.friends(p.accountId());
        case "friendRequests"->service.pendingRequests(p.accountId());
        case "block"->{service.block(p.accountId(),number(f,"accountId"));yield Map.of("blocked",true);}
        case "unblock"->{service.unblock(p.accountId(),number(f,"accountId"));yield Map.of("blocked",false);}
        case "presenceSet"->{service.presence(p.accountId(),text(f,"state"),nullableLong(f,"roomId"),textOr(f,"visibility","FRIENDS"));yield Map.of("updated",true);}
        case "presenceGet"->service.presence(p.accountId(),number(f,"accountId"));
        case "roomInvite"->service.roomInvite(p.accountId(),number(f,"recipientId"),number(f,"roomId"),idempotency(e));
        default->throw new IllegalArgumentException("unknown social action");});}
    private void notifications(HttpExchange e)throws IOException{handle(e,(p,f)->switch(text(f,"action")){
        case "list"->service.notifications(p.accountId(),numberOr(f,"cursor",0),(int)numberOr(f,"limit",50));
        case "read"->Map.of("readCursor",service.read(p.accountId(),"NOTIFICATION",number(f,"cursor")));
        case "systemSend"->service.systemMessage(p,number(f,"recipientId"),idempotency(e),text(f,"payload"));
        default->throw new IllegalArgumentException("unknown notification action");});}
    private void mail(HttpExchange e)throws IOException{handle(e,(p,f)->switch(text(f,"action")){
        case "list"->service.mails(p.accountId(),numberOr(f,"cursor",0),(int)numberOr(f,"limit",50));
        case "detail"->service.mail(p.accountId(),number(f,"mailId"));
        case "read"->Map.of("readCursor",service.read(p.accountId(),"MAIL",number(f,"cursor")));
        case "systemSend"->service.systemMail(p,number(f,"recipientId"),idempotency(e),text(f,"subject"),text(f,"body"));
        default->throw new IllegalArgumentException("unknown mail action");});}
    private void notices(HttpExchange e)throws IOException{handle(e,(p,f)->switch(text(f,"action")){
        case "list"->service.notices(p.accountId(),numberOr(f,"cursor",0),(int)numberOr(f,"limit",50));
        case "read"->Map.of("readCursor",service.read(p.accountId(),"NOTICE",number(f,"cursor")));
        default->throw new IllegalArgumentException("unknown notice action");});}
    private void redDots(HttpExchange e)throws IOException{handle(e,(p,f)->{if(!"summary".equals(text(f,"action")))throw new IllegalArgumentException("unknown red-dot action");return service.redDots(p.accountId());});}
    @SuppressWarnings("unchecked")private void handle(HttpExchange e,Action action)throws IOException{String trace=trace(e);try{if(!"POST".equals(e.getRequestMethod()))throw new Api(405,"METHOD_NOT_ALLOWED","POST required");if(!"1".equals(e.getRequestHeaders().getFirst("X-Aoo-Api-Version")))throw new Api(426,"API_VERSION_REQUIRED","X-Aoo-Api-Version: 1 required");var principal=auth.authenticate(e.getRequestHeaders().getFirst("Authorization"),header(e,"X-Device-Id"),header(e,"X-Client-Channel"),header(e,"X-Client-Version"),e.getRemoteAddress().getAddress().getHostAddress());Map<String,Object> f=json.readValue(e.getRequestBody(),Map.class);reply(e,200,Map.of("code","OK","data",action.run(principal,f),"traceId",trace,"timestamp",System.currentTimeMillis()));}catch(Api x){reply(e,x.status,Map.of("code",x.code,"message",x.getMessage(),"data",Map.of(),"traceId",trace,"timestamp",System.currentTimeMillis()));}catch(SecurityException x){reply(e,403,Map.of("code","FORBIDDEN","message",x.getMessage(),"data",Map.of(),"traceId",trace,"timestamp",System.currentTimeMillis()));}catch(IllegalArgumentException x){reply(e,400,Map.of("code","INVALID_REQUEST","message",String.valueOf(x.getMessage()),"data",Map.of(),"traceId",trace,"timestamp",System.currentTimeMillis()));}catch(IllegalStateException x){reply(e,409,Map.of("code","CONFLICT","message",String.valueOf(x.getMessage()),"data",Map.of(),"traceId",trace,"timestamp",System.currentTimeMillis()));}}
    private String idempotency(HttpExchange e){String value=e.getRequestHeaders().getFirst("Idempotency-Key");if(value==null||!value.matches("[A-Za-z0-9_.:-]{1,128}"))throw new IllegalArgumentException("valid Idempotency-Key required");return value;}
    private static String text(Map<String,Object> f,String k){Object v=f.get(k);if(!(v instanceof String s)||s.isBlank())throw new IllegalArgumentException(k+" required");return s;}
    private static String textOr(Map<String,Object> f,String k,String d){Object v=f.get(k);return v==null?d:String.valueOf(v);}
    private static long number(Map<String,Object> f,String k){Object v=f.get(k);if(v instanceof Number n)return n.longValue();throw new IllegalArgumentException(k+" required");}
    private static long numberOr(Map<String,Object> f,String k,long d){return f.get(k)==null?d:number(f,k);}
    private static Long nullableLong(Map<String,Object> f,String k){return f.get(k)==null?null:number(f,k);}
    private void reply(HttpExchange e,int status,Object body)throws IOException{byte[] b=json.writeValueAsBytes(body);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,b.length);try(var out=e.getResponseBody()){out.write(b);}}
    private static String header(HttpExchange e,String name){String value=e.getRequestHeaders().getFirst(name);if(value==null||value.isBlank())throw new SecurityException(name+" required");return value;}
    private static String trace(HttpExchange e){String value=e.getRequestHeaders().getFirst("X-Trace-Id");return value==null||value.isBlank()?java.util.UUID.randomUUID().toString():value;}
    @FunctionalInterface private interface Action{Object run(SocialPrincipal p,Map<String,Object> fields);}
    private static final class Api extends RuntimeException{final int status;final String code;Api(int s,String c,String m){super(m);status=s;code=c;}}
}
