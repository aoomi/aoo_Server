package jsproto.c2s.cclass;

/**
 * 奖励信息
 *
 * @author Administrator
 */
public class RewardInfo {
    public int prizeType;// 奖励类型
    public int count;// 数量


    public RewardInfo() {
        super();
    }

    public RewardInfo(int prizeType, int count) {
        super();
        this.prizeType = prizeType;
        this.count = count;
    }

    public int getPrizeType() {
        return prizeType;
    }

    public void setPrizeType(int prizeType) {
        this.prizeType = prizeType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
