package com.ddm.server.protocol.v2;

/** Mandatory boundary preventing full room state or hidden cards from being broadcast. */
@FunctionalInterface
public interface PlayerViewProjector<S> {
    ProtocolEnvelope project(S authoritativeState, long viewerPlayerId, ProtocolEnvelope event);
}
