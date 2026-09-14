package com.ddm.server.protocol.v2;

public final class ProtocolContext {
    public final long userId;
    public final String connectionId;
    public final String deviceId;

    public ProtocolContext(long userId, String connectionId, String deviceId) {
        this.userId = userId;
        this.connectionId = connectionId;
        this.deviceId = deviceId;
    }
}
