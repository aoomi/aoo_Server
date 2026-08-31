package jsproto.c2s.cclass.config;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 胡牌类型奖励信息
 *
 * @author Administrator
 */
public class HuRewardPrizeInfo {
    private int huType = 0;
    List<RewardInfo> rewardInfos = new ArrayList<>();


    public HuRewardPrizeInfo() {
        super();
    }

    public HuRewardPrizeInfo(int huType, List<RewardInfo> rewardInfos) {
        super();
        this.huType = huType;
        this.rewardInfos = rewardInfos;
    }

    public int getHuType() {
        return huType;
    }

    public void setHuType(int huType) {
        this.huType = huType;
    }

    public List<RewardInfo> getRewardInfos() {
        return rewardInfos;
    }

    public void setRewardInfos(List<RewardInfo> rewardInfos) {
        this.rewardInfos = rewardInfos;
    }

    @Override
    public String toString() {
        return "HuRewardPrizeInfo [huType=" + huType + ", rewardInfos=" + rewardInfos + "]";
    }


}
