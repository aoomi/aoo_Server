package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class CPlayer_GetPlayerInfo extends BaseSendMsg {
    public long pid;

    public static CPlayer_GetPlayerInfo make(int pid) {
        CPlayer_GetPlayerInfo ret = new CPlayer_GetPlayerInfo();
        ret.pid = pid;
        return ret;
    }
}