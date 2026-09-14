package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

public class CBase_Trusteeship extends BaseSendMsg {
    public long roomID;
    public boolean trusteeship;

    public static CBase_Trusteeship make(long roomID, boolean trusteeship) {
        CBase_Trusteeship ret = new CBase_Trusteeship();
        ret.roomID = roomID;
        ret.trusteeship = trusteeship;
        return ret;
    }
}
