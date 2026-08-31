package core.security;

import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
public final class RedisAccountSessionRegistry implements AccountSessionRegistry {
    private static final String PREFIX = "aoo:account:session:";
    private static final String BY_ACCOUNT = "aoo:account:active:";
    private final RedissonClient redis;

    public RedisAccountSessionRegistry(RedissonClient redis) {
        this.redis = redis;
        AccountSessionRegistryHolder.install(this);
    }

    @Override public void save(long accountId, String deviceId, String accessToken, Duration retention) {
        String key = key(accountId);
        RMap<String, String> session = redis.getMap(key);
        session.putAll(Map.of("deviceId", deviceId, "accessTokenHash", hash(accessToken), "revoked", "0"));
        session.expire(retention);
    }

    @Override public boolean matches(long accountId, String deviceId, String accessToken) {
        String activeSession = bucket(BY_ACCOUNT + accountId).get();
        String key = activeSession == null ? key(accountId) : PREFIX + activeSession;
        RMap<String, String> session = redis.getMap(key);
        String storedDevice = session.get("deviceId");
        String storedHash = session.get("accessTokenHash");
        String revoked = session.get("revoked");
        return deviceId.equals(storedDevice) && hash(accessToken).equals(storedHash) && !"1".equals(revoked);
    }

    @Override public void revoke(long accountId, String reason) {
        String sessionId = bucket(BY_ACCOUNT + accountId).get();
        String key = sessionId == null ? key(accountId) : PREFIX + sessionId;
        RMap<String, String> session = redis.getMap(key);
        session.put("revoked", "1");
        session.put("revokeReason", reason == null ? "UNKNOWN" : reason);
    }

    @Override public SessionTokens bootstrap(long accountId, String deviceId, String accessToken, Duration retention) {
        String oldSession = bucket(BY_ACCOUNT + accountId).get();
        if (oldSession != null) redis.<String, String>getMap(PREFIX + oldSession).put("revoked", "1");
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = randomToken();
        String key = PREFIX + sessionId;
        RMap<String, String> session = redis.getMap(key);
        session.putAll(Map.of("accountId", Long.toString(accountId), "deviceId", deviceId,
                "accessToken", accessToken, "accessTokenHash", hash(accessToken),
                "refreshTokenHash", hash(refreshToken), "version", "1", "revoked", "0"));
        session.expire(retention);
        bucket(BY_ACCOUNT + accountId).set(sessionId, retention);
        return new SessionTokens(accountId, sessionId, accessToken, refreshToken);
    }

    @Override public SessionTokens rotate(String sessionId, String deviceId, String refreshToken, Duration retention) {
        boolean firstUse = bucket(PREFIX + "refresh-use:" + hash(refreshToken)).setIfAbsent("1", retention);
        if (!firstUse) throw new SecurityException("refresh token already used");
        String key = PREFIX + sessionId;
        RMap<String, String> session = redis.getMap(key);
        String accountValue = session.get("accountId");
        String storedDevice = session.get("deviceId");
        String storedRefresh = session.get("refreshTokenHash");
        String accessToken = session.get("accessToken");
        String revoked = session.get("revoked");
        if (accountValue == null || accessToken == null || !deviceId.equals(storedDevice)
                || !hash(refreshToken).equals(storedRefresh) || "1".equals(revoked))
            throw new SecurityException("refresh token invalid or already used");
        long accountId = Long.parseLong(accountValue);
        String nextSessionId = UUID.randomUUID().toString();
        String nextRefresh = randomToken();
        String nextKey = PREFIX + nextSessionId;
        RMap<String, String> nextSession = redis.getMap(nextKey);
        nextSession.putAll(Map.of("accountId", Long.toString(accountId), "deviceId", deviceId,
                "accessToken", accessToken, "accessTokenHash", hash(accessToken),
                "refreshTokenHash", hash(nextRefresh), "version", "2", "revoked", "0"));
        nextSession.expire(retention);
        session.put("revoked", "1");
        bucket(BY_ACCOUNT + accountId).set(nextSessionId, retention);
        return new SessionTokens(accountId, nextSessionId, accessToken, nextRefresh);
    }

    @Override public void replaceAccessToken(String sessionId, String accessToken, Duration retention) {
        String key = PREFIX + sessionId;
        RMap<String, String> session = redis.getMap(key);
        if (!session.isExists()) throw new SecurityException("session not found");
        session.put("accessToken", accessToken);
        session.put("accessTokenHash", hash(accessToken));
        session.expire(retention);
    }

    private String key(long accountId) { return PREFIX + "legacy:" + accountId; }
    private RBucket<String> bucket(String key) { return redis.getBucket(key); }
    private String randomToken() { return UUID.randomUUID().toString() + UUID.randomUUID(); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
}
