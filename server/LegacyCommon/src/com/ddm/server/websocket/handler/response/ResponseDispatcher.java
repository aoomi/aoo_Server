package com.ddm.server.websocket.handler.response;

import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.def.TerminalType;
import com.ddm.server.websocket.handler.IBaseHandler;
import com.ddm.server.websocket.handler.MessageDispatcher;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.server.ServerSession;
import com.ddm.server.websocket.server.ServerSocketRequest;

import java.util.HashMap;
import java.util.Map;


public abstract class ResponseDispatcher<Session extends ServerSession> extends MessageDispatcher<Session> {

    public ResponseDispatcher(TerminalType thisServerType, int thisServerId) {
        super(thisServerType, thisServerId);
        this._responseHandlers = new HashMap<>();
    }

    protected final Map<String, ResponseHandler> _responseHandlers;

    @Override
    public void handle(Session session, MessageHeader header, String body) {
        try {
            WebSocketResponse response = new WebSocketResponse(session, header);
            ResponseHandler handler = null;

            // session内有注册处理回调
            ServerSocketRequest request = session.popRequest(header.event, header.sequence);
            if (request != null) {
                handler = request.getResponseHandler();
            }

            // session内没有的话，查看是否有公共回调
            if (handler == null) {
                handler = _responseHandlers.get(header.event);
            }

            if (handler != null) {
                if (header.errcode == ErrorCode.Success.value()) {
                    handler.handleResponse(response, body);
                } else {
                    handler.handleError(response, header.errcode, body);
                }
            } else {
                // 使用默认回调 - 即不需要回调
                if (header.errcode == ErrorCode.Success.value()) {
                    CommLogD.info("[{}]handle success", header.event);
                } else {
                    CommLogD.info("[{}]handle failed, errorCode:{}, messageBytes:{}", header.event, header.errcode, body == null ? 0 : body.length());
                }
            }
        } catch (Exception e) {
            CommLogD.error("handle [{}] response error, message: {}", header.event, e.getMessage(), e);
        }
    }

    @Override
    public void addHandler(IBaseHandler handler) {
        _responseHandlers.put(handler.getEvent(), (ResponseHandler) handler);
    }

    @Override
    public ResponseHandler getHandler(String event) {
        return _responseHandlers.get(event);
    }
}
