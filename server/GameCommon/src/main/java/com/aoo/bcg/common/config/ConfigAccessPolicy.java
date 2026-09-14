package com.aoo.bcg.common.config;

import java.util.Set;

/** Least-privilege namespace policy shared by config-center adapters and deployment checks. */
public final class ConfigAccessPolicy {
    public enum Role { SERVICE_READER, RELEASE_PUBLISHER }
    public record Principal(String identity, Role role, Set<String> readablePrefixes, Set<String> writablePrefixes) {
        public Principal {
            if (identity == null || identity.isBlank() || role == null) throw new IllegalArgumentException("invalid config principal");
            readablePrefixes = Set.copyOf(readablePrefixes);
            writablePrefixes = Set.copyOf(writablePrefixes);
            if (role == Role.SERVICE_READER && !writablePrefixes.isEmpty()) throw new IllegalArgumentException("service reader cannot publish");
            if (role == Role.RELEASE_PUBLISHER && readablePrefixes.stream().anyMatch(value -> value.startsWith("/aoo/secrets/")))
                throw new IllegalArgumentException("publisher cannot resolve service secrets");
        }
    }

    private ConfigAccessPolicy() {}
    public static void requireRead(Principal principal, String namespace) {
        if (principal.readablePrefixes().stream().noneMatch(namespace::startsWith))
            throw new SecurityException("configuration namespace read denied");
    }
    public static void requireWrite(Principal principal, String namespace) {
        if (principal.role() != Role.RELEASE_PUBLISHER
                || principal.writablePrefixes().stream().noneMatch(namespace::startsWith))
            throw new SecurityException("configuration namespace publish denied");
    }
}
