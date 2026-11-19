package com.aoo.bcg.common.perspective;

public record ViewerContext(long roomId, long authenticatedViewerId, ViewerRole role) {
    public ViewerContext {
        if (roomId <= 0 || authenticatedViewerId <= 0 || role == null)
            throw new IllegalArgumentException("invalid viewer context");
    }
    public static ViewerContext player(long roomId, long playerId) {
        return new ViewerContext(roomId, playerId, ViewerRole.PLAYER);
    }
}
