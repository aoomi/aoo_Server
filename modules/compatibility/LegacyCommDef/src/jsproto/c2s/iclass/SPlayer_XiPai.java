package jsproto.c2s.iclass;

import cenum.ClassType;
import jsproto.c2s.cclass.BaseSendMsg;


public class SPlayer_XiPai extends BaseSendMsg {
    public long roomID;
    public long pid;
    public ClassType cType;

    public static SPlayer_XiPai make(long roomID, long pid, ClassType cType) {
        SPlayer_XiPai ret = new SPlayer_XiPai();
        ret.roomID = roomID;
        ret.pid = pid;
        ret.cType = cType;
        return ret;


    }
}