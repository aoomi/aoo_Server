package com.aoo.bcg.hall.room;

import java.util.Map;

/** Internal-only boundary that seats a durable Hall member in the authoritative room runtime. */
public interface RoomMembershipAuthorityPort {
    Map<String, Object> join(long roomId, long accountId, int seatNo, String playVersion,
                             String requestId, String traceId, Map<String,Object> admission);
}
