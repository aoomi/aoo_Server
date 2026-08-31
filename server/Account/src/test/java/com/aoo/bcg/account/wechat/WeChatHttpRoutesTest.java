package com.aoo.bcg.account.wechat;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.account.AccountSecurityService;
import com.aoo.bcg.account.callback.WeChatCallbackVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class WeChatHttpRoutesTest {
    @Test void exposesVersionedLoginAndSignedCallbackWithoutDefaultSuccess()throws Exception{
        Instant now=Instant.ofEpochSecond(1787529600);var oauth=new WeChatOAuthClient("wx-app","very-secret-value".toCharArray(),WeChatOAuthClient.DEFAULT_ENDPOINT,Duration.ofSeconds(1),(uri,t)->new WeChatOAuthClient.Response(200,"{\"access_token\":\"T\",\"expires_in\":7200,\"openid\":\"OPEN\"}"));
        var clock=Clock.fixed(now,ZoneOffset.UTC);var accounts=new AccountSecurityService(clock,Map.of("wechat",oauth));
        // Login must fail until a real server-side binding exists.
        var repo=new MemoryRepo();var bindings=new WeChatBindingService(oauth,repo,(id,token)->token.equals("access-good"),clock);
        var verifier=new WeChatCallbackVerifier("callback-token-value".toCharArray(),clock,Duration.ofMinutes(5),(k,u)->true);
        var routes=new WeChatHttpRoutes(accounts,bindings,token->{if(!token.equals("access-good"))throw new SecurityException("bad token");return new WeChatHttpRoutes.AuthenticatedAccount(7);},verifier,(verified,body)->new WeChatHttpRoutes.CallbackResponse(200,"success"),new ObjectMapper());
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);routes.mount(server);server.start();
        try{var client=HttpClient.newHttpClient();URI base=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            var noVersion=client.send(HttpRequest.newBuilder(base.resolve("/api/v1/wechat/login")).POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());assertEquals(426,noVersion.statusCode());
            String login="{\"code\":\"one-time\",\"deviceId\":\"phone\",\"channel\":\"app\",\"clientVersion\":\"1.0\"}";
            var denied=client.send(HttpRequest.newBuilder(base.resolve("/api/v1/wechat/login")).header("X-API-Version","1").POST(HttpRequest.BodyPublishers.ofString(login)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(401,denied.statusCode());
            String ts=Long.toString(now.getEpochSecond()),nonce="nonce-1",sig=signature("callback-token-value",ts,nonce);String query="?signature="+sig+"&timestamp="+ts+"&nonce="+nonce+"&echostr="+URLEncoder.encode("safe-echo",StandardCharsets.UTF_8);
            var echo=client.send(HttpRequest.newBuilder(base.resolve("/api/v1/wechat/callback"+query)).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,echo.statusCode());assertEquals("safe-echo",echo.body());
        }finally{server.stop(0);}
    }
    private static String signature(String token,String timestamp,String nonce)throws Exception{String[] values={token,timestamp,nonce};Arrays.sort(values);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(String.join("",values).getBytes(StandardCharsets.UTF_8)));}
    private static final class MemoryRepo implements WeChatBindingService.Repository{final Map<Long,WeChatBindingService.Binding> values=new HashMap<>();public Optional<WeChatBindingService.Binding> findByAccountId(long id){return Optional.ofNullable(values.get(id));}public WeChatBindingService.Claim claim(WeChatBindingService.Binding value){values.put(value.accountId(),value);return new WeChatBindingService.Claim(WeChatBindingService.ClaimStatus.CREATED,value);}public boolean delete(long id,String subject,long version){return values.remove(id)!=null;}}
}
