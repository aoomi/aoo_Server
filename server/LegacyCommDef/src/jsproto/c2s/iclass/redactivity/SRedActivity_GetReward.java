package jsproto.c2s.iclass.redactivity;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class SRedActivity_GetReward extends BaseSendMsg {

    public long activityID;//红包活动ID
    public int money;//获取多少红包  以分为单位

    public static SRedActivity_GetReward make(long activityID, int money) {
        SRedActivity_GetReward ret = new SRedActivity_GetReward();
        ret.activityID = activityID;
        ret.money = money;
        return ret;
    }
}