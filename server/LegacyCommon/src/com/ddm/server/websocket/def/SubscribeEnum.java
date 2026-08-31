package com.ddm.server.websocket.def;

/**
 * 订阅枚举
 */
public enum SubscribeEnum {
    /**
     * 网关服
     */
    GATE,
    /**
     * 大厅服
     */
    HALL,
    /**
     * 游戏服
     */
    GAME,

    /**
     * 注册中心
     */
    REGISTRY,
    ;

    /**
     * 返回可订阅服务名称
     *
     * @param serverName 当前服务名称
     * @param value      筛选值
     *                   <p>
     *                   GATE_HALL
     *                   GATE_GAME
     * @return
     */
    public static String getSubscribe(String serverName, String value) {
        for (SubscribeEnum subscribeEnum : SubscribeEnum.values()) {
            if (value.startsWith(String.format("%s_%s", serverName, subscribeEnum.name()))) {
                return subscribeEnum.name();
            }
        }
        return "";
    }

    public static SubscribeEnum nameOf(String serverName) {
        for (SubscribeEnum subscribeEnum : SubscribeEnum.values()) {
            if (subscribeEnum.name().equals(serverName)) {
                return subscribeEnum;
            }
        }
        return null;
    }
}
