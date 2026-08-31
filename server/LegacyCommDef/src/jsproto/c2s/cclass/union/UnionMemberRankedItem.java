package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 中至用
 */
@Data
public class UnionMemberRankedItem {

    /**
     * 亲友圈名称
     */
    private String clubName;

    /**
     * 亲友圈账号
     */
    private int clubSign;
    /**
     * 有效耗钻
     */
    private int consumeValue;
    /**
     * 大赢家
     */
    private int bigWinner;

    /**
     * 活跃积分
     */
    private double promotionShareValue;
    /**
     * 成员总积分和(输赢分-房费)
     */
    private double unionAllMemberPointTotal;

    /**
     * 中至最终积分
     */
    private double zhongZhiTotalPoint;


    public UnionMemberRankedItem(String clubName, int clubSign,
                                 double unionAllMemberPointTotal,
                                 int bigWinner, int consumeValue, double promotionShareValue, double zhongZhiTotalPoint) {
        this.clubName = clubName;

        this.clubSign = clubSign;

        this.unionAllMemberPointTotal = unionAllMemberPointTotal;
        this.bigWinner = bigWinner;
        this.consumeValue = consumeValue;
        this.promotionShareValue = promotionShareValue;
        this.zhongZhiTotalPoint = zhongZhiTotalPoint;
    }
}
