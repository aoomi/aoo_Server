package com.ddm.server.websocket.server;

import BaseThread.BaseMutexObject;
import com.ddm.server.websocket.handler.response.ResponseHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSocketRequestMgr {
    private static short MAX_REQUEST_LENGTH = Short.MAX_VALUE / 2;

    private final ServerSession session;
    private String opcode;
    private short sequence = 0;
    private BaseMutexObject seqMutex = new BaseMutexObject();
    private Map<Short, ServerSocketRequest> requests = new HashMap<>();

    public ServerSocketRequestMgr(ServerSession session, String operation) {
        this.session = session;
        this.opcode = operation;
        // 当前锁等级40级
        seqMutex.reduceMutexLevel(10);
    }

    public ServerSocketRequest popRequest(short sequence) {
        ServerSocketRequest request = null;
        try {
            seqMutex.lock();
            request = requests.remove(sequence);
        } finally {
            seqMutex.unlock();
        }
        return request;
    }

    public ServerSocketRequest genRequest(ResponseHandler handler) {
        if (requests.size() >= MAX_REQUEST_LENGTH) {
            return null;
        }
        ServerSocketRequest request = null;
        seqMutex.lock();
        try {
            do {
                sequence++;
                sequence = sequence >= MAX_REQUEST_LENGTH ? 0 : sequence;
            } while (requests.get(sequence) != null);
            request = new ServerSocketRequest(session, opcode, sequence, handler);
            requests.put(sequence, request);
        } finally {
            seqMutex.unlock();
        }
        return request;
    }

    public void checkTimeoutRequest() {
        List<Short> timeoutList = new ArrayList<>();
        List<ServerSocketRequest> requestLists = null;
        try {
            seqMutex.lock();
            requestLists = new ArrayList<>(requests.values());
        } finally {
            seqMutex.unlock();
        }
        for (ServerSocketRequest request : requestLists) {
            if (request.isTimeout()) {
                timeoutList.add(request.getSequence());
            }
        }
        for (Short req : timeoutList) {
            ServerSocketRequest request = popRequest(req);
            // 因为处理过程中也有可能有请求会被处理掉
            if (request != null) {
                request.expired();
            }
        }
    }
}
