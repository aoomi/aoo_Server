package jsproto.c2s.cclass.config;

import java.util.List;

/**
 * 胡类型奖励配置
 *
 * @author Huaxing
 */
public class HuRewardConfig {
    private long id;
    private int gameType;
    private int beginTime;
    private int endTime;
    private List<HuRewardGroupInfo> prize;//胡牌奖励分组信息
    private int createTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(int beginTime) {
        this.beginTime = beginTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public List<HuRewardGroupInfo> getPrize() {
        return prize;
    }

    public void setPrize(List<HuRewardGroupInfo> prize) {
        this.prize = prize;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "HuRewardConfig [id=" + id + ", gameType=" + gameType + ", beginTime=" + beginTime + ", endTime="
                + endTime + ", prize=" + prize + ", createTime=" + createTime + "]";
    }


}
