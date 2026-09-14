package com.ddm.server.websocket.codecfactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteBuffer;

/**
 * netty的消息解码器
 */
public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    public static final byte MASK = 0x1;// 1000 0000
    public static final byte HAS_EXTEND_DATA = 126;
    public static final byte HAS_EXTEND_DATA_CONTINUE = 127;
    public static final byte PAYLOADLEN = 0x7F;// 0111 1111
    public static final byte OPCODE = 0x0F;// 0000 1111

    public MessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        in.markReaderIndex();
        in.readByte();
        byte head2 = in.readByte();
        int length = (byte) (head2 & PAYLOADLEN);
        if (length < HAS_EXTEND_DATA) {
        } else if (length == HAS_EXTEND_DATA) {
            byte[] extended = new byte[2];
            in.readBytes(extended);
            int shift = 0;
            length = 0;
            for (int i = extended.length - 1; i >= 0; i--) {
                length = length + ((extended[i] & 0xFF) << shift);
                shift += 8;
            }
        } else if (length == HAS_EXTEND_DATA_CONTINUE) {
            byte[] extended = new byte[4];
            in.readBytes(extended);
            int shift = 0;
            length = 0;
            for (int i = extended.length - 1; i >= 0; i--) {
                length = length + ((extended[i] & 0xFF) << shift);
                shift += 8;
            }
        }

        int ismask = head2 >> 7 & MASK;
        byte[] data = null;
        if (ismask == 1) {// 有掩码
            // 获取掩码
            byte[] mask = new byte[4];
            in.readBytes(mask);

            data = new byte[Math.min(length, in.readableBytes())];
            in.readBytes(data);
            for (int i = 0; i < data.length; i++) {
                // 数据进行异或运算
                data[i] = (byte) (data[i] ^ mask[i % 4]);
            }
            in.skipBytes(in.readableBytes());
            return ByteBuffer.wrap(data);
        } else {
            // 无掩码，第一个消息- http 消息
            byte[] b = new byte[in.readableBytes()];
            in.readBytes(b);
            in.skipBytes(in.readableBytes());
            return new String(b, "UTF-8");
        }
    }
}
