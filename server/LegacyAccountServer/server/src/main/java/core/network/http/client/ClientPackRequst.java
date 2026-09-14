package core.network.http.client;

import business.global.strategy.AbstractPackStrategy;
import business.global.strategy.PackSpringContext;
import business.account.Account;
import business.account.AccountManager;
import cenum.CommonFieldEnum;
import com.ddm.server.annotation.OperLog;
import com.ddm.server.common.utils.IpUtil;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.network.http.RequestSecurityGuard;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import com.google.gson.Gson;
import core.security.WsTicketSigner;
import core.security.AccountSessionRegistry;
import core.security.AccountSessionRegistryHolder;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import jsproto.c2s.cclass.token.AccountTokenInfo;
import jsproto.c2s.cclass.token.CreateAccountTokenInfo;

@RestController
@RequestMapping("/")
public class ClientPackRequst {
    private static final Gson GSON = new Gson();
    @Resource
    private PackSpringContext packSpringContext;
    @Resource
    private RequestSecurityGuard requestSecurityGuard;
    @Resource
    private AccountManager accountManager;
    /**
     * 客户端请求的接口
     * @throws Exception
     */
    @RequestMapping(value = "/ClientPack", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String ClientPack(HttpServletRequest request,@RequestBody String body) throws Exception {
        requestSecurityGuard.verifyClient(request, body);
        // 转json
            JsonObject resJson = JsonParser.parseString(body).getAsJsonObject();
        if (!resJson.has(CommonFieldEnum.HEAD.value())) {
            throw BizException.Of(CommonEnum.BODY_NOT_MATCH.getResultCode(),CommonEnum.BODY_NOT_MATCH.getResultMsg());
        }
        AbstractPackStrategy strategy = this.packSpringContext.getService(resJson.get(CommonFieldEnum.HEAD.value()).getAsInt());
        if (Objects.isNull(strategy)) {
            throw BizException.Of(CommonEnum.HTTP_NOTFINDPACK.getResultCode(),CommonEnum.HTTP_NOTFINDPACK.getResultMsg());
        }
        return strategy.OnReceivePack(IpUtil.getIpAddr(request),body);
    }

    @RequestMapping(value = "/JavaServerPack", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public Map<String, Object> javaServerPack(@RequestBody String body) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            JsonObject request = JsonParser.parseString(body).getAsJsonObject();
            long accountId = request.get("AccountID").getAsLong();
            String token = request.get("Token").getAsString();
            CreateAccountTokenInfo parsed = accountManager.parseAccountToken(token);
            Account account = accountManager.getAccountMap().get(accountId);
            AccountTokenInfo active = account == null ? null : account.getAccountTokenInfo();
            if (parsed.getAccountID() != accountId || active == null || !token.equals(active.getToken())) {
                throw new IllegalArgumentException("account token mismatch");
            }
            response.put("Code", 0);
            response.put("AccountID", accountId);
            response.put("ServerID", 0);
            response.put("PhoneNum", "");
        } catch (RuntimeException exception) {
            response.put("Code", CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultCode());
            response.put("AccountID", 0);
        }
        return response;
    }

    @RequestMapping(value = "/api/v2/account/dispatch", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public HttpProtocolEnvelope dispatchV2(HttpServletRequest request, @RequestBody String body) throws Exception {
        requestSecurityGuard.verifyClient(request, body);
        HttpProtocolEnvelope envelope = GSON.fromJson(body, HttpProtocolEnvelope.class);
        if (envelope == null || !"2.0".equals(envelope.protocolVersion)
                || envelope.requestId == null || envelope.requestId.isBlank()
                || envelope.traceId == null || envelope.traceId.isBlank()
                || !"req".equals(envelope.kind) || envelope.seq <= 0
                || envelope.body == null || !envelope.body.isJsonObject()) {
            throw BizException.Of(CommonEnum.BODY_NOT_MATCH.getResultCode(), CommonEnum.BODY_NOT_MATCH.getResultMsg());
        }
        String idempotencyMaterial = body + "\n" + Objects.toString(request.getHeader("Authorization"), "");
        HttpProtocolEnvelope previous = HttpProtocolIdempotency.previous(envelope.requestId, idempotencyMaterial);
        if (previous != null) return previous;
        if (!HttpProtocolIdempotency.acquire(envelope.requestId, idempotencyMaterial)) throw new IllegalStateException("HTTP request is already processing");
        try {
        if ("account.token_refresh".equals(envelope.msgId)) {
            HttpProtocolEnvelope response = refreshToken(envelope);
            HttpProtocolIdempotency.complete(envelope.requestId, idempotencyMaterial, response);
            return response;
        }
        if ("account.ws_ticket".equals(envelope.msgId)) {
            HttpProtocolEnvelope response = issueWsTicket(request, envelope);
            HttpProtocolIdempotency.complete(envelope.requestId, idempotencyMaterial, response);
            return response;
        }
        if (!"account.login_compat".equals(envelope.msgId)) {
            throw BizException.Of(CommonEnum.HTTP_NOTFINDPACK.getResultCode(), CommonEnum.HTTP_NOTFINDPACK.getResultMsg());
        }
        JsonObject requestBody = envelope.body.getAsJsonObject();
        if (!requestBody.has("action") || !requestBody.has("deviceId")
                || requestBody.get("deviceId").getAsString().isBlank() || !requestBody.has("payload")
                || !requestBody.get("payload").isJsonObject()) {
            throw BizException.Of(CommonEnum.BODY_NOT_MATCH.getResultCode(), CommonEnum.BODY_NOT_MATCH.getResultMsg());
        }
        JsonObject legacy = requestBody.getAsJsonObject("payload");
        if (!legacy.has(CommonFieldEnum.HEAD.value())) {
            throw BizException.Of(CommonEnum.BODY_NOT_MATCH.getResultCode(), CommonEnum.BODY_NOT_MATCH.getResultMsg());
        }
        AbstractPackStrategy strategy = packSpringContext.getService(legacy.get(CommonFieldEnum.HEAD.value()).getAsInt());
        if (Objects.isNull(strategy)) {
            throw BizException.Of(CommonEnum.HTTP_NOTFINDPACK.getResultCode(), CommonEnum.HTTP_NOTFINDPACK.getResultMsg());
        }
        String result = strategy.OnReceivePack(IpUtil.getIpAddr(request), GSON.toJson(legacy));
        HttpProtocolEnvelope response = new HttpProtocolEnvelope();
        response.protocolVersion = "2.0";
        response.msgId = envelope.msgId;
        response.kind = "resp";
        response.requestId = envelope.requestId;
        response.seq = envelope.seq;
        response.traceId = envelope.traceId;
        response.timestamp = System.currentTimeMillis();
        response.code = 0;
        response.message = "success";
        JsonObject responseBody = new JsonObject();
        responseBody.add("payload", JsonParser.parseString(result));
        if (responseBody.get("payload").isJsonObject() && responseBody.getAsJsonObject("payload").has("AccountID")) {
            long accountId = responseBody.getAsJsonObject("payload").get("AccountID").getAsLong();
            responseBody.addProperty("wsTicket", WsTicketSigner.issue(accountId));
            String accessToken = extractAccessToken(responseBody);
            if (accessToken != null) {
                AccountSessionRegistry.SessionTokens sessionTokens = AccountSessionRegistryHolder.required()
                        .bootstrap(accountId, requestBody.get("deviceId").getAsString(), accessToken, Duration.ofDays(30));
                responseBody.addProperty("sessionId", sessionTokens.sessionId());
                responseBody.addProperty("refreshToken", sessionTokens.refreshToken());
            }
        }
        response.body = responseBody;
        HttpProtocolIdempotency.complete(envelope.requestId, idempotencyMaterial, response);
        return response;
        } catch (Exception exception) {
            HttpProtocolIdempotency.release(envelope.requestId, idempotencyMaterial);
            throw exception;
        }
    }

    private HttpProtocolEnvelope issueWsTicket(HttpServletRequest request, HttpProtocolEnvelope envelope) {
        JsonObject data = envelope.body.getAsJsonObject();
        if (!data.has("deviceId") || data.get("deviceId").getAsString().isBlank()) {
            throw BizException.Of(CommonEnum.BODY_NOT_MATCH.getResultCode(), CommonEnum.BODY_NOT_MATCH.getResultMsg());
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BizException.Of(CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultCode(), CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultMsg());
        }
        String token = authorization.substring("Bearer ".length()).trim();
        CreateAccountTokenInfo parsed = accountManager.parseAccountToken(token);
        Account account = accountManager.getAccountMap().get(parsed.getAccountID());
        AccountTokenInfo activeToken = account == null ? null : account.getAccountTokenInfo();
        String deviceId = data.get("deviceId").getAsString();
        AccountSessionRegistry sessions = AccountSessionRegistryHolder.required();
        boolean memoryValid = activeToken != null && token.equals(activeToken.getToken());
        boolean distributedValid = sessions.matches(parsed.getAccountID(), deviceId, token);
        if (!memoryValid && !distributedValid) {
            throw BizException.Of(CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultCode(), CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultMsg());
        }
        if (memoryValid) sessions.save(parsed.getAccountID(), deviceId, token, Duration.ofDays(7));
        HttpProtocolEnvelope response = new HttpProtocolEnvelope();
        response.protocolVersion = "2.0";
        response.msgId = envelope.msgId;
        response.kind = "resp";
        response.requestId = envelope.requestId;
        response.seq = envelope.seq;
        response.traceId = envelope.traceId;
        response.timestamp = System.currentTimeMillis();
        response.code = 0;
        response.message = "success";
        JsonObject result = new JsonObject();
        result.addProperty("wsTicket", WsTicketSigner.issue(parsed.getAccountID()));
        result.addProperty("expiresInSeconds", 30);
        response.body = result;
        return response;
    }

    private HttpProtocolEnvelope refreshToken(HttpProtocolEnvelope envelope) {
        JsonObject data = envelope.body.getAsJsonObject();
        AccountSessionRegistry.SessionTokens tokens = AccountSessionRegistryHolder.required().rotate(
                data.get("sessionId").getAsString(), data.get("deviceId").getAsString(),
                data.get("refreshToken").getAsString(), Duration.ofDays(30));
        CreateAccountTokenInfo oldToken = accountManager.parseAccountToken(tokens.accessToken());
        String nextAccessToken = accountManager.createAccountToken(oldToken.getAccountID(), oldToken.getAccountType(),
                oldToken.getCharAccount(), oldToken.getPsw());
        AccountSessionRegistryHolder.required().replaceAccessToken(tokens.sessionId(), nextAccessToken, Duration.ofDays(30));
        HttpProtocolEnvelope response = new HttpProtocolEnvelope();
        response.protocolVersion = "2.0"; response.msgId = envelope.msgId; response.kind = "resp";
        response.requestId = envelope.requestId; response.seq = envelope.seq; response.traceId = envelope.traceId;
        response.timestamp = System.currentTimeMillis(); response.code = 0; response.message = "success";
        JsonObject body = new JsonObject();
        body.addProperty("accessToken", nextAccessToken); body.addProperty("refreshToken", tokens.refreshToken());
        body.addProperty("sessionId", tokens.sessionId()); body.addProperty("expiresInSeconds", 2592000);
        response.body = body;
        return response;
    }

    private String extractAccessToken(JsonObject responseBody) {
        for (String name : new String[] {"accessToken", "token", "accountToken"}) {
            if (responseBody.has(name) && responseBody.get(name).isJsonPrimitive()) return responseBody.get(name).getAsString();
        }
        if (responseBody.has("payload") && responseBody.get("payload").isJsonPrimitive()) {
            try {
                JsonObject payload = com.google.gson.JsonParser.parseString(responseBody.get("payload").getAsString()).getAsJsonObject();
                for (String name : new String[] {"accessToken", "token", "accountToken"})
                    if (payload.has(name)) return payload.get(name).getAsString();
            } catch (RuntimeException ignored) { }
        }
        return null;
    }

}
