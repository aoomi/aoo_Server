package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 亲友圈审核通知
 */
@Data
public class SUnion_ZhongZhiShowStatus extends BaseSendMsg {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     *
     */
    private int zhongZhiShowStatus;

    public static SUnion_ZhongZhiShowStatus make(long unionId, int zhongZhiShowStatus) {
        SUnion_ZhongZhiShowStatus ret = new SUnion_ZhongZhiShowStatus();
        ret.setUnionId(unionId);
        ret.setZhongZhiShowStatus(zhongZhiShowStatus);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}
