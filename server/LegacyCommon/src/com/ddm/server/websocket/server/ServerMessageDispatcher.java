/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.websocket.server;

import BaseCommon.CommClass;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.IMessageDispatcher;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.handler.IBaseHandler;
import com.ddm.server.websocket.handler.MessageDispatcher;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.handler.requset.RequestHandler;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ServerMessageDispatcher<Session extends ServerSession> implements IMessageDispatcher<Session> {

    @Override
    public abstract void init();

    protected final Map<MessageType, MessageDispatcher<Session>> _messageDispatcher = new HashMap<>();

    @Override
    public int handleRawMessage(final Session session, ByteBuffer stream) {
        MessageHeader header = new MessageHeader();
        header.messageType = MessageType.values()[stream.get()];
        header.srcType = stream.get();
        header.srcId = stream.getLong();
        header.descType = stream.get();
        header.descId = stream.getLong();
        header.sequence = stream.getShort();

        MessageDispatcher<Session> dispatcher = _messageDispatcher.get(header.messageType);
        if (dispatcher == null) {
            CommLogD.error("[WSBaseSocketListener] 协议类型错误 类型:{},消息頭部:{}", header.messageType, header);
            return -1;
        }
        try {
            StringBuffer msg = new StringBuffer();
            char ch;
            while ('\0' != (ch = stream.getChar())) {
                msg.append(ch);
            }
            return dispatcher.dispatch(session, header, msg.toString());
        } catch (Exception e) {
            CommLogD.error("[WSBaseSocketListener] 协议处理协议信息处理时错误:", e);
        }
        return -2;
    }

    public void putDispatcher(MessageType messageType, MessageDispatcher<Session> dispatcher) {
        _messageDispatcher.put(messageType, dispatcher);
    }

    public void addRequestHandler(RequestHandler handler) {
        _messageDispatcher.get(MessageType.Request).addHandler(handler);
    }

    public IBaseHandler getHandle(String handler) {
        return _messageDispatcher.get(MessageType.Request).getHandler(handler);
    }

    public void registerRequestHandlers(Class<? extends RequestHandler> clazz) {
        List<Class<?>> dealers = CommClass.getAllClassByInterface(clazz, clazz.getPackage().getName());
        int regCnt = 0;
        for (Class<?> cs : dealers) {
            RequestHandler dealer = null;
            try {
                dealer = (RequestHandler) CommClass.forName(cs.getName()).newInstance();
            } catch (Exception e) {
                CommLogD.error("ServerMessageDispatcher register handler occured error:{}", e.getMessage(), e);
            }
            if (null == dealer) {
                continue;
            }

            this.addRequestHandler(dealer);
            regCnt += 1;
        }
        CommLogD.info("ServerMessageDispatcher registerHandler count:{}", regCnt);
    }

    public void registerRequestHandlers(Set<String> dealers) {
        int regCnt = 0;
        for (String cs : dealers) {
            RequestHandler dealer = null;
            if (_messageDispatcher.get(MessageType.Request).getHandler(cs.replaceAll("^.*\\.handler\\.", "").toLowerCase()) == null) {
                try {
                    dealer = (RequestHandler) CommClass.forName(cs).newInstance();
                } catch (Exception e) {
                    CommLogD.error("ServerMessageDispatcher register handler occured error:{}", e.getMessage(), e);
                }
                if (null == dealer) {
                    continue;
                }
                this.addRequestHandler(dealer);
                regCnt += 1;
            }
        }
        CommLogD.info("ServerMessageDispatcher registerHandler count:{}", regCnt);
    }

    public void registerRequestHandlers(String name) {
        int regCnt = 0;
        RequestHandler dealer = null;
        try {
            dealer = (RequestHandler) CommClass.forName(name).newInstance();
        } catch (Exception e) {
            CommLogD.error("ServerMessageDispatcher register handler occured error:{}", e.getMessage(), e);
        }
        if (null == dealer) {
            return;
        }

        this.addRequestHandler(dealer);
        regCnt += 1;
        CommLogD.info("ServerMessageDispatcher registerHandler count:{}", regCnt);
    }
}
