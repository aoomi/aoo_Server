package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


/**
 * 玩家签到
 *
 * @author Huaxing
 */
public class SPlayer_Rebate extends BaseSendMsg {
    public float rebatePercentage;//返利比例
    public int totalRebate;//总返利
    public int alreadyReceivedRebate;//已经领取返利
    public int refererCount;//绑定人数

    public static SPlayer_Rebate make(float rebatePercentage, int totalRebate, int alreadyReceivedRebate, int refererCount) {
        SPlayer_Rebate ret = new SPlayer_Rebate();
        ret.rebatePercentage = rebatePercentage;
        ret.totalRebate = totalRebate;
        ret.alreadyReceivedRebate = alreadyReceivedRebate;
        ret.refererCount = refererCount;
        return ret;
    }
}
