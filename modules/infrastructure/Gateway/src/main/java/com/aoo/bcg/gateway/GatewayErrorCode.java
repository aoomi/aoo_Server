package com.aoo.bcg.gateway;

import com.aoo.bcg.common.error.ErrorCodeCatalog;

/** Stable machine-readable errors. Clients branch on code, never on message text. */
public enum GatewayErrorCode {
    INVALID_ENVELOPE(1001, "invalid request envelope", "INVALID_ENVELOPE"),
    UNAUTHORIZED(1002, "authentication required", "PERMISSION_DENIED"),
    REQUEST_REPLAYED(1003, "request rejected", "REQUEST_REPLAYED"),
    RATE_LIMITED(1004, "request rate limited", "RATE_LIMITED"),
    PAYLOAD_TOO_LARGE(1005, "payload too large", "PAYLOAD_TOO_LARGE"),
    BACKPRESSURE(1006, "connection overloaded", "BACKPRESSURE"),
    WS_TICKET_EXPIRED(1007, "websocket ticket expired", "TICKET_EXPIRED"),
    WS_TICKET_REJECTED(1008, "websocket ticket rejected", "TICKET_REJECTED"),
    IDEMPOTENCY_CONFLICT(1009, "idempotency key already used", "IDEMPOTENCY_CONFLICT"),
    API_VERSION_REQUIRED(1010, "X-Aoo-Api-Version: 1 is required", "API_VERSION_REQUIRED"),
    LEGACY_ENTRY_DISABLED(1011, "legacy gateway entry is disabled", "LEGACY_ENTRY_DISABLED"),
    RISK_ADMISSION_REJECTED(1012, "request cannot be accepted", "RISK_ADMISSION_REJECTED"),
    ROUTE_NOT_FOUND(3001, "room route not found", "ROUTE_NOT_FOUND"),
    ROUTE_MOVED(3002, "room route moved", "ROUTE_MOVED"),
    ROUTE_STALE(3003, "room route is stale", "ROUTE_STALE"),
    NODE_DRAINING(3004, "room node is draining", "NODE_DRAINING"),
    VERSION_INCOMPATIBLE(3005, "protocol version incompatible", "VERSION_UNSUPPORTED"),
    FENCING_REJECTED(3006, "stale room owner rejected", "FENCING_REJECTED"),
    ROOM_ADMIN_FORBIDDEN(3007, "room administration forbidden", "PERMISSION_DENIED"),
    ROOM_STATE_CONFLICT(3008, "room state version conflict", "ROOM_STATE_CONFLICT"),
    ROOM_CAPABILITY_UNSUPPORTED(3009, "room capability unsupported", "ROOM_CAPABILITY_UNSUPPORTED"),
    ROOM_AUTHORITY_UNAVAILABLE(3010, "room authority unavailable", "UPSTREAM_UNAVAILABLE");

    private final int code;
    private final String defaultMessage;
    private final String registryCode;

    GatewayErrorCode(int code, String defaultMessage, String registryCode) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.registryCode = registryCode;
        ErrorDomain expected = code >= 3000 ? ErrorDomain.ROOM : ErrorDomain.COMMON;
        if (!expected.contains(code)) throw new IllegalArgumentException("error code outside domain");
        ErrorCodeCatalog.standard().requireSymbolic(registryCode);
    }

    public int code() { return code; }
    public String defaultMessage() { return defaultMessage; }
    public String registryCode() { return registryCode; }

    static GatewayErrorCode fromFailure(Throwable failure) {
        return fromRegistryCode(ErrorCodeCatalog.classify(failure, "ROOM_STATE_CONFLICT"));
    }

    static GatewayErrorCode fromRegistryCode(String registryCode) {
        return switch (registryCode) {
            case "ROUTE_NOT_FOUND", "IDENTITY_NOT_FOUND", "CONFIG_NOT_FOUND" -> ROUTE_NOT_FOUND;
            case "VERSION_UNSUPPORTED" -> VERSION_INCOMPATIBLE;
            case "UPSTREAM_UNAVAILABLE", "PROVIDER_MISSING" -> ROOM_AUTHORITY_UNAVAILABLE;
            case "INVALID_ENVELOPE" -> INVALID_ENVELOPE;
            case "PERMISSION_DENIED" -> ROOM_ADMIN_FORBIDDEN;
            case "REQUEST_REPLAYED", "IDEMPOTENCY_CONFLICT" -> IDEMPOTENCY_CONFLICT;
            case "BACKPRESSURE" -> BACKPRESSURE;
            case "ROOM_CAPABILITY_UNSUPPORTED" -> ROOM_CAPABILITY_UNSUPPORTED;
            case "ROOM_STATE_CONFLICT", "SNAPSHOT_MISSING", "PRICING_POLICY_MISSING", "IDENTITY_MISMATCH" -> ROOM_STATE_CONFLICT;
            default -> ROOM_STATE_CONFLICT;
        };
    }
}
