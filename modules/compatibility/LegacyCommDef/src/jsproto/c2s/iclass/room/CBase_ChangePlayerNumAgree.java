package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;


public class CBase_ChangePlayerNumAgree extends BaseSendMsg {

    public int agree;  //// 0未表态 1支持 2拒绝
    public long roomID;

    public static CBase_ChangePlayerNumAgree make(long roomID, int agree) {
        CBase_ChangePlayerNumAgree ret = new CBase_ChangePlayerNumAgree();
        ret.roomID = roomID;
        ret.agree = agree;
        return ret;
    }
}