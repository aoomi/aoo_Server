package com.aoo.bcg.gateway;

import java.util.Objects;
import java.util.Locale;
import java.util.Set;

/** Rejects accidental publication of unversioned or non-TLS HTTP endpoints. */
public final class HttpRoutePolicy {
    public static final String API_PREFIX = "/api/v2";
    public static final String API_VERSION_HEADER = "X-Aoo-Api-Version";
    public static final String API_VERSION = "1";
    private static final Set<String> PRODUCTION_ROUTES = Set.of(
            "/api/v2/gateway/ws_ticket",
            "/api/v2/gateway/ws",
            "/api/v2/version/check",
            "/api/v2/social",
            "/api/v2/notifications",
            "/api/v2/mail",
            "/api/v2/notices",
            "/api/v2/red-dots"
    );

    private HttpRoutePolicy() { }

    public static String requireApiV1(String path) {
        Objects.requireNonNull(path, "path");
        if (!path.equals(API_PREFIX) && !path.startsWith(API_PREFIX + "/")) {
            throw new IllegalArgumentException("Gateway HTTP route must be below " + API_PREFIX);
        }
        if (path.contains("//") || path.contains("..") || path.indexOf('?') >= 0 || path.indexOf('#') >= 0) {
            throw new IllegalArgumentException("HTTP route must be a normalized path");
        }
        return path;
    }

    public static void requireHttps(boolean secure) {
        if (!secure) throw new SecurityException("Gateway API requires HTTPS");
    }

    public static void requireVersion(String value) {
        if (!API_VERSION.equals(value)) throw new UnsupportedApiVersionException();
    }

    public static boolean isRetiredEntry(String path) {
        if (path == null) return true;
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.equals("/v1") || lower.startsWith("/v1/") || lower.equals("/api/v1")
                || lower.startsWith("/api/v1/") || lower.equals("/clientpack") || lower.startsWith("/clientpack/")
                || lower.equals("/gateway/ws") || lower.equals("/gateway/ws_ticket")
                || lower.equals("/ws") || lower.startsWith("/ws/")
                || lower.equals("/websocket") || lower.startsWith("/websocket/")
                || lower.equals("/javaserverpack") || lower.startsWith("/javaserverpack/")
                || lower.equals("/legacy") || lower.startsWith("/legacy/")
                || lower.equals("/api/legacy") || lower.startsWith("/api/legacy/")
                || lower.endsWith(".php") || lower.contains(".php/");
    }

    /** Exact allowlist for the only routes implemented by this production process. */
    public static boolean isProductionRoute(String path) {
        return path != null && (PRODUCTION_ROUTES.contains(path)
                || path.startsWith("/api/v2/account/")
                || path.startsWith("/api/v2/player-profile/")
                || path.startsWith("/api/v2/hall/"));
                
    }

    public static final class UnsupportedApiVersionException extends IllegalArgumentException { }
}
