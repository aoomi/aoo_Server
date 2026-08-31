package jsproto.c2s.cclass.union;

import cenum.PrizeType;
import lombok.Data;

/**
 * 赛事排行奖励
 */
@Data
public class UnionRankingReward {
    /**
     * 消耗类型
     */
    private PrizeType prizeType;
    /**
     * 排名
     */
    private int ranking;
    /**
     * 数量
     */
    private int value;

    public UnionRankingReward(int prizeType, int ranking, int value) {
        this.prizeType = PrizeType.valueOf(prizeType);
        this.ranking = ranking;
        this.value = value;
    }

    public UnionRankingReward() {
    }
}
