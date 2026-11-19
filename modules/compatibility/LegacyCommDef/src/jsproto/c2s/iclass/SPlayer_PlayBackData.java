package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 分段回放数据包
 *
 * @author Huaxing
 */
public class SPlayer_PlayBackData extends BaseSendMsg {
    public int id = 0;
    public String msg;
    public int playBackNum = 0;

    public static SPlayer_PlayBackData make(int id, String playBack, int playBackNum) {
        SPlayer_PlayBackData ret = new SPlayer_PlayBackData();
        ret.id = id;
        ret.msg = playBack;
        ret.playBackNum = playBackNum;
        return ret;
    }
}
