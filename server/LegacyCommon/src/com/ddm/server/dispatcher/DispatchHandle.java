package com.ddm.server.dispatcher;

import com.ddm.server.common.CommLogD;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.ddm.server.websocket.handler.MessageDispatcher;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.server.ServerSession;
import lombok.Data;

@Data
public class DispatchHandle implements BaseExecutor {
    private MessageDispatcher messageDispatcher;
    private ServerSession session;
    private MessageHeader header;
    private String message;

    public DispatchHandle(MessageDispatcher messageDispatcher, ServerSession session, MessageHeader header, String message) {
        this.messageDispatcher = messageDispatcher;
        this.session = session;
        this.header = header;
        this.message = message;
    }

    @Override
    public void invoke() {
        try {
            // 本服直接处理
            messageDispatcher.handle(session, header, message);
            session.onReceived(header, message);
        } catch (Exception e) {
            final String handleName = this.getClass().getSimpleName() + " with [" + header.event + "]";
            CommLogD.error("exception in NetHandlerContainerAdaptor: op:{}, sub:{}, error:", header.event, handleName, e);
        }
    }

    @Override
    public int threadId() {
        return 0;
    }
}
