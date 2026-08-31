package com.ddm.server.protocol.v2;

public final class ProtocolVersionLock {
    public final String protocolVersion;
    public final String playVersion;

    public ProtocolVersionLock(String protocolVersion, String playVersion) {
        if (protocolVersion == null || protocolVersion.isBlank()) throw new IllegalArgumentException("protocolVersion");
        if (playVersion == null || playVersion.isBlank()) throw new IllegalArgumentException("playVersion");
        this.protocolVersion = protocolVersion;
        this.playVersion = playVersion;
    }

    public void require(ProtocolEnvelope message) {
        if (!protocolVersion.equals(message.protocolVersion)) throw new IllegalArgumentException("Protocol version mismatch");
        if (!playVersion.equals(message.playVersion)) throw new IllegalArgumentException("Play version mismatch");
    }
}
