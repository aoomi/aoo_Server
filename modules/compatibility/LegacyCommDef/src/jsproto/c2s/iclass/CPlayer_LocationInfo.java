package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class CPlayer_LocationInfo extends BaseSendMsg {
    public long pid;

    public static CPlayer_LocationInfo make(long pid) {
        CPlayer_LocationInfo ret = new CPlayer_LocationInfo();
        ret.pid = pid;
        return ret;


    }
}
