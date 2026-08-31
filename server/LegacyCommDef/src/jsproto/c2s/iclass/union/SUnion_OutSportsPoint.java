package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 *
 */
@Data
public class SUnion_OutSportsPoint extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long unionId;
    /**
     * 竞技点
     */
    private double outSports;

    public static SUnion_OutSportsPoint make(long unionId, double outSports) {
        SUnion_OutSportsPoint ret = new SUnion_OutSportsPoint();
        ret.setUnionId(unionId);
        ret.setOutSports(outSports);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}
