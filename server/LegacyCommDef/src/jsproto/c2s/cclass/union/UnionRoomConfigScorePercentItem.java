package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionRoomConfigScorePercentItem {
    /**
     * 配置Id
     */
    private long configId;
    /**
     * 配置名称
     */
    private String configName;
    /**
     * 房间人数
     */
    private int size;
    /**
     * 积分比例
     */
    private double scorePercent;
    private boolean changeFlag;

    public UnionRoomConfigScorePercentItem(long configId, String configName, int size, double scorePercent, boolean changeFlag) {
        this.configId = configId;
        this.configName = configName;
        this.size = size;
        this.scorePercent = scorePercent;
        this.changeFlag = changeFlag;
    }
}
