package com.ddm.server.netty;

import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * webSocket握手协议处理器
 */
public class TWebSocketServerProtocolHandler extends WebSocketServerProtocolHandler {
    public TWebSocketServerProtocolHandler(String websocketPath) {
        super(websocketPath, null, false, 1 << 20);
    }
}
