package com.aoo.bcg.common.reconnect;
public interface ReconnectService<V, E> { ReconnectResult<V, E> reconnect(long authenticatedPlayerId, long roomId, long lastSequence, String reconnectToken); }
