package com.ddm.server.websocket.handler;

import com.ddm.server.websocket.def.MessageType;

public class MessageHeader {
    public MessageType messageType;
    public byte srcType;
    public long srcId;
    public byte descType;
    public long descId;

    public String event;
    public short sequence;
    public short errcode;// response时才有意义

    public MessageHeader() {
    }

    public MessageHeader genResponseHeader(short errorCode) {
        MessageHeader header = new MessageHeader();
        header.messageType = MessageType.Response;

        header.srcType = this.descType;// 交换目的地与始发地
        header.srcId = this.descId;// 交换目的地与始发地
        header.descType = this.srcType;// 交换目的地与始发地
        header.descId = this.srcId;// 交换目的地与始发地

        header.event = this.event;
        header.sequence = this.sequence;
        header.errcode = errorCode; // response时才有意义
        return header;
    }

    public MessageHeader genResponseHeader() {
        return this.genResponseHeader((short) 0);
    }

    public short getSequence() {
        return sequence;
    }
}