package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;


public class CBase_CheckSameIpDissolveRoom extends BaseSendMsg {

    public long roomID;

    public static CBase_CheckSameIpDissolveRoom make(long roomID) {
        CBase_CheckSameIpDissolveRoom ret = new CBase_CheckSameIpDissolveRoom();
        ret.roomID = roomID;
        return ret;
    }
}