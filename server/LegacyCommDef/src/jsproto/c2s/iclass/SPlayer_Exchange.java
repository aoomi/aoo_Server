package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SPlayer_Exchange extends BaseSendMsg {
    public long pid = 0;
    public String msg = "SUCCESS";
    public int DiamondNum = 0;

    public static SPlayer_Exchange make(long pid, int DiamondNum) {
        SPlayer_Exchange ret = new SPlayer_Exchange();
        ret.pid = pid;
        ret.DiamondNum = DiamondNum;
        return ret;


    }
}
