package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事收益百分比
 */
@Data
public class UnionScorePercentItem {
    /**
     * ID
     */
    private long id;
    /**
     * 房间配置Id
     */
    private long configId;
    /**
     * 收益百分比
     */
    private int scorePercent;
    /**
     * 分成值
     */
    private double scoreDividedInto;
    /**
     * 类型（0：百分比，1：固定值）
     */
    private int type;

    public static String getItemsName() {
        return "id,configId,scorePercent,scoreDividedInto,type";
    }

}
