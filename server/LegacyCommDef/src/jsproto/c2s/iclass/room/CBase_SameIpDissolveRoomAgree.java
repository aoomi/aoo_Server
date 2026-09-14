package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;


public class CBase_SameIpDissolveRoomAgree extends BaseSendMsg {

    public int agree;  //// 0未表态 1支持 2拒绝
    public long roomID;

    public static CBase_SameIpDissolveRoomAgree make(long roomID, int agree) {
        CBase_SameIpDissolveRoomAgree ret = new CBase_SameIpDissolveRoomAgree();
        ret.roomID = roomID;
        ret.agree = agree;
        return ret;
    }
}