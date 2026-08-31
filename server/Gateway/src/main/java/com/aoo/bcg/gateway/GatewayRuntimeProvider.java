package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.time.Clock;
import java.util.Map;

/** Production assembly SPI. A Gateway process must have exactly one real authority runtime provider. */
public interface GatewayRuntimeProvider {
    Runtime create(DataSource source, ObjectMapper json, Clock clock);
    record Runtime(GameWebSocketRouter router, GatewayWebSocketFrameHandler.SessionResolver sessions,
                   GatewayWebSocketFrameHandler.BroadcastSink broadcasts,
                   GatewayWebSocketFrameHandler.NonRoomDispatcher nonRoom, RoomAuthority authority) {
        public Runtime {
            java.util.Objects.requireNonNull(router);java.util.Objects.requireNonNull(sessions);java.util.Objects.requireNonNull(broadcasts);java.util.Objects.requireNonNull(nonRoom);java.util.Objects.requireNonNull(authority);
        }
        GatewayWebSocketFrameHandler.Factory factory(ObjectMapper json,Clock clock) {
            return identity -> new GatewayWebSocketFrameHandler(identity,router,sessions,broadcasts,nonRoom,json,clock);
        }
    }
    interface RoomAuthority extends AutoCloseable {
        Map<String,Object> create(Map<String,Object> command);
        Map<String,Object> recover(Map<String,Object> command);
        Map<String,Object> join(Map<String,Object> command);
        void remove(long roomId,long fencingToken);
        @Override default void close() { }
    }
}
