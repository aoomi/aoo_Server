package jsproto.c2s.iclass.redactivity;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class CRedActivity_GetReward extends BaseSendMsg {

    public long activityID;//红包活动ID
    public boolean getReward;//是否获取奖励

    public static CRedActivity_GetReward make(long activityID, boolean getReward) {
        CRedActivity_GetReward ret = new CRedActivity_GetReward();
        ret.activityID = activityID;
        ret.getReward = getReward;
        return ret;
    }
}