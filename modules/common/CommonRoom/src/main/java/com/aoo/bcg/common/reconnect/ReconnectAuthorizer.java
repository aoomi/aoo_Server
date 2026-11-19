package com.aoo.bcg.common.reconnect;

public interface ReconnectAuthorizer {
    void requireAccess(long authenticatedPlayerId, long roomId, String reconnectToken);
}
