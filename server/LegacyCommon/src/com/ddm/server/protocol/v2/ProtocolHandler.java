package com.ddm.server.protocol.v2;

@FunctionalInterface
public interface ProtocolHandler {
    ProtocolEnvelope handle(ProtocolContext context, ProtocolEnvelope request) throws Exception;
}
