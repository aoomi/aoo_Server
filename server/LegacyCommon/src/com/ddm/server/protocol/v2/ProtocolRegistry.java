package com.ddm.server.protocol.v2;

import com.aoo.bcg.gamespi.protocol.GeneratedProtocolIds;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolRegistry {
    private final Map<String, ProtocolHandler> handlers = new ConcurrentHashMap<>();

    public void register(String msgId, ProtocolHandler handler) {
        if (!GeneratedProtocolIds.DEFINITIONS.containsKey(msgId)) {
            throw new IllegalArgumentException("Message is not declared in the machine protocol source: " + msgId);
        }
        if (handlers.putIfAbsent(msgId, handler) != null) {
            throw new IllegalStateException("Duplicate protocol handler: " + msgId);
        }
    }

    public ProtocolEnvelope dispatch(ProtocolContext context, ProtocolEnvelope request) throws Exception {
        ProtocolValidator.validate(request);
        GeneratedProtocolIds.Definition definition = GeneratedProtocolIds.DEFINITIONS.get(request.msgId);
        if (definition == null) throw new IllegalArgumentException("Unknown msgId: " + request.msgId);
        if (!definition.kind().equals(request.kind)) {
            throw new IllegalArgumentException("Invalid kind for " + request.msgId + ": " + request.kind);
        }
        ProtocolHandler handler = handlers.get(request.msgId);
        if (handler == null) throw new IllegalArgumentException("Unknown msgId: " + request.msgId);
        return handler.handle(context, request);
    }
}
