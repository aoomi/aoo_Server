package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionRankingItem {
    /**
     * 排名
     * 0：未上榜,
     */
    private int rankingId;

    /**
     * 0:没有奖励,1:金币，2:钻石
     */
    private int prizeType;

    /**
     * 数量
     */
    private int value;


    public UnionRankingItem(int rankingId, int prizeType, int value) {
        this.rankingId = rankingId;
        this.prizeType = prizeType;
        this.value = value;
    }

    public UnionRankingItem(int rankingId) {
        this.rankingId = rankingId;
    }

    public UnionRankingItem() {
    }
}
