package com.aoo.bcg.social;

/** Adapts the unified account session authority without defining a second token format. */
@FunctionalInterface
public interface SocialRequestAuthenticator {
    SocialPrincipal authenticate(String authorization, String deviceId, String channel, String clientVersion, String remoteIp);
}
