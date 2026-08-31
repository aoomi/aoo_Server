package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 联赛钻石提醒数量修改
 */
@Data
public class CUnion_ChangeRankedInfo {
    /**
     * 亲友圈Id
     */
    public long clubId;

    /**
     * 联盟id
     */
    public long unionId;
    /**
     * 对所有用户开放 0 关闭 1 开启
     */
    private boolean rankedOpenZhongZhi;
    /**
     * 开放入口 0 关闭 1 开启
     */
    private boolean rankedOpenEntryZhongZhi;

    public static CUnion_ChangeRankedInfo make(long clubId, boolean rankedOpenZhongZhi, boolean rankedOpenEntryZhongZhi, long unionId) {
        CUnion_ChangeRankedInfo ret = new CUnion_ChangeRankedInfo();
        ret.clubId = clubId;
        ret.rankedOpenZhongZhi = rankedOpenZhongZhi;
        ret.rankedOpenEntryZhongZhi = rankedOpenEntryZhongZhi;
        ret.unionId = unionId;
        return ret;
    }
}
