package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 推广奖励
 *
 * @author Huaxing
 */
public class SPlayer_RefererReward extends BaseSendMsg {
    public long pid;
    public String msg;
    public int roomCard;

    public static SPlayer_RefererReward make(long pid, String msg, int roomCard) {
        SPlayer_RefererReward ret = new SPlayer_RefererReward();
        ret.pid = pid;
        ret.msg = msg;
        ret.roomCard = roomCard;
        return ret;
    }
}
