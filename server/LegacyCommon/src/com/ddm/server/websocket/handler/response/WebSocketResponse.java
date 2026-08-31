package com.ddm.server.websocket.handler.response;

import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.server.ServerSession;

public class WebSocketResponse {

    private ServerSession session;
    private MessageHeader header;

    public WebSocketResponse(ServerSession session, MessageHeader header) {
        this.session = session;
        this.header = header;
    }

    public MessageHeader getHeader() {
        return header;
    }

    public int getRemoteServerID() {
        return session.getRemoteServerID();
    }

    public ServerSession getSession() {
        return session;
    }
}
