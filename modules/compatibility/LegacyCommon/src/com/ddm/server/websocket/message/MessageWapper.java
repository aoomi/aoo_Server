package com.ddm.server.websocket.message;

import com.ddm.server.websocket.handler.MessageHeader;
import lombok.Data;

/**
 * 消息结果
 */
@Data
public class MessageWapper {
    /**
     * 消息头
     */
    private MessageHeader header;
    /**
     * 消息内容
     */
    private String msg;

    public MessageWapper() {

    }

    public MessageWapper(MessageHeader header, String msg) {
        this.header = header;
        this.msg = msg;
    }

    public static MessageWapper make(MessageHeader header, String msg) {
        return new MessageWapper(header, msg);

    }
}
