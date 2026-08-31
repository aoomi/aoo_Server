package com.aoo.bcg.account.wechat;

import com.aoo.bcg.account.AccountSecurityService;
import com.aoo.bcg.account.callback.WeChatCallbackVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Versioned production HTTP entry points for WeChat login, binding and signed callbacks. */
public final class WeChatHttpRoutes {
    private static final int MAX_BODY_BYTES=256*1024;
    private final LoginHandler loginHandler;private final WeChatBindingService bindings;private final RouteAuthenticator authenticator;
    private final WeChatCallbackVerifier callbacks;private final CallbackHandler callbackHandler;private final ObjectMapper json;
    public WeChatHttpRoutes(AccountSecurityService accounts,WeChatBindingService bindings,RouteAuthenticator authenticator,WeChatCallbackVerifier callbacks,CallbackHandler callbackHandler,ObjectMapper json){this((code,device,channel,version,ip,mode)->accounts.loginPlatform("wechat",code,new AccountSecurityService.ClientContext(device,channel,version,ip),AccountSecurityService.LoginMode.valueOf(mode)),bindings,authenticator,callbacks,callbackHandler,json);}
    public WeChatHttpRoutes(LoginHandler loginHandler,WeChatBindingService bindings,RouteAuthenticator authenticator,WeChatCallbackVerifier callbacks,CallbackHandler callbackHandler,ObjectMapper json){this.loginHandler=Objects.requireNonNull(loginHandler);this.bindings=Objects.requireNonNull(bindings);this.authenticator=Objects.requireNonNull(authenticator);this.callbacks=Objects.requireNonNull(callbacks);this.callbackHandler=Objects.requireNonNull(callbackHandler);this.json=Objects.requireNonNull(json);}

    public void mount(HttpServer server){Objects.requireNonNull(server).createContext("/api/v1/wechat/login",this::login);server.createContext("/api/v1/wechat/bind",this::bind);server.createContext("/api/v1/wechat/unbind",this::unbind);server.createContext("/api/v1/wechat/callback",this::callback);}
    private void login(HttpExchange ex)throws IOException{handleJson(ex,false,()->{requirePost(ex);Map<String,Object> f=body(ex);return loginHandler.login(text(f,"code"),text(f,"deviceId"),text(f,"channel"),text(f,"clientVersion"),ex.getRemoteAddress().getAddress().getHostAddress(),textOr(f,"loginMode","MULTI_DEVICE"));});}
    private void bind(HttpExchange ex)throws IOException{handleJson(ex,true,()->{requirePost(ex);String bearer=bearer(ex);long accountId=authenticator.authenticate(bearer).accountId();return bindings.bind(accountId,bearer,text(body(ex),"code"),idempotency(ex));});}
    private void unbind(HttpExchange ex)throws IOException{handleJson(ex,true,()->{requirePost(ex);String bearer=bearer(ex);long accountId=authenticator.authenticate(bearer).accountId();Map<String,Object> f=body(ex);return Map.of("unbound",bindings.unbind(accountId,bearer,number(f,"expectedVersion"),idempotency(ex)));});}
    private void callback(HttpExchange ex)throws IOException{
        try{Map<String,String> q=query(ex);String signature=required(q,"signature"),timestamp=required(q,"timestamp"),nonce=required(q,"nonce");
            if("GET".equals(ex.getRequestMethod())){String echo=callbacks.verifyUrl(signature,timestamp,nonce,required(q,"echostr"));replyText(ex,200,echo);return;}
            if(!"POST".equals(ex.getRequestMethod()))throw new Api(405,"METHOD_NOT_ALLOWED","GET or POST required");
            var verified=callbacks.verify(signature,timestamp,nonce);byte[] payload=readLimited(ex);CallbackResponse result=Objects.requireNonNull(callbackHandler.handle(verified,payload),"callback response");replyText(ex,result.status(),result.body());
        }catch(Api failure){reply(ex,failure.status,Map.of("code",failure.code,"message",failure.getMessage()));}
        catch(WeChatCallbackVerifier.CallbackException failure){reply(ex,401,Map.of("code",failure.code().name(),"message",failure.getMessage()));}
        catch(IllegalArgumentException failure){reply(ex,400,Map.of("code","INVALID_REQUEST","message",String.valueOf(failure.getMessage())));}
        catch(Exception failure){reply(ex,500,Map.of("code","CALLBACK_FAILED","message","WeChat callback processing failed"));}
    }
    private void handleJson(HttpExchange ex,boolean authenticated,Action action)throws IOException{try{if(!"1".equals(ex.getRequestHeaders().getFirst("X-API-Version")))throw new Api(426,"API_VERSION_REQUIRED","X-API-Version: 1 required");if(authenticated&&ex.getRequestHeaders().getFirst("Authorization")==null)throw new Api(401,"UNAUTHORIZED","Bearer credential required");reply(ex,200,Map.of("code","OK","apiVersion","1","data",action.run()));}
        catch(Api failure){reply(ex,failure.status,Map.of("code",failure.code,"message",failure.getMessage()));}
        catch(WeChatProtocolException failure){int status=failure.code()==WeChatProtocolException.Code.INVALID_CODE?401:503;reply(ex,status,Map.of("code",failure.code().name(),"message",failure.getMessage()));}
        catch(WeChatBindingService.BindingException failure){int status=failure.code()==WeChatBindingService.ErrorCode.UNAUTHORIZED?401:409;reply(ex,status,Map.of("code",failure.code().name(),"message",failure.getMessage()));}
        catch(AccountSecurityService.Unauthorized failure){reply(ex,401,Map.of("code","UNAUTHORIZED","message",failure.getMessage()));}
        catch(IllegalArgumentException failure){reply(ex,400,Map.of("code","INVALID_REQUEST","message",String.valueOf(failure.getMessage())));}
        catch(Exception failure){reply(ex,500,Map.of("code","INTERNAL_ERROR","message","WeChat account request failed"));}}
    private static void requirePost(HttpExchange ex){if(!"POST".equals(ex.getRequestMethod()))throw new Api(405,"METHOD_NOT_ALLOWED","POST required");}
    private String bearer(HttpExchange ex){String value=ex.getRequestHeaders().getFirst("Authorization");if(value==null||!value.startsWith("Bearer ")||value.length()<=7)throw new Api(401,"UNAUTHORIZED","Bearer credential required");return value.substring(7);}
    private String idempotency(HttpExchange ex){String value=ex.getRequestHeaders().getFirst("Idempotency-Key");if(value==null||!value.matches("[A-Za-z0-9_.:-]{8,128}"))throw new IllegalArgumentException("valid Idempotency-Key required");return value;}
    private Map<String,Object> body(HttpExchange ex)throws IOException{return json.readValue(readLimited(ex),new TypeReference<>(){});}
    private static byte[] readLimited(HttpExchange ex)throws IOException{try(var in=ex.getRequestBody();var out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int total=0,read;while((read=in.read(buffer))!=-1){total+=read;if(total>MAX_BODY_BYTES)throw new Api(413,"PAYLOAD_TOO_LARGE","request body too large");out.write(buffer,0,read);}return out.toByteArray();}}
    private static Map<String,String> query(HttpExchange ex){Map<String,String> out=new LinkedHashMap<>();String raw=ex.getRequestURI().getRawQuery();if(raw==null)return out;for(String item:raw.split("&")){int split=item.indexOf('=');String key=decode(split<0?item:item.substring(0,split)),value=decode(split<0?"":item.substring(split+1));if(out.putIfAbsent(key,value)!=null)throw new IllegalArgumentException("duplicate query parameter: "+key);}return out;}
    private static String decode(String v){return URLDecoder.decode(v,StandardCharsets.UTF_8);}
    private static String required(Map<String,String> f,String k){String v=f.get(k);if(v==null||v.isBlank())throw new IllegalArgumentException(k+" required");return v;}
    private static String text(Map<String,Object> f,String k){Object v=f.get(k);if(!(v instanceof String s)||s.isBlank())throw new IllegalArgumentException(k+" required");return s;}
    private static String textOr(Map<String,Object> f,String k,String d){Object v=f.get(k);return v==null?d:text(f,k);}
    private static long number(Map<String,Object> f,String k){Object v=f.get(k);if(v instanceof Number n)return n.longValue();throw new IllegalArgumentException(k+" required");}
    private void reply(HttpExchange ex,int status,Object value)throws IOException{byte[] bytes=json.writeValueAsBytes(value);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.getResponseHeaders().set("Cache-Control","no-store");ex.sendResponseHeaders(status,bytes.length);try(var out=ex.getResponseBody()){out.write(bytes);}}
    private static void replyText(HttpExchange ex,int status,String value)throws IOException{byte[] bytes=value.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","text/plain; charset=utf-8");ex.getResponseHeaders().set("Cache-Control","no-store");ex.sendResponseHeaders(status,bytes.length);try(var out=ex.getResponseBody()){out.write(bytes);}}
    @FunctionalInterface private interface Action{Object run()throws Exception;}
    @FunctionalInterface public interface LoginHandler{Object login(String code,String deviceId,String channel,String clientVersion,String ipAddress,String loginMode);}
    @FunctionalInterface public interface RouteAuthenticator{AuthenticatedAccount authenticate(String bearerToken);}
    @FunctionalInterface public interface CallbackHandler{CallbackResponse handle(WeChatCallbackVerifier.VerifiedCallback verified,byte[] rawBody)throws Exception;}
    public record AuthenticatedAccount(long accountId){public AuthenticatedAccount{if(accountId<=0)throw new IllegalArgumentException("invalid accountId");}}
    public record CallbackResponse(int status,String body){public CallbackResponse{if(status<200||status>599)throw new IllegalArgumentException("invalid status");Objects.requireNonNull(body);}}
    private static final class Api extends RuntimeException{final int status;final String code;Api(int status,String code,String message){super(message);this.status=status;this.code=code;}}
}
