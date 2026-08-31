package com.ddm.server.protocol.v2;

import com.ddm.server.common.redis.RedisUtil;

public final class ProtocolRoomTracker {
    private static final String COUNTER = "protocol:v1:active-rooms";
    private static final int MARKER_TTL_SECONDS = 90 * 24 * 60 * 60;

    private ProtocolRoomTracker() {}

    public static void roomCreated(long roomId) {
        if (roomId <= 0L || ProtocolRequestScope.current() != ProtocolRequestScope.Version.V1) return;
        RedisUtil.markActiveRoom(marker(roomId), COUNTER, MARKER_TTL_SECONDS);
    }

    public static void roomRemoved(long roomId) {
        if (roomId <= 0L) return;
        try {
            RedisUtil.unmarkActiveRoom(marker(roomId), COUNTER);
        } finally {
            HallRoomLifecycleNotifier.close(roomId);
        }
    }

    private static String marker(long roomId) { return "protocol:v1:room:" + roomId; }
}
