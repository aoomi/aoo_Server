package cenum;

import lombok.Data;

/**
 * 调度程序组件枚举
 */

public enum DispatcherComponentEnum {
    /**
     * 玩家事件管理器
     */
    PLAYER(0, 8192,ServerSceneEnum.ALL),
    ;

    private int value;

    private int bufferSize;

    private ServerSceneEnum sceneEnum;

    DispatcherComponentEnum(int value, int bufferSize,ServerSceneEnum sceneEnum) {
        this.value = value;
        this.bufferSize = bufferSize;
        this.sceneEnum = sceneEnum;
    }


    public int id() {
        return value;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public ServerSceneEnum getSceneEnum() {
        return sceneEnum;
    }
}
