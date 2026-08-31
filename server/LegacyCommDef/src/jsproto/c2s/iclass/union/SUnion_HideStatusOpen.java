package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SUnion_HideStatusOpen extends BaseSendMsg {

    /**
     * 赛事Id
     */
    private long unionId;


    public static SUnion_HideStatusOpen make(long unionId) {
        SUnion_HideStatusOpen ret = new SUnion_HideStatusOpen();
        ret.setUnionId(unionId);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }


}
