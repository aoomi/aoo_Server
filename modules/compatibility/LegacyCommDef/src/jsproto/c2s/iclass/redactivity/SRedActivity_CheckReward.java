package jsproto.c2s.iclass.redactivity;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class SRedActivity_CheckReward extends BaseSendMsg {

    public long activityID;//红包活动ID

    public boolean isHaveReward;

    public static SRedActivity_CheckReward make(long activityID, boolean isHaveReward) {
        SRedActivity_CheckReward ret = new SRedActivity_CheckReward();
        ret.activityID = activityID;
        ret.isHaveReward = isHaveReward;
        return ret;
    }
}