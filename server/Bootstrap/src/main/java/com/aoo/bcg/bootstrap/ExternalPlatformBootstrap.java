package com.aoo.bcg.bootstrap;

import com.aoo.bcg.account.callback.*;
import com.aoo.bcg.account.wechat.*;
import com.aoo.bcg.account.HttpGatewaySessionReplacementNotifier;
import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.aoo.bcg.location.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;

/** Production assembly for external platform, signed callback and location-risk routes. */
public final class ExternalPlatformBootstrap {
    private ExternalPlatformBootstrap(){}
    public static HttpServer start()throws Exception{
        DataSource source=new DriverManagerDataSource(required("external.database.url","EXTERNAL_DATABASE_URL"),required("external.database.user","EXTERNAL_DATABASE_USER"),required("external.database.password","EXTERNAL_DATABASE_PASSWORD"));
        try(var c=source.getConnection();var p=c.prepareStatement("SELECT 1")){p.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();Clock clock=Clock.systemUTC();
        var oauth=new WeChatOAuthClient(required("wechat.app.id","WECHAT_APP_ID"),required("wechat.app.secret","WECHAT_APP_SECRET").toCharArray());
        var replacements=new HttpGatewaySessionReplacementNotifier(URI.create(required("account.gateway.internal.url","ACCOUNT_GATEWAY_INTERNAL_URL")),required("account.gateway.internal.token","ACCOUNT_GATEWAY_INTERNAL_TOKEN"),json);
        var sessions=new JdbcWeChatSessionService(source,oauth,clock,replacements);
        var bindings=new WeChatBindingService(oauth,new JdbcWeChatBindingRepository(source),sessions,clock);
        var verifier=new WeChatCallbackVerifier(required("wechat.callback.token","WECHAT_CALLBACK_TOKEN").toCharArray(),clock,Duration.ofMinutes(5),new JdbcWeChatReplayGuard(source));
        var store=new JdbcLocationPersistence(source,json);
        var policy=new LocationRiskPolicy(number("location.proximity.meters","LOCATION_PROXIMITY_METERS",50),number("location.max.accuracy.meters","LOCATION_MAX_ACCURACY_METERS",100),Duration.ofMinutes(5),60,25,20,Duration.ofHours(24));
        var location=new LocationRiskService(store,store,store,store,policy,clock);
        String token=required("location.bearer.token","LOCATION_BEARER_TOKEN");
        var identity=new LocationRequestAuthenticator.Identity(required("location.tenant","LOCATION_TENANT_ID"),"external-platform-api",Set.of("location.authorization.write","location.signal.write","location.risk.assess"));
        var auth=new LocationRequestAuthenticator(Map.of(token,identity),value("location.proxy.secret","LOCATION_PROXY_SECRET",""),clock);
        HttpServer server=HttpServer.create(new InetSocketAddress(Integer.parseInt(value("external.http.port","EXTERNAL_HTTP_PORT","8106"))),128);
        new WeChatHttpRoutes(sessions,bindings,sessions,verifier,new JdbcWeChatCallbackHandler(source),json).mount(server);
        new LocationHttpRoutes(location,auth,json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();return server;
    }
    private static double number(String p,String e,double f){return Double.parseDouble(value(p,e,Double.toString(f)));}
    private static String required(String p,String e){String v=value(p,e,null);if(v==null||v.isBlank())throw new IllegalStateException(e+" is required");return v;}
    private static String value(String p,String e,String f){String v=System.getProperty(p);return v!=null?v:System.getenv().getOrDefault(e,f);}
}
