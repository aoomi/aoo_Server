package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 联赛钻石提醒数量修改
 */
@Data
public class SUnion_ChangeRankedInfo extends BaseSendMsg {
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
    private int rankedOpenZhongZhi;
    /**
     * 开放入口 0 关闭 1 开启
     */
    private int rankedOpenEntryZhongZhi;

    public static SUnion_ChangeRankedInfo make(long clubId, int rankedOpenZhongZhi, int rankedOpenEntryZhongZhi, long unionId) {
        SUnion_ChangeRankedInfo ret = new SUnion_ChangeRankedInfo();
        ret.clubId = clubId;
        ret.rankedOpenZhongZhi = rankedOpenZhongZhi;
        ret.rankedOpenEntryZhongZhi = rankedOpenEntryZhongZhi;
        ret.unionId = unionId;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
