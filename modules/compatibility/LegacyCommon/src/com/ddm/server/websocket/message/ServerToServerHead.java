package com.ddm.server.websocket.message;

import lombok.Data;

/**
 * 注册中心的其他服务端之前通信协议
 */
@Data
public class ServerToServerHead {

    /**
     * 消息Id
     */
    private short messageId;

    /**
     * 服务端Id
     */
    private short serverId;

    /**
     * 消息头
     */
    private String event;

    /**
     * 消息内容
     */
    private String message;
}
