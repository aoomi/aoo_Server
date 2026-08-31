package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事房间配置项
 */
@Data
public class UnionRoomConfigItem {
    /**
     * 配置Id
     */
    private long configId;

    /**
     * 房间key
     */
    private String roomKey;

    /**
     * 配置名
     */
    private String name;

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
     * 游戏id
     */
    private int gameId;

    /**
     * 序列Id
     */
    private int tagId;

    /**
     * 密码
     */
    private String password;

    /**
     * 竞技点倍数
     */
    private Double sportsDouble = 1D;

    public UnionRoomConfigItem(long configId, String roomKey, String name, int size, int setCount, int tab, int gameId, int tagId, String password) {
        this.configId = configId;
        this.roomKey = roomKey;
        this.name = name;
        this.size = size;
        this.setCount = setCount;
        this.tab = tab;
        this.gameId = gameId;
        this.tagId = tagId;
        this.password = password;
    }

    public UnionRoomConfigItem(long configId, String roomKey, String name, int size, int setCount, int tab, int gameId, int tagId, String password, Double sportsDouble) {
        this.configId = configId;
        this.roomKey = roomKey;
        this.name = name;
        this.size = size;
        this.setCount = setCount;
        this.tab = tab;
        this.gameId = gameId;
        this.tagId = tagId;
        this.password = password;
        this.sportsDouble = sportsDouble;
    }
}
