package com.ddm.server.websocket.codecfactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * netty的消息编码器
 */
public class MessageEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object message, ByteBuf byteBuf) throws Exception {
        byte[] _protocol;
        if (message instanceof String) {// 第一个协议
            _protocol = ((String) message).getBytes("UTF-8");
            byteBuf.writeBytes(_protocol);
        } else if (message instanceof ByteBuf) {
            _protocol = WebEncoder.encode(((ByteBuf) message).array());
            byteBuf.writeBytes(_protocol);
        } else if (message instanceof byte[]) {
            _protocol = WebEncoder.encode((byte[]) message);
            byteBuf.writeBytes(_protocol);
        }
    }
}
