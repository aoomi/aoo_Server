package com.aoo.bcg.common.config;

import java.net.URI;

/** Opaque versioned pointer; secret bytes never belong to ordinary configuration. */
public record SecretReference(String provider, String path, String version) {
    public SecretReference {
        if (provider == null || !provider.matches("[a-z][a-z0-9-]*")) throw new IllegalArgumentException("invalid secret provider");
        if (path == null || !path.matches("[A-Za-z0-9_./-]+") || path.startsWith("/")) throw new IllegalArgumentException("invalid secret path");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("secret version is required");
    }

    public static SecretReference parse(String value) {
        URI uri = URI.create(value);
        if (!"secret".equals(uri.getScheme()) || uri.getHost() == null || uri.getFragment() == null)
            throw new IllegalArgumentException("secret must use secret://provider/path#version");
        String path = uri.getPath();
        return new SecretReference(uri.getHost(), path == null ? "" : path.substring(1), uri.getFragment());
    }
}
