package com.aoo.bcg.gateway;

/** Stable machine-readable errors. Clients branch on code, never on message text. */
public enum GatewayErrorCode {
    INVALID_ENVELOPE(1001, "invalid request envelope"),
    UNAUTHORIZED(1002, "authentication required"),
    REQUEST_REPLAYED(1003, "request rejected"),
    RATE_LIMITED(1004, "request rate limited"),
    PAYLOAD_TOO_LARGE(1005, "payload too large"),
    BACKPRESSURE(1006, "connection overloaded"),
    WS_TICKET_EXPIRED(1007, "websocket ticket expired"),
    WS_TICKET_REJECTED(1008, "websocket ticket rejected"),
    IDEMPOTENCY_CONFLICT(1009, "idempotency key already used"),
    API_VERSION_REQUIRED(1010, "X-Aoo-Api-Version: 1 is required"),
    LEGACY_ENTRY_DISABLED(1011, "legacy gateway entry is disabled"),
    RISK_ADMISSION_REJECTED(1012, "request cannot be accepted"),
    ROUTE_NOT_FOUND(3001, "room route not found"),
    ROUTE_MOVED(3002, "room route moved"),
    ROUTE_STALE(3003, "room route is stale"),
    NODE_DRAINING(3004, "room node is draining"),
    VERSION_INCOMPATIBLE(3005, "protocol version incompatible"),
    FENCING_REJECTED(3006, "stale room owner rejected"),
    ROOM_ADMIN_FORBIDDEN(3007, "room administration forbidden"),
    ROOM_STATE_CONFLICT(3008, "room state version conflict"),
    ROOM_CAPABILITY_UNSUPPORTED(3009, "room capability unsupported"),
    ROOM_AUTHORITY_UNAVAILABLE(3010, "room authority unavailable");

    private final int code;
    private final String defaultMessage;

    GatewayErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        ErrorDomain expected = code >= 3000 ? ErrorDomain.ROOM : ErrorDomain.COMMON;
        if (!expected.contains(code)) throw new IllegalArgumentException("error code outside domain");
    }

    public int code() { return code; }
    public String defaultMessage() { return defaultMessage; }
}
