package com.aoo.bcg.gateway;

public final class GatewayTicketException extends SecurityException {
    private final GatewayErrorCode code;
    public GatewayTicketException(GatewayErrorCode code) { super(code.defaultMessage()); this.code = code; }
    public GatewayErrorCode code() { return code; }
}
