package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈房间配置项
 */
@Data
public class ClubRoomConfigItem {
    /**
     * 配置Id
     */
    private long configId;

    /**
     * 房间key
     */
    private String roomKey;

    /**
     * 游戏id
     */
    private int gameId;

    /**
     * 人数
     */
    private int size;
    /**
     * 局数
     */
    private int setCount;
    /**
     * 1：标记
     */
    private int tab;

    /**
     * 序列Id
     */
    private int tagId;

    public ClubRoomConfigItem(long configId, String roomKey, int gameId, int size, int setCount, int tab, int tagId) {
        this.configId = configId;
        this.roomKey = roomKey;
        this.gameId = gameId;
        this.size = size;
        this.setCount = setCount;
        this.tab = tab;
        this.tagId = tagId;
    }
}
