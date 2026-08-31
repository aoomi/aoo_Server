package com.ddm.server.websocket.def;

public enum TerminalType {
    None(0), // 空白定义 - 0
    Client(1), // 客户端 - 1
    GameServer(2), // 游戏服务器 - 2
    ZoneServer(3), // 小跨服服务器 - 3
    WorldServer(4),// 全跨服服务器 - 4
    ;

    private byte value;

    private TerminalType(int value) {
        this.value = (byte) value;
    }

    public byte value() {
        return value;
    }
}
