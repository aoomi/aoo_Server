package jsproto.c2s.cclass.arena;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 获奖信息
 *
 * @author Administrator
 */
public class AwardRecordInfo {
    private String name = "";
    private long pid;// 用户ID
    private int ranking;// 排名
    private int matchPoint;// 比赛分数
    // 奖励
    private List<RewardInfo> eMailRewards = new ArrayList<RewardInfo>();

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public int getMatchPoint() {
        return matchPoint;
    }

    public void setMatchPoint(int matchPoint) {
        this.matchPoint = matchPoint;
    }

    public List<RewardInfo> geteMailRewards() {
        return eMailRewards;
    }

    public void seteMailRewards(List<RewardInfo> eMailRewards) {
        this.eMailRewards = eMailRewards;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
