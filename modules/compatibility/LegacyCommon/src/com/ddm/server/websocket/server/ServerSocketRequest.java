package com.ddm.server.websocket.server;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.handler.response.ResponseHandler;
import com.ddm.server.websocket.handler.response.WebSocketResponse;

public class ServerSocketRequest {

    private static long TIME_OUT = 8000;

    private long beginTime = 0;
    private String operation;
    private short sequence = 0;
    private ServerSession session = null;
    private ResponseHandler handler;

    public ServerSocketRequest(ServerSession session, String operation, short sequence, ResponseHandler handler) {
        this.session = session;
        this.operation = operation;
        this.sequence = sequence;
        this.handler = handler;
        this.beginTime = System.currentTimeMillis();
    }

    public short getSequence() {
        return sequence;
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() > beginTime + TIME_OUT;
    }

    public ResponseHandler getResponseHandler() {
        return handler;
    }

    public void expired() {
        if (handler != null) {
            MessageHeader header = new MessageHeader();
            header.event = this.operation;
            header.sequence = this.sequence;

            header.descType = 0;
            header.descId = 0;

            WebSocketResponse response = new WebSocketResponse(session, header);
            handler.handleError(response, ErrorCode.Request_RequestTimeout.value(), "handle timeout");
        }
    }
}
