package com.aoo.bcg.gateway;

/** Stable failure returned by the internal room-authority boundary. */
public final class RoomAuthorityBusinessError extends RuntimeException {
    private final int status;
    private final String code;

    public RoomAuthorityBusinessError(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
