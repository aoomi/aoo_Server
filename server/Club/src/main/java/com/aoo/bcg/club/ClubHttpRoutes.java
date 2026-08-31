package com.aoo.bcg.club;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;

/** Club-owned routes mounted only by the root Bootstrap process. */
public final class ClubHttpRoutes {
    private ClubHttpRoutes() {}
    public static void mount(HttpServer server,JdbcClubService service,ObjectMapper json){
        mount(server,service,json,null,null);
    }
    public static void mount(HttpServer server,JdbcClubService service,ObjectMapper json,ClubRoomKickCoordinator kicks,String gatewayToken){
        server.createContext("/health/club",ex->reply(ex,200,"{\"status\":\"UP\",\"persistence\":\"jdbc\"}"));
        server.createContext("/v1/clubs",ex->handle(service,json,kicks,gatewayToken,ex));
    }
    private static void handle(JdbcClubService service,ObjectMapper json,ClubRoomKickCoordinator kicks,String gatewayToken,HttpExchange ex)throws IOException{try{
        if(!"POST".equals(ex.getRequestMethod())){reply(ex,405,"{\"error\":\"method not allowed\"}");return;}
        Map<String,String> f=parse(new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));String action=f.getOrDefault("action","create");String key=ex.getRequestHeaders().getFirst("Idempotency-Key");long club=number(f,"clubId"),actor=number(f,"actorId");
        Object result=switch(action){case "create"->service.create(key,club,actor,f.get("name"));case "template"->service.saveTemplate(key,club,actor,f.get("templateId"),f.get("name"),f.get("game"),f.get("rules"));case "table"->service.createTable(key,club,actor,f.get("tableId"),f.get("templateId"));case "dissolve"->service.dissolve(key,club,actor);case "room_kick"->{authenticateGateway(ex,gatewayToken,actor);if(kicks==null)throw new IllegalStateException("room authority unavailable");yield kicks.kick(new ClubRoomKickCoordinator.Command(key,club,actor,number(f,"targetPlayerId"),number(f,"roomId"),(int)number(f,"seatId"),(int)number(f,"targetSeatId"),f.get("playVersion"),number(f,"stateVersion")));}default->throw new IllegalArgumentException("unknown action");};
        reply(ex,200,json.writeValueAsString(result));
    }catch(SecurityException e){reply(ex,403,error(e));}catch(IllegalArgumentException e){reply(ex,400,error(e));}catch(IllegalStateException e){reply(ex,409,error(e));}}
    private static long number(Map<String,String> m,String k){return Long.parseLong(m.getOrDefault(k,""));}
    private static void authenticateGateway(HttpExchange ex,String token,long actor){if(token==null||token.length()<32)throw new SecurityException("gateway credential unavailable");String h=ex.getRequestHeaders().getFirst("Authorization");byte[] supplied=h!=null&&h.startsWith("Bearer ")?h.substring(7).getBytes(StandardCharsets.UTF_8):new byte[0];if(!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied))throw new SecurityException("invalid gateway credential");if(!Long.toString(actor).equals(ex.getRequestHeaders().getFirst("X-Authenticated-Player")))throw new SecurityException("actor identity mismatch");}
    private static Map<String,String> parse(String body){Map<String,String> out=new HashMap<>();for(String pair:body.split("&")){int i=pair.indexOf('=');if(i>0)out.put(decode(pair.substring(0,i)),decode(pair.substring(i+1)));}return out;}
    private static String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
    private static String error(Exception e){return "{\"error\":\""+String.valueOf(e.getMessage()).replace("\"","\\\"")+"\"}";}
    private static void reply(HttpExchange ex,int status,String body)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.sendResponseHeaders(status,b.length);ex.getResponseBody().write(b);ex.close();}
}
