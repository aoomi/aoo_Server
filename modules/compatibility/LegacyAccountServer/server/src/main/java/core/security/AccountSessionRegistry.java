package core.security;

import java.time.Duration;

public interface AccountSessionRegistry {
    void save(long accountId, String deviceId, String accessToken, Duration retention);
    boolean matches(long accountId, String deviceId, String accessToken);
    void revoke(long accountId, String reason);
    SessionTokens bootstrap(long accountId, String deviceId, String accessToken, Duration retention);
    SessionTokens rotate(String sessionId, String deviceId, String refreshToken, Duration retention);
    void replaceAccessToken(String sessionId, String accessToken, Duration retention);
    record SessionTokens(long accountId, String sessionId, String accessToken, String refreshToken) {}
}
