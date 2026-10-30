package jsproto.c2s.cclass.activity;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 奖励信息
 *
 * @author Administrator
 */
public class ActivityRewardInfo {
    // 排名
    private int ranking;
    // 内容
    private String content;
    // 奖励
    private List<RewardInfo> eMailRewards = new ArrayList<RewardInfo>();


    public List<RewardInfo> geteMailRewards() {
        return eMailRewards;
    }

    public void seteMailRewards(List<RewardInfo> eMailRewards) {
        this.eMailRewards = eMailRewards;
    }


    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ActivityRewardInfo [ranking=" + ranking + ", content=" + content + ", eMailRewards=" + eMailRewards
                + "]";
    }


}
