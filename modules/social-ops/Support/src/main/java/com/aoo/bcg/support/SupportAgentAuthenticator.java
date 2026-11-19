package com.aoo.bcg.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Authenticates the production support ingress credential and requires an explicit agent identity. */
public final class SupportAgentAuthenticator {
    private final byte[] credential;
    public SupportAgentAuthenticator(String credential) {
        if (credential == null || credential.length() < 32) throw new IllegalArgumentException("support agent credential must contain at least 32 characters");
        this.credential = credential.getBytes(StandardCharsets.UTF_8);
    }
    public long authenticate(String authorization, String agentHeader) {
        byte[] supplied = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (!MessageDigest.isEqual(credential, supplied)) throw new SecurityException("valid support credential required");
        try { long id = Long.parseLong(agentHeader); if (id <= 0) throw new NumberFormatException(); return id; }
        catch (Exception failure) { throw new SecurityException("valid X-Support-Agent-Id required"); }
    }
}
