package com.aoo.bcg.gifting;

/** Uses the unified account session authority; gifting must not define a second client token format. */
@FunctionalInterface
public interface GiftingRequestAuthenticator {
    long authenticate(String authorization, String deviceId, String channel, String clientVersion, String remoteIp);
}
