package com.aoo.bcg.gateway;
public record ConnectionIdentity(long userId, String deviceFingerprint, String origin) {
    public ConnectionIdentity {
        if (userId <= 0 || deviceFingerprint == null || deviceFingerprint.isBlank()
                || origin == null || origin.isBlank()) throw new IllegalArgumentException("invalid connection identity");
    }
}
