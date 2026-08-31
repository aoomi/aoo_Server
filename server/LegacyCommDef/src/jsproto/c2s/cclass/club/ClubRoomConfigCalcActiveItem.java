package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubRoomConfigCalcActiveItem {
    /**
     * 配置Id
     */
    private long configId;
    /**
     * 配置名称
     */
    private String configName;
    /**
     * 游戏名称
     */
    private int gameId;
    /**
     * 房间人数
     */
    private int size;
    /**
     * 积分比例
     */
    private double value;
    /**
     * 可分配值
     */
    private double allowValue;
    /**
     * 类型 0 百分比 1 固定值
     */
    private int type;

    private boolean changeFlag;

    public ClubRoomConfigCalcActiveItem() {
    }

    public ClubRoomConfigCalcActiveItem(long configId, String configName, int size) {
        this.configId = configId;
        this.configName = configName;
        this.size = size;
    }

    public ClubRoomConfigCalcActiveItem(long configId, int gameId, int size) {
        this.configId = configId;
        this.gameId = gameId;
        this.size = size;
    }


}
