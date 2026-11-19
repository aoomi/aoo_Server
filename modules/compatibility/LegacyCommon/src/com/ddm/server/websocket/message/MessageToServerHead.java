package com.ddm.server.websocket.message;

import lombok.Data;

/**
 * 消息发送到其他服务端的头部
 */
@Data
public class MessageToServerHead {
    /**
     * 账号Id
     */
    private long accountId;
    /**
     * 玩家pid
     */
    private long pid;
    /**
     * 连接sessionId
     */
    private long sessoinId;
    /**
     * 消息Id
     */
    private short messageId;
    /**
     * 服务Id
     */
    private short serverId;
    /**
     * 服务类型（大厅、游戏、网关）
     */
    private short serverType;

    /**
     * 默认0：没验证，需要客户端请求登录接口
     * 验证成功1：客户端与网关服一直保持连接，无需走登录接口
     */
    private byte valid;

    /**
     * 需要告知指定服务端我订阅了你哪个主题，你可以通过这个主题发给我消息。
     * -- 特殊情况
     * ---- 如果是类似创建房间，网关订阅大厅和游戏后面的尾数标号是配对的 订阅大厅：HALL_GATE_01，订阅游戏：GAME_GATE_01
     * ---- 客户端创建房间时，向大厅发送订阅HALL_GATE_01的数据，大厅向游戏发送创建房间时HALL_GATE_01->GAME_GATE_01，游戏直接回包给客户端是GATE_GAME_01
     * ---- 后面的通知玩家就直接向GATE_GAME_01 发布信息
     */
    private String topic;

    /**
     * 客户端携带ip
     */
    private String ip;


}
