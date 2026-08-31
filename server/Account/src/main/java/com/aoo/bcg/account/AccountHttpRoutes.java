package com.aoo.bcg.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Public account API plus secret-authenticated moderation endpoints. */
public final class AccountHttpRoutes {
    static final String PREFIX="/api/v2/account";
    private AccountHttpRoutes(){}
    public static void mount(HttpServer server,JdbcAccountSessionService service,ObjectMapper json,String operatorToken){
        Objects.requireNonNull(operatorToken);if(operatorToken.isBlank())throw new IllegalArgumentException("operator token required");
        server.createContext("/health/account",e->reply(e,200,"{\"status\":\"UP\",\"persistence\":\"jdbc\"}"));
        server.createContext(PREFIX,e->handle(e,service,json,operatorToken));
    }
    private static void handle(HttpExchange e,JdbcAccountSessionService s,ObjectMapper json,String operatorToken)throws IOException{try{
        if(!"1".equals(e.getRequestHeaders().getFirst("X-Aoo-Api-Version"))){reply(e,426,"{\"error\":\"X-Aoo-Api-Version: 1 required\"}");return;}
        if(!"POST".equals(e.getRequestMethod())&&!"GET".equals(e.getRequestMethod())){reply(e,405,"{\"error\":\"method not allowed\"}");return;}
        String path=e.getRequestURI().getPath().substring(PREFIX.length());Map<String,String> f=form(e);JdbcAccountSessionService.Client client=client(e,f);Object out;
        switch(path){
            case "/register"->out=s.register(f.get("login"),chars(f,"password"),f.get("recovery"),client);
            case "/guest/register"->out=s.registerGuest(f.get("credential"),client);
            case "/login"->out=s.login(f.getOrDefault("identity",f.get("login")),chars(f,"password"),client,single(f));
            case "/guest/login"->out=s.loginGuest(f.get("credential"),client,single(f));
            case "/guest/upgrade"->{var p=principal(e,s,client);s.upgrade(p.accountId(),f.get("login"),chars(f,"password"),f.get("recovery"),client);out=Map.of("status","upgraded");}
            case "/token/refresh"->out=s.refresh(f.get("refreshToken"),client);
            case "/logout"->{s.logout(bearer(e),client);out=Map.of("status","logged_out");}
            case "/password"->{var p=principal(e,s,client);s.changePassword(p.accountId(),chars(f,"currentPassword"),chars(f,"newPassword"),client);out=Map.of("status","changed");}
            case "/recover"->{s.recover(f.get("recovery"),chars(f,"newPassword"),client);out=Map.of("status","recovered");}
            case "/session"->out=principal(e,s,client);
            case "/audit"->{var p=principal(e,s,client);out=s.audit(p.accountId());}
            case "/operator/ban"->{operator(e,operatorToken);long id=number(f,"accountId");s.ban(id,Instant.parse(f.get("until")),f.get("reason"),client.ip());out=Map.of("status","banned");}
            case "/operator/revoke"->{operator(e,operatorToken);long id=number(f,"accountId");s.revokeSessions(id,f.getOrDefault("reason","operator"),client.ip());out=Map.of("status","revoked");}
            default->{reply(e,404,"{\"error\":\"not found\"}");return;}
        }reply(e,200,json.writeValueAsString(out));
    }catch(JdbcAccountSessionService.Unauthorized x){reply(e,401,error(x));}catch(JdbcAccountSessionService.Forbidden x){reply(e,403,error(x));}catch(JdbcAccountSessionService.Conflict x){reply(e,409,error(x));}catch(IllegalArgumentException x){reply(e,400,error(x));}catch(IllegalStateException x){reply(e,503,error(x));}}
    private static JdbcAccountSessionService.Principal principal(HttpExchange e,JdbcAccountSessionService s,JdbcAccountSessionService.Client c){return s.authorize(bearer(e),c);} private static String bearer(HttpExchange e){String h=e.getRequestHeaders().getFirst("Authorization");if(h==null||!h.startsWith("Bearer "))throw new JdbcAccountSessionService.Unauthorized("bearer token required");return h.substring(7);}
    private static void operator(HttpExchange e,String wanted){String got=e.getRequestHeaders().getFirst("X-Account-Operator-Token");if(got==null||!java.security.MessageDigest.isEqual(got.getBytes(StandardCharsets.UTF_8),wanted.getBytes(StandardCharsets.UTF_8)))throw new JdbcAccountSessionService.Forbidden("operator authentication failed");}
    private static JdbcAccountSessionService.Client client(HttpExchange e,Map<String,String> f){String ip=e.getRemoteAddress()==null?"unknown":e.getRemoteAddress().getAddress().getHostAddress();return new JdbcAccountSessionService.Client(header(e,"X-Device-Id"),header(e,"X-Client-Channel"),header(e,"X-Client-Version"),ip);}
    private static String header(HttpExchange e,String n){String v=e.getRequestHeaders().getFirst(n);if(v==null||v.isBlank())throw new IllegalArgumentException(n+" required");return v;} private static boolean single(Map<String,String> f){return !"multi".equalsIgnoreCase(f.getOrDefault("mode","single"));}private static char[] chars(Map<String,String> f,String n){String v=f.get(n);if(v==null)throw new IllegalArgumentException(n+" required");return v.toCharArray();}private static long number(Map<String,String> f,String n){return Long.parseLong(f.getOrDefault(n,""));}
    private static Map<String,String> form(HttpExchange e)throws IOException{Map<String,String> out=new HashMap<>();if("GET".equals(e.getRequestMethod()))return out;String body=new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);for(String pair:body.split("&")){int i=pair.indexOf('=');if(i>0)out.put(decode(pair.substring(0,i)),decode(pair.substring(i+1)));}return out;}private static String decode(String v){return URLDecoder.decode(v,StandardCharsets.UTF_8);}private static String error(Exception x){return "{\"error\":"+quote(String.valueOf(x.getMessage()))+"}";}private static String quote(String v){return "\""+v.replace("\\","\\\\").replace("\"","\\\"")+"\"";}private static void reply(HttpExchange e,int status,String body)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,b.length);e.getResponseBody().write(b);e.close();}
}
