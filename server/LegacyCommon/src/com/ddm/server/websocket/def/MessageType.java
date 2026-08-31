package com.ddm.server.websocket.def;

public enum MessageType {
    /**
     * 空白定义
     */
    None(0),
    /**
     * 对request的反馈，无需回包
     */
    Response(1),
    /**
     * 向其他服务器请求，必定回包
     */
    Request(2),
    /**
     * 向其他服务器发送监控信息，无需回包
     */
    Notify(3),

    /**
     * 检查通信
     */
    Ping(4),

    /**
     * 回包并更新发布主题
     */
    ResponseAndUpdatePub(5),
    ;

    /**
     * 值
     */
    short value;

    MessageType(int value) {
        this.value = (short) value;
    }

    public short value() {
        return value;
    }
}
