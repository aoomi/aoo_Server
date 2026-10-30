/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.websocket;

import com.ddm.server.websocket.message.MessageToServerHead;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public interface IMessageDispatcher<Session extends BaseSession> {

    public abstract void init();

    //mina消息处理
    public abstract int handleRawMessage(final Session session, ByteBuffer message);

    //netty消息处理
    public abstract int handleRawMessage(final Session session, ByteBuf message);

    public abstract int handleRawMessageMQ(MessageToServerHead messageHead, ByteBuf message);

}
