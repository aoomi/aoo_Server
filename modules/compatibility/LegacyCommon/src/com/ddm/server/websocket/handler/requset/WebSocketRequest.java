package com.ddm.server.websocket.handler.requset;

import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.server.ServerSession;


public class WebSocketRequest {

    private ServerSession session;
    private MessageHeader header;
    private ErrorCode errorCode = ErrorCode.Not;

    public WebSocketRequest(ServerSession session, MessageHeader header) {
        this.session = session;
        this.header = header;
    }

    public void response(Object protocol) {
        if (protocol == null) {
            protocol = "{}";
        }
        session.sendResponseObj(header, protocol);
    }

    public void response(Object protocol, String subjectTopic) {
        if (protocol == null) {
            protocol = "{}";
        }
        session.sendResponseObj(header, protocol, subjectTopic);
    }

    // 空包
    public void response() {
        session.sendResponse(header, "{}");
    }

    public void error(short errorcode, String format, Object... params) {
        String message = format;
        try {
            message = String.format(format, params);
        } catch (Exception e) {
            CommLogD.error("[WebSocketRequest]格式化错误字符串:{},时错误", e);
        }
        session.sendError(header, errorcode, message);
    }

    public void error(ErrorCode errorcode, String format, Object... params) {
        String message = format;
        try {
            message = String.format(format, params);
        } catch (Exception e) {
            CommLogD.error("[WebSocketRequest]格式化错误字符串:{},时错误", e);
        }
        this.errorCode = errorcode;
        session.sendError(header, errorcode, message);
    }

    public ServerSession getSession() {
        return session;
    }

    public int getRemoteServerID() {
        return session.getRemoteServerID();
    }

    public MessageHeader getHeader() {
        return this.header;
    }

    public boolean isErrorCode() {
        if (ErrorCode.Not.equals(this.errorCode)) {
            return true;
        }
        return false;
    }
}
