package com.aoo.bcg.gateway;
public record ConnectionIdentity(long userId, String deviceFingerprint, String origin, String sessionId, long authGeneration, String pageInstanceId) {
    public ConnectionIdentity(long userId, String deviceFingerprint, String origin, String pageInstanceId) {
        this(userId, deviceFingerprint, origin, deviceFingerprint, 0, pageInstanceId);
    }
    public ConnectionIdentity {
        if (userId <= 0 || deviceFingerprint == null || deviceFingerprint.isBlank()
                || origin == null || origin.isBlank() || sessionId == null || sessionId.isBlank()
                || authGeneration < 0 || pageInstanceId == null || pageInstanceId.isBlank()) throw new IllegalArgumentException("invalid connection identity");
    }
}
