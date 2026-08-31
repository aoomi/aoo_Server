package com.aoo.bcg.external;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public record ExternalPlatformConfig(
        URI baseUri,
        Duration connectTimeout,
        Duration requestTimeout,
        int maxAttempts,
        Duration initialBackoff,
        int circuitFailureThreshold,
        Duration circuitOpenDuration) {

    public ExternalPlatformConfig {
        Objects.requireNonNull(baseUri, "baseUri");
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(requestTimeout, "requestTimeout");
        requirePositive(initialBackoff, "initialBackoff");
        requirePositive(circuitOpenDuration, "circuitOpenDuration");
        if (!baseUri.isAbsolute() || baseUri.getHost() == null) throw new IllegalArgumentException("baseUri must be absolute");
        if (!"https".equalsIgnoreCase(baseUri.getScheme()) && !isLoopback(baseUri.getHost())) {
            throw new IllegalArgumentException("external platform requires HTTPS");
        }
        if (maxAttempts < 1 || maxAttempts > 5) throw new IllegalArgumentException("maxAttempts must be 1..5");
        if (circuitFailureThreshold < 1) throw new IllegalArgumentException("circuitFailureThreshold must be positive");
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive");
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host);
    }
}
