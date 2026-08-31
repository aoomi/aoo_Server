package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SSCJYMJ_SportsPointNotEnough extends BaseSendMsg {
    //房间id
    public long roomID;

    public static SSCJYMJ_SportsPointNotEnough make(long roomID) {
        SSCJYMJ_SportsPointNotEnough ret = new SSCJYMJ_SportsPointNotEnough();
        ret.roomID = roomID;
        return ret;
    }
}
