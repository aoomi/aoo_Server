package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 玩家在线时间
 *
 * @author Huaxing
 */
public class SPlayer_OnlineTime extends BaseSendMsg {
    public int onlineHour = 0;//现在小时

    public static SPlayer_OnlineTime make(int onlineHour) {
        SPlayer_OnlineTime ret = new SPlayer_OnlineTime();
        ret.onlineHour = onlineHour;
        return ret;
    }

}
