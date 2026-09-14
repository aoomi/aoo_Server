package jsproto.c2s.cclass.arena;

public class CostItem {
    private int prizeType;// 奖励类型
    private int count;// 数量
    private boolean isShare;


    public CostItem(int prizeType, int count, boolean isShare) {
        super();
        this.prizeType = prizeType;
        this.count = count;
        this.isShare = isShare;
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

    public boolean isShare() {
        return isShare;
    }

    public void setShare(boolean isShare) {
        this.isShare = isShare;
    }


}
