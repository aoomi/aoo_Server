package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事收益百分比
 */
@Data
public class CUnionScorePercentItem {
    /**
     * 房间配置Id
     */
    private long configId;
    /**
     * 收益百分比
     */
    private double scorePercent;
}
