package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class CPlayer_AppOs extends BaseSendMsg {
    public int type;

    public static CPlayer_AppOs make(int type) {
        CPlayer_AppOs ret = new CPlayer_AppOs();
        ret.type = type;
        return ret;


    }
}