package jsproto.c2s.cclass.email;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 奖励信息
 *
 * @author Administrator
 */
public class EMail_RewardInfo {
    private int ranking;
    private List<RewardInfo> eMailRewards = new ArrayList<RewardInfo>();

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public List<RewardInfo> geteMailRewards() {
        return eMailRewards;
    }

    public void seteMailRewards(List<RewardInfo> eMailRewards) {
        this.eMailRewards = eMailRewards;
    }

}
