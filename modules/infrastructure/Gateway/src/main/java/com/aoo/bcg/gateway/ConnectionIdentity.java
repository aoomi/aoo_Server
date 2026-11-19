package com.aoo.bcg.gateway;
public record ConnectionIdentity(long userId, String deviceFingerprint, String origin, String sessionId, long authGeneration,
                                 String pageInstanceId, Long roomId, Integer seatId, String playVersion) {
    public ConnectionIdentity(long userId, String deviceFingerprint, String origin, String sessionId, long authGeneration, String pageInstanceId) {
        this(userId,deviceFingerprint,origin,sessionId,authGeneration,pageInstanceId,null,null,null);
    }
    public ConnectionIdentity(long userId, String deviceFingerprint, String origin, String pageInstanceId) {
        this(userId, deviceFingerprint, origin, deviceFingerprint, 0, pageInstanceId,null,null,null);
    }
    public ConnectionIdentity {
        if (userId <= 0 || deviceFingerprint == null || deviceFingerprint.isBlank()
                || origin == null || origin.isBlank() || sessionId == null || sessionId.isBlank()
                || authGeneration < 0 || pageInstanceId == null || pageInstanceId.isBlank()) throw new IllegalArgumentException("invalid connection identity");
        boolean anyRoomScope=roomId!=null||seatId!=null||playVersion!=null;
        if(anyRoomScope&&(roomId==null||roomId<=0||seatId==null||seatId<0||playVersion==null||playVersion.isBlank()))
            throw new IllegalArgumentException("invalid room connection identity");
    }
    public boolean roomScoped(){return roomId!=null;}
}
