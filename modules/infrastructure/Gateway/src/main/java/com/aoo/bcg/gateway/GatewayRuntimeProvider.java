package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.time.Clock;
import java.util.Map;

/** Production assembly SPI. A Gateway process must have exactly one real authority runtime provider. */
public interface GatewayRuntimeProvider {
    Runtime create(DataSource source, ObjectMapper json, Clock clock);
    /**
     * Stable, deliberately non-diagnostic readiness result.  Implementations
     * must fail closed; the public route never exposes provider/database
     * details through this value.
     */
    record Readiness(boolean ready, String code) {
        public Readiness {
            if (code == null || code.isBlank() || !code.matches("[A-Z0-9_]+")) {
                throw new IllegalArgumentException("invalid readiness code");
            }
        }

        public static Readiness readyResult() {
            return new Readiness(true, "READY");
        }

        public static Readiness notReady() {
            return new Readiness(false, "ROOM_AUTHORITY_NOT_READY");
        }
    }

    record Runtime(GameWebSocketRouter router, GatewayWebSocketFrameHandler.SessionResolver sessions,
                   GatewayWebSocketFrameHandler.BroadcastSink broadcasts,
                   GatewayWebSocketFrameHandler.NonRoomDispatcher nonRoom,
                   GatewayWebSocketFrameHandler.HandoffOwnerRegistrar handoffOwnerRegistrar,
                   RoomAuthority authority) {
        public Runtime {
            java.util.Objects.requireNonNull(router);java.util.Objects.requireNonNull(sessions);
            java.util.Objects.requireNonNull(broadcasts);java.util.Objects.requireNonNull(nonRoom);
            java.util.Objects.requireNonNull(handoffOwnerRegistrar);java.util.Objects.requireNonNull(authority);
        }
        public Runtime(GameWebSocketRouter router, GatewayWebSocketFrameHandler.SessionResolver sessions,
                       GatewayWebSocketFrameHandler.BroadcastSink broadcasts,
                       GatewayWebSocketFrameHandler.NonRoomDispatcher nonRoom, RoomAuthority authority) {
            this(router, sessions, broadcasts, nonRoom,
                    GatewayWebSocketFrameHandler.HandoffOwnerRegistrar.none(), authority);
        }
        GatewayWebSocketFrameHandler.Factory factory(ObjectMapper json,Clock clock) {
            return identity -> new GatewayWebSocketFrameHandler(identity,router,sessions,broadcasts,
                    nonRoom,handoffOwnerRegistrar,json,clock);
        }
    }
    interface RoomAuthority extends AutoCloseable {
        Map<String,Object> create(Map<String,Object> command);
        Map<String,Object> recover(Map<String,Object> command);
        /** Typed destroy adapter; the map must carry the complete expected room identity. */
        default Map<String,Object> destroy(Map<String,Object> command) {
            throw new IllegalStateException("ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED");
        }
        Map<String,Object> join(Map<String,Object> command);
        Map<String,Object> leave(Map<String,Object> command);
        void remove(long roomId,long fencingToken);
        /** Read-only bounded health probe. The default is fail-closed. */
        default Readiness readiness() { return Readiness.notReady(); }
        @Override default void close() { }
    }
}
