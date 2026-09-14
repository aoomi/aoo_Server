package jsproto.c2s.cclass.friendsredpack;

/**
 * 好友帮拆红包主界面信息
 *
 * @author Administrator
 */
public class FriendsHelpUnfoldRedPackInterfaceInfo {
    // 已拆得红包
    private int dismantleValue;
    // 开始时间
    private int startTime;
    // 结束时间
    private int endTime;
    // 提现计数
    private int count;
    // 是否新人
    private boolean isNewPeople;

    public FriendsHelpUnfoldRedPackInterfaceInfo(int dismantleValue, int startTime, int endTime, int count, boolean isNewPeople) {
        super();
        this.dismantleValue = dismantleValue;
        this.startTime = startTime;
        this.endTime = endTime;
        this.count = count;
        this.isNewPeople = isNewPeople;
    }


    public int getDismantleValue() {
        return dismantleValue;
    }

    public void setDismantleValue(int dismantleValue) {
        this.dismantleValue = dismantleValue;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


    public boolean isNewPeople() {
        return isNewPeople;
    }


    public void setNewPeople(boolean isNewPeople) {
        this.isNewPeople = isNewPeople;
    }


}
