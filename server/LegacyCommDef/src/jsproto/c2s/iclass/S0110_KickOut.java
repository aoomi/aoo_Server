package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class S0110_KickOut extends BaseSendMsg {

    public int type;


    public static S0110_KickOut make(int type) {
        S0110_KickOut ret = new S0110_KickOut();
        ret.type = type;

        return ret;


    }
}