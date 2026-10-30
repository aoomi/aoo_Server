package com.aoo.bcg.social;

/** Account authority established identity for social commands. */
public record SocialPrincipal(long accountId, String role) {
    public SocialPrincipal {
        if (accountId <= 0) throw new IllegalArgumentException("accountId must be positive");
        if (!"USER".equals(role) && !"SYSTEM".equals(role)) throw new IllegalArgumentException("invalid social role");
    }

    public boolean system() { return "SYSTEM".equals(role); }
}
