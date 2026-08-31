package com.aoo.bcg.location;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** Identity produced by the platform authentication layer; this module never trusts request-supplied identity. */
public record CallerContext(String tenantId, String subject, Set<String> scopes, Instant authenticatedAt) {
    public CallerContext {
        tenantId = required(tenantId, "tenantId");
        subject = required(subject, "subject");
        scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
        Objects.requireNonNull(authenticatedAt, "authenticatedAt");
    }

    void require(String scope) {
        if (!scopes.contains(scope)) throw new LocationServiceException(LocationErrorCode.FORBIDDEN, "missing scope: " + scope);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new LocationServiceException(LocationErrorCode.UNAUTHENTICATED, name + " is required");
        return value;
    }
}
