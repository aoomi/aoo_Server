package jsproto.c2s.cclass.union;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class UnionMatchInfo {
    /**
     * 排名列表前10
     */
    private List<UnionMatchItem> unionMatchItemList;
    /**
     * 当前排名
     */
    private int curRankingId = 0;

    public UnionMatchInfo(List<UnionMatchItem> unionMatchItemList, int curRankingId) {
        this.unionMatchItemList = unionMatchItemList;
        this.curRankingId = curRankingId;
    }

    public UnionMatchInfo() {
        unionMatchItemList = Collections.emptyList();
    }
}
