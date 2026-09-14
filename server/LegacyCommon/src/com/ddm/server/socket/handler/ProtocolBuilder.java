/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.socket.handler;

import com.ddm.server.websocket.def.ErrorCode;
import com.google.protobuf.Message;

import java.nio.ByteBuffer;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @author clark
 * @date 2016年1月12日
 */
public abstract class ProtocolBuilder {
    private short sequence;
    private Message data = null;

    public abstract String getName();

    public abstract short getOpcode();

    public short getSequence() {
        return sequence;
    }

    public void setSequence(short sequence) {
        this.sequence = sequence;
    }

    public Message getProtoData() {
        return data;
    }

    public void setProtoData(Message data) {
        this.data = data;
    }

    /**
     * 回包：打包数据前，必须设置sequence
     * 发包：自动设置sequence
     *
     * @return
     */
    public ByteBuffer getContent() {
        ByteBuffer bb = ByteBuffer.allocate(data.getSerializedSize() + 8);
        bb.putShort((short) (data.getSerializedSize() + 8));
        bb.putShort(this.getOpcode());
        bb.putShort(sequence);
        bb.putShort(ErrorCode.Success.value());
        bb.put(data.toByteArray());
        bb.flip();
        return bb;
    }
}
