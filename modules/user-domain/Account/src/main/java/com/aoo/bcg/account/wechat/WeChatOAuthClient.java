package com.aoo.bcg.account.wechat;

import com.aoo.bcg.account.AccountSecurityService;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Production WeChat OAuth code-exchange adapter for {@link AccountSecurityService}. */
public final class WeChatOAuthClient implements AccountSecurityService.PlatformCredentialVerifier {
    public static final URI DEFAULT_ENDPOINT = URI.create("https://api.weixin.qq.com/sns/oauth2/access_token");
    private final String appId;
    private final char[] appSecret;
    private final URI endpoint;
    private final Duration timeout;
    private final Transport transport;
    private final RetryPolicy retryPolicy;
    private final Clock clock;
    private final Sleeper sleeper;
    private int consecutiveFailures;
    private Instant circuitOpenUntil;

    public WeChatOAuthClient(String appId, char[] appSecret) {
        this(appId, appSecret, DEFAULT_ENDPOINT, Duration.ofSeconds(5), new JdkTransport(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build()),RetryPolicy.production(),Clock.systemUTC(),Thread::sleep);
    }

    public WeChatOAuthClient(String appId, char[] appSecret, URI endpoint, Duration timeout, Transport transport) {
        this(appId,appSecret,endpoint,timeout,transport,RetryPolicy.production(),Clock.systemUTC(),Thread::sleep);
    }

    WeChatOAuthClient(String appId,char[] appSecret,URI endpoint,Duration timeout,Transport transport,RetryPolicy retryPolicy,Clock clock,Sleeper sleeper) {
        this.appId = text(appId, "appId");
        if (appSecret == null || appSecret.length < 16) throw new IllegalArgumentException("appSecret is too short");
        this.appSecret = appSecret.clone();
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (!"https".equalsIgnoreCase(endpoint.getScheme())) throw new IllegalArgumentException("WeChat endpoint must use HTTPS");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.retryPolicy=Objects.requireNonNull(retryPolicy,"retryPolicy");this.clock=Objects.requireNonNull(clock,"clock");this.sleeper=Objects.requireNonNull(sleeper,"sleeper");
    }

    @Override public String verify(String oneTimeCredential) {
        return exchange(oneTimeCredential).subject();
    }
    public String appId(){return appId;}

    public Ticket exchange(String code) {
        code = text(code, "code");
        if (code.length() > 512) throw new WeChatProtocolException(WeChatProtocolException.Code.INVALID_CODE, "invalid WeChat code");
        String secret = new String(appSecret);
        URI uri;
        try {
            uri = URI.create(endpoint + "?appid=" + encode(appId) + "&secret=" + encode(secret)
                    + "&code=" + encode(code) + "&grant_type=authorization_code");
        } finally {
            // The configured copy remains a char[] and is never exposed in exceptions or logs.
            secret = null;
        }
        ensureCircuitAllowsRequest();
        Response response=requestWithRetry(uri);
        if (response.statusCode() != 200) {
            recordFailure();
            throw new WeChatProtocolException(WeChatProtocolException.Code.PROVIDER_UNAVAILABLE, "WeChat OAuth returned HTTP " + response.statusCode());
        }
        Map<String, String> json;
        try { json = FlatJson.parse(response.body()); }
        catch (RuntimeException invalid) {
            throw new WeChatProtocolException(WeChatProtocolException.Code.INVALID_PROVIDER_RESPONSE, "invalid WeChat OAuth response");
        }
        if (json.containsKey("errcode") && !"0".equals(json.get("errcode"))) {
            recordSuccess();
            String error = json.get("errcode");
            var codeType = "40029".equals(error) || "40163".equals(error)
                    ? WeChatProtocolException.Code.INVALID_CODE : WeChatProtocolException.Code.PROVIDER_REJECTED;
            throw new WeChatProtocolException(codeType, "WeChat OAuth rejected the request (errcode=" + safeErrorCode(error) + ")");
        }
        String openId = required(json, "openid");
        String accessToken = required(json, "access_token");
        long expiresIn;
        try { expiresIn = Long.parseLong(required(json, "expires_in")); }
        catch (NumberFormatException invalid) { throw new WeChatProtocolException(WeChatProtocolException.Code.INVALID_PROVIDER_RESPONSE, "invalid WeChat OAuth expiry"); }
        if (expiresIn <= 0 || expiresIn > 31_536_000) throw new WeChatProtocolException(WeChatProtocolException.Code.INVALID_PROVIDER_RESPONSE, "invalid WeChat OAuth expiry");
        recordSuccess();
        return new Ticket(openId, optional(json, "unionid"), accessToken, optional(json, "refresh_token"), expiresIn, optional(json, "scope"));
    }

    private Response requestWithRetry(URI uri){
        for(int attempt=1;attempt<=retryPolicy.maxAttempts();attempt++){
            try{
                Response response=transport.get(uri,timeout);
                if(!retryable(response.statusCode())||attempt==retryPolicy.maxAttempts())return response;
            }catch(IOException unavailable){if(attempt==retryPolicy.maxAttempts()){recordFailure();throw unavailable("WeChat OAuth request failed");}}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();recordFailure();throw unavailable("WeChat OAuth request interrupted");}
            try{sleeper.sleep(retryPolicy.backoff().toMillis()*attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();recordFailure();throw unavailable("WeChat OAuth retry interrupted");}
        }
        throw new IllegalStateException("unreachable");
    }
    private static boolean retryable(int status){return status==429||status>=500&&status<=599;}
    private static WeChatProtocolException unavailable(String message){return new WeChatProtocolException(WeChatProtocolException.Code.PROVIDER_UNAVAILABLE,message);}
    private synchronized void ensureCircuitAllowsRequest(){if(circuitOpenUntil!=null){if(clock.instant().isBefore(circuitOpenUntil))throw unavailable("WeChat OAuth circuit is open");circuitOpenUntil=null;consecutiveFailures=0;}}
    private synchronized void recordFailure(){if(++consecutiveFailures>=retryPolicy.failureThreshold())circuitOpenUntil=clock.instant().plus(retryPolicy.openDuration());}
    private synchronized void recordSuccess(){consecutiveFailures=0;circuitOpenUntil=null;}

    private static String safeErrorCode(String value) { return value != null && value.matches("-?[0-9]{1,12}") ? value : "unknown"; }
    private static String required(Map<String,String> map, String key) {
        String value = map.get(key); if (value == null || value.isBlank()) throw new WeChatProtocolException(WeChatProtocolException.Code.INVALID_PROVIDER_RESPONSE, "missing WeChat OAuth field: " + key); return value;
    }
    private static String optional(Map<String,String> map, String key) { String value=map.get(key); return value==null||value.isBlank()?null:value; }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String text(String value, String name) { if(value==null||value.isBlank())throw new IllegalArgumentException(name+" is blank");return value.strip(); }

    /** unionid is preferred because it is stable across applications under the same open platform account. */
    public record Ticket(String openId, String unionId, String accessToken, String refreshToken, long expiresInSeconds, String scope) {
        public String subject() { return unionId == null ? "openid:" + openId : "unionid:" + unionId; }
    }
    public record Response(int statusCode, String body) { public Response { Objects.requireNonNull(body,"body"); } }
    @FunctionalInterface public interface Transport { Response get(URI uri, Duration timeout) throws IOException, InterruptedException; }
    @FunctionalInterface interface Sleeper { void sleep(long millis)throws InterruptedException; }
    record RetryPolicy(int maxAttempts,Duration backoff,int failureThreshold,Duration openDuration){RetryPolicy{if(maxAttempts<1||maxAttempts>5||failureThreshold<1||backoff.isNegative()||openDuration.isNegative()||openDuration.isZero())throw new IllegalArgumentException("invalid retry policy");}static RetryPolicy production(){return new RetryPolicy(3,Duration.ofMillis(100),5,Duration.ofSeconds(30));}}
    private record JdkTransport(HttpClient client) implements Transport {
        @Override public Response get(URI uri, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request=HttpRequest.newBuilder(uri).timeout(timeout).header("Accept","application/json").GET().build();
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(),response.body());
        }
    }

    /** Strict parser for WeChat's flat OAuth JSON response; rejects duplicate keys and nested values. */
    static final class FlatJson {
        static Map<String,String> parse(String source) {
            Cursor c=new Cursor(source); Map<String,String> out=new HashMap<>(); c.ws(); c.expect('{'); c.ws();
            if(c.take('}')){c.end();return out;}
            boolean complete=false;
            while(!complete){String key=c.string();c.ws();c.expect(':');c.ws();String value=c.value();if(out.putIfAbsent(key,value)!=null)throw new IllegalArgumentException("duplicate JSON key");c.ws();complete=c.take('}');if(!complete){c.expect(',');c.ws();}}
            c.end();return out;
        }
        private static final class Cursor { final String s;int p;Cursor(String s){this.s=Objects.requireNonNull(s);}
            void ws(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}void expect(char v){if(p>=s.length()||s.charAt(p++)!=v)throw new IllegalArgumentException();}
            boolean take(char v){if(p<s.length()&&s.charAt(p)==v){p++;return true;}return false;}void end(){ws();if(p!=s.length())throw new IllegalArgumentException();}
            String value(){if(p<s.length()&&s.charAt(p)=='\"')return string();int start=p;while(p<s.length()&&"0123456789-".indexOf(s.charAt(p))>=0)p++;if(start==p)throw new IllegalArgumentException();return s.substring(start,p);}
            String string(){expect('\"');StringBuilder b=new StringBuilder();while(p<s.length()){char ch=s.charAt(p++);if(ch=='\"')return b.toString();if(ch=='\\'){if(p>=s.length())throw new IllegalArgumentException();char e=s.charAt(p++);switch(e){case '\"','\\','/'->b.append(e);case 'b'->b.append('\b');case 'f'->b.append('\f');case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'u'->{if(p+4>s.length())throw new IllegalArgumentException();b.append((char)Integer.parseInt(s.substring(p,p+4),16));p+=4;}default->throw new IllegalArgumentException();}}else{if(ch<0x20)throw new IllegalArgumentException();b.append(ch);}}throw new IllegalArgumentException();}
        }
    }
}
