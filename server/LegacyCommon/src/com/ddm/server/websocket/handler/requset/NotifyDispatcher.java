package com.ddm.server.websocket.handler.requset;

import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.def.TerminalType;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.server.ServerSession;

import java.io.IOException;


public abstract class NotifyDispatcher<Session extends ServerSession> extends RequestDispatcher<Session> {

    public NotifyDispatcher(TerminalType serverType, int serverId, RequestDispatcher<Session> requestDispatcher) {
        super(serverType, serverId, requestDispatcher._requestHandlers);
    }

    @Override
    public void handle(Session session, MessageHeader header, String data) {
        try {
            RequestHandler handler = _requestHandlers.get(header.event);
            if (handler == null) {
                session.sendError(header, ErrorCode.Request_NotFoundHandler, "协议[" + header.event + "]未找到处理器");
                return;
            }
            handler.handleMessage(new WebSocketRequest(session, header), data);
        } catch (WSException e) {
            CommLogD.warn("handle [0x{}] notify failed, reason:{}", header.event, e.getMessage());
            e.callback();
        } catch (IOException e) {
            CommLogD.error("handle [0x{}] notify parse message error, detail:{}", header.event, e.getMessage(), e);
        } catch (Throwable e) {
            CommLogD.error("handle [0x{}] notify failed, reason:{}", header.event, e.getMessage(), e);
        }
    }
}
