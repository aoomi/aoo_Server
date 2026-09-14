package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


/**
 * 玩家签到
 *
 * @author Huaxing
 */
public class CPlayer_Sign extends BaseSendMsg {
    public int type;//1：查询，2：领取奖励

    public static CPlayer_Sign make(int type) {
        CPlayer_Sign ret = new CPlayer_Sign();
        ret.type = type;
        return ret;
    }
}
