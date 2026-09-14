package com.aoo.bcg.gateway;
import java.time.Duration;
public interface WsTicketService {
    String issue(long userId, String deviceFingerprint, String allowedOrigin, Duration ttl);
    /** Compatibility-only entry point; production handshakes must supply the device fingerprint. */
    @Deprecated ConnectionIdentity consumeOnce(String ticket, String origin);
    ConnectionIdentity consumeOnce(String ticket, String origin, String deviceFingerprint);
}
