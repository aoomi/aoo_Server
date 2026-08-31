package com.ddm.server.websocket.codecfactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

//编码器
public class WebEncoder implements ProtocolEncoder {

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        byte[] _protocol = null;
        if (message instanceof String) {// 第一个协议
            _protocol = ((String) message).getBytes("UTF-8");
        } else if (message instanceof ByteBuffer) {
            _protocol = WebEncoder.encode(((ByteBuffer) message).array());
        }
        out.write(IoBuffer.wrap(_protocol));
    }

    // 对传入数据进行无掩码转换
    public static byte[] encode(byte[] msgByte) throws UnsupportedEncodingException {
        // 掩码开始位置
        int masking_key_startIndex = 2;

        // 计算掩码开始位置
        if (msgByte.length <= 125) {
            masking_key_startIndex = 2;
        } else if (msgByte.length > 65536) {
            masking_key_startIndex = 10;
        } else if (msgByte.length > 125) {
            masking_key_startIndex = 4;
        }

        // 创建返回数据
        byte[] result = new byte[msgByte.length + masking_key_startIndex];

        // 开始计算ws-frame
        // frame-fin + frame-rsv1 + frame-rsv2 + frame-rsv3 + frame-opcode
        result[0] = (byte) 0x82; // 130

        // frame-masked+frame-payload-length
        // 从第9个字节开始是 1111101=125,掩码是第3-第6个数据
        // 从第9个字节开始是 1111110>=126,掩码是第5-第8个数据
        if (msgByte.length <= 125) {
            result[1] = (byte) (msgByte.length);
        } else if (msgByte.length > 65536) {
            result[1] = 0x7F; // 127
        } else if (msgByte.length > 125) {
            result[1] = 0x7E; // 126
            result[2] = (byte) (msgByte.length >> 8);
            result[3] = (byte) (msgByte.length % 256);
        }

        // 将数据编码放到最后
        for (int i = 0; i < msgByte.length; i++) {
            result[i + masking_key_startIndex] = msgByte[i];
        }

        return result;
    }

    @Override
    public void dispose(IoSession session) throws Exception {
        // do nothing
    }
}