package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SNJPDK_SportsPointNotEnough extends BaseSendMsg {
    //房间id
    public long roomID;

    public static SNJPDK_SportsPointNotEnough make(long roomID) {
        SNJPDK_SportsPointNotEnough ret = new SNJPDK_SportsPointNotEnough();
        ret.roomID = roomID;
        return ret;
    }
}
