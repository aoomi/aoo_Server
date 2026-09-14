package jsproto.c2s.cclass.union;

import cenum.PrizeType;
import lombok.Data;

import java.util.List;

/**
 * 联盟比赛日志项
 */
@Data
public class UnionMatchLogItem {
    /**
     * 俱乐部Id列表
     */
    private List<Long> clubIdList;
    /**
     * 盟主
     */
    private long ownerId;
    /**
     * 奖励类型
     */
    private PrizeType prizeType;
    /**
     * 排名
     */
    private int ranking;
    /**
     * 奖励值
     */
    private int value;

    /**
     * 回合id
     */
    private int roundId;

    /**
     * 联盟Id
     */
    private long unionId;
    /**
     * 俱乐部id
     */
    private long clubId;

    public UnionMatchLogItem(List<Long> clubIdList, long ownerId, PrizeType prizeType, int ranking, int value, int roundId, long unionId, long clubId) {
        this.clubIdList = clubIdList;
        this.ownerId = ownerId;
        this.prizeType = prizeType;
        this.ranking = ranking;
        this.value = value;
        this.roundId = roundId;
        this.unionId = unionId;
        this.clubId = clubId;
    }
}
