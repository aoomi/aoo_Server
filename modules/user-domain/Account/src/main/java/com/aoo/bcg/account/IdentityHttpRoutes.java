package com.aoo.bcg.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import javax.sql.DataSource;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Unified identity administration and signed trusted-device PIN endpoints. */
public final class IdentityHttpRoutes {
    private static final String PREFIX="/api/v2/account/identity";
    private final DataSource source;private final UnifiedIdentityService identities;private final TrustedDevicePinService pins;private final JdbcAccountSessionService sessions;private final ObjectMapper json;private final byte[] operatorToken;private final byte[] secondFactorToken;
    public IdentityHttpRoutes(DataSource source,UnifiedIdentityService identities,TrustedDevicePinService pins,JdbcAccountSessionService sessions,ObjectMapper json,String operatorToken,String secondFactorToken){this.source=Objects.requireNonNull(source);this.identities=Objects.requireNonNull(identities);this.pins=Objects.requireNonNull(pins);this.sessions=Objects.requireNonNull(sessions);this.json=Objects.requireNonNull(json);this.operatorToken=required(operatorToken,"operator token").getBytes(StandardCharsets.UTF_8);this.secondFactorToken=required(secondFactorToken,"second factor token").getBytes(StandardCharsets.UTF_8);}
    public void mount(HttpServer server){server.createContext(PREFIX,this::handle);}
    private void handle(HttpExchange e)throws IOException{try{if(!"1".equals(e.getRequestHeaders().getFirst("X-Aoo-Api-Version")))throw new IllegalArgumentException("version required");Map<String,String> f=form(e);String path=e.getRequestURI().getPath().substring(PREFIX.length());Object out;JdbcAccountSessionService.Client client=client(e);
        if(path.equals("/pin/challenge")){out=pins.challenge(Long.parseLong(f.get("accountId")),client.deviceId(),Long.parseLong(f.get("sequence")),client.ip());}
        else if(path.equals("/pin/login")){long id=pins.verify(f.get("challengeId"),f.get("challenge"),Long.parseLong(f.get("sequence")),required(f.get("pin"),"pin").charAt(0),Base64.getDecoder().decode(f.get("signature")),client.ip());out=sessions.loginTrustedDevice(id,client,true);}
        else{UnifiedIdentityService.AdminContext admin=admin(e,f,path);out=switch(path){
            case "/admin/display/change"->{identities.changeDisplayId(Long.parseLong(f.get("accountId")),f.get("displayId"),admin);yield Map.of("status","changed");}
            case "/admin/bind"->{identities.bind(Long.parseLong(f.get("accountId")),UnifiedIdentityService.Type.valueOf(f.get("identityType")),f.get("value"),Boolean.parseBoolean(f.getOrDefault("verified","true")),admin);yield Map.of("status","bound");}
            case "/admin/unbind"->{identities.unbind(Long.parseLong(f.get("accountId")),UnifiedIdentityService.Type.valueOf(f.get("identityType")),f.get("value"),admin);yield Map.of("status","unbound");}
            case "/admin/migrate"->{identities.migrateIdentity(Long.parseLong(f.get("sourceAccountId")),Long.parseLong(f.get("targetAccountId")),UnifiedIdentityService.Type.valueOf(f.get("identityType")),f.get("value"),f.get("sourceProof"),f.get("targetProof"),admin);yield Map.of("status","migrated");}
            case "/admin/display/owners"->identities.owners(f.get("displayId"));
            case "/admin/session/revoke"->{sessions.revokeSessions(Long.parseLong(f.get("accountId")),admin.reason(),admin.sourceIp());yield Map.of("status","revoked");}
            case "/admin/pin/bind"->{pins.bind(Long.parseLong(f.get("accountId")),f.get("deviceId"),required(f.get("pin"),"pin").charAt(0),f.get("publicKeyPem"),true);yield Map.of("status","bound");}
            default->throw new IllegalArgumentException("unknown identity route");};}
        reply(e,200,json.writeValueAsString(out));
    }catch(UnifiedIdentityService.Conflict x){reply(e,409,"{\"error\":\"identity conflict\"}");}catch(SecurityException x){reply(e,401,"{\"error\":\"unauthorized\"}");}catch(IllegalArgumentException x){reply(e,400,"{\"error\":\"invalid request\"}");}catch(Exception x){reply(e,503,"{\"error\":\"identity service unavailable\"}");}}
    private UnifiedIdentityService.AdminContext admin(HttpExchange e,Map<String,String> f,String path)throws SQLException{constant(e,"X-Account-Operator-Token",operatorToken);constant(e,"X-Admin-Second-Factor",secondFactorToken);long operator=Long.parseLong(required(e.getRequestHeaders().getFirst("X-Admin-Id"),"admin id"));String permission=path.equals("/admin/migrate")?"account.identity.migrate":path.equals("/admin/display/owners")?"account.identity.read":"account.identity.mutate";if(!allowed(operator,permission))throw new SecurityException("forbidden");String request=required(e.getRequestHeaders().getFirst("X-Request-Id"),"request id"),trace=required(e.getRequestHeaders().getFirst("X-Trace-Id"),"trace id"),reason=required(e.getRequestHeaders().getFirst("X-Admin-Reason"),"reason");return new UnifiedIdentityService.AdminContext(operator,reason,ip(e),trace,request,true,Set.of(permission));}
    private boolean allowed(long operator,String permission)throws SQLException{String sql="SELECT 1 FROM admin_operator_permission WHERE operator_id=? AND permission_code=? AND enabled=1 UNION ALL SELECT 1 FROM admin_operator_role ar JOIN admin_role r ON r.role_code=ar.role_code AND r.enabled=1 JOIN admin_role_permission p ON p.role_code=ar.role_code WHERE ar.operator_id=? AND ar.enabled=1 AND p.permission_code=? LIMIT 1";try(Connection c=source.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,operator);p.setString(2,permission);p.setLong(3,operator);p.setString(4,permission);try(ResultSet r=p.executeQuery()){return r.next();}}}
    private static void constant(HttpExchange e,String header,byte[] wanted){String value=e.getRequestHeaders().getFirst(header);if(value==null||!MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8),wanted))throw new SecurityException("unauthorized");}
    private static JdbcAccountSessionService.Client client(HttpExchange e){return new JdbcAccountSessionService.Client(required(e.getRequestHeaders().getFirst("X-Device-Id"),"device"),required(e.getRequestHeaders().getFirst("X-Client-Channel"),"channel"),required(e.getRequestHeaders().getFirst("X-Client-Version"),"version"),ip(e));}
    private static String ip(HttpExchange e){return e.getRemoteAddress()==null?"unknown":e.getRemoteAddress().getAddress().getHostAddress();}
    private static Map<String,String> form(HttpExchange e)throws IOException{Map<String,String> out=new HashMap<>();String body=new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);for(String pair:body.split("&")){int i=pair.indexOf('=');if(i>0)out.put(URLDecoder.decode(pair.substring(0,i),StandardCharsets.UTF_8),URLDecoder.decode(pair.substring(i+1),StandardCharsets.UTF_8));}return out;}
    private static String required(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" required");return value;}
    private static void reply(HttpExchange e,int status,String body)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json;charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,b.length);e.getResponseBody().write(b);e.close();}
}
