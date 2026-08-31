package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SNJPDK_SportsPointEnough extends BaseSendMsg {
    //房间id
    public long roomID;
    public String msg;

    public static SNJPDK_SportsPointEnough make(long roomID, String msg) {
        SNJPDK_SportsPointEnough ret = new SNJPDK_SportsPointEnough();
        ret.roomID = roomID;
        ret.msg = msg;
        return ret;
    }
}
