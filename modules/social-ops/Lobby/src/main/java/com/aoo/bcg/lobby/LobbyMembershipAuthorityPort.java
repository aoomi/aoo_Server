package com.aoo.bcg.lobby;

import java.util.Map;

/** Internal-only boundary that seats a durable Hall member in the authoritative room runtime. */
public interface LobbyMembershipAuthorityPort {
    Map<String, Object> join(long roomId, long accountId, int seatNo, String playVersion,
                             String requestId, String traceId, Map<String,Object> admission);
    Map<String, Object> leave(long roomId, long accountId, String playVersion,
                              String requestId, String traceId,
                              Map<String, Object> completeIdentity);
}
