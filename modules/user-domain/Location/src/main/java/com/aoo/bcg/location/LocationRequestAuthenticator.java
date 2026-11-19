package com.aoo.bcg.location;

import com.sun.net.httpserver.Headers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;

/** Authenticates either an opaque bearer credential or headers from a mutually trusted reverse proxy. */
public final class LocationRequestAuthenticator {
    public record Identity(String tenantId, String subject, Set<String> scopes) {
        public Identity { scopes = Set.copyOf(scopes); }
    }
    private final Map<String, Identity> bearerIdentities;
    private final byte[] trustedProxySecret;
    private final Clock clock;

    public LocationRequestAuthenticator(Map<String, Identity> bearerIdentities, String trustedProxySecret, Clock clock) {
        this.bearerIdentities = Map.copyOf(bearerIdentities);
        this.trustedProxySecret = trustedProxySecret == null ? new byte[0] : trustedProxySecret.getBytes(StandardCharsets.UTF_8);
        this.clock = Objects.requireNonNull(clock);
    }

    public CallerContext authenticate(Headers headers) {
        String authorization = headers.getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            byte[] supplied = authorization.substring(7).getBytes(StandardCharsets.UTF_8);
            for (var entry : bearerIdentities.entrySet()) if (MessageDigest.isEqual(entry.getKey().getBytes(StandardCharsets.UTF_8), supplied)) return context(entry.getValue());
            throw new LocationServiceException(LocationErrorCode.UNAUTHENTICATED, "invalid bearer credential");
        }
        byte[] supplied = bytes(headers.getFirst("X-Aoo-Proxy-Secret"));
        if (trustedProxySecret.length == 0 || !MessageDigest.isEqual(trustedProxySecret, supplied)) throw new LocationServiceException(LocationErrorCode.UNAUTHENTICATED, "bearer or trusted proxy authentication required");
        String tenant = one(headers, "X-Aoo-Tenant-Id"), subject = one(headers, "X-Aoo-Subject");
        String scopeHeader = one(headers, "X-Aoo-Scopes");
        return new CallerContext(tenant, subject, Set.of(scopeHeader.trim().split("\\s+")), clock.instant());
    }
    private CallerContext context(Identity i) { return new CallerContext(i.tenantId(), i.subject(), i.scopes(), clock.instant()); }
    private static String one(Headers h,String n){List<String> v=h.get(n);if(v==null||v.size()!=1||v.getFirst().isBlank())throw new LocationServiceException(LocationErrorCode.UNAUTHENTICATED,n+" must occur exactly once");return v.getFirst();}
    private static byte[] bytes(String s){return s==null?new byte[0]:s.getBytes(StandardCharsets.UTF_8);}
}
