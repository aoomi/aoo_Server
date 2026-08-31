package jsproto.c2s.iclass.club;

import lombok.Data;

@Data
public class CClub_CaseSports {
    /**
     * 亲友圈Id
     */
    public long clubId;

    /**
     * 亲友圈Id
     */
    public long unionId;
    /**
     * 0 增加
     * 1 减少
     */
    private int type = 0;
    /**
     * 操作的分数
     */
    private double value = 0;

    public static CClub_CaseSports make(long clubId, int type) {
        CClub_CaseSports ret = new CClub_CaseSports();
        ret.clubId = clubId;
        ret.type = type;
        return ret;
    }
}
