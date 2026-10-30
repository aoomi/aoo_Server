package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class CBase_RoomXiPai extends BaseSendMsg {
    public long roomID;
    //public long pid;

    public static CBase_RoomXiPai make(long roomID/*, long pid*/) {
        CBase_RoomXiPai ret = new CBase_RoomXiPai();
        ret.roomID = roomID;
        //ret.pid = pid;
        return ret;
    }
}
