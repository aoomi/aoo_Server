package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 亲友圈审核通知
 */
@Data
public class SUnion_Examine extends BaseSendMsg {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 数量
     */
    private int size;

    public static SUnion_Examine make(long unionId, boolean size) {
        SUnion_Examine ret = new SUnion_Examine();
        ret.setUnionId(unionId);
        ret.setSize(size ? 1 : 0);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}
