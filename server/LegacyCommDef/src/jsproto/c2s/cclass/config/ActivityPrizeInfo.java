package jsproto.c2s.cclass.config;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务活动奖励类型
 *
 * @author Administrator
 */
public class ActivityPrizeInfo {

    private int id = 0;
    List<RewardInfo> rewardInfos = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<RewardInfo> getRewardInfos() {
        return rewardInfos;
    }

    public void setRewardInfos(List<RewardInfo> rewardInfos) {
        this.rewardInfos = rewardInfos;
    }

    @Override
    public String toString() {
        return "ActivityPrizeInfo [id=" + id + ", rewardInfos=" + rewardInfos + "]";
    }


}
