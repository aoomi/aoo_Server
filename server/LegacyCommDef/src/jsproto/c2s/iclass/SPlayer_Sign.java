package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 玩家在线时间
 *
 * @author Huaxing
 */
public class SPlayer_Sign extends BaseSendMsg {
    public String msg;//
    public int signCount = 0;
    private boolean isTodaySign = false;

    public static SPlayer_Sign make(String msg, int signCount, boolean isTodaySign) {
        SPlayer_Sign ret = new SPlayer_Sign();
        ret.msg = msg;
        ret.signCount = signCount;
        ret.isTodaySign = isTodaySign;
        return ret;
    }

}
