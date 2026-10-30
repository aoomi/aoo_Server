package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionMemberRankedSumInfo {

    /**
     * 联赛活跃积分总和
     */
    private double scorePointSum;
    /**
     * 房卡消耗
     */
    private double consumeValueSum;
    /**
     * 成员积分总和
     */
    private double unionAllMemberPointTotalSum;
    /**
     * 最终积分总和
     */
    private double zhongZhiTotalPointSum;

    public UnionMemberRankedSumInfo(double scorePointSum, double consumeValueSum, double unionAllMemberPointTotalSum, double zhongZhiTotalPointSum) {

        this.scorePointSum = scorePointSum;
        this.consumeValueSum = consumeValueSum;
        this.unionAllMemberPointTotalSum = unionAllMemberPointTotalSum;
        this.zhongZhiTotalPointSum = zhongZhiTotalPointSum;
    }
}
