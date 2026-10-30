package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SSCJYMJ_SportsPointEnough extends BaseSendMsg {
    //房间id
    public long roomID;
    public String msg;

    public static SSCJYMJ_SportsPointEnough make(long roomID, String msg) {
        SSCJYMJ_SportsPointEnough ret = new SSCJYMJ_SportsPointEnough();
        ret.roomID = roomID;
        ret.msg = msg;
        return ret;
    }
}
