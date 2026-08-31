package jsproto.c2s.cclass.friendsredpack;

/**
 * 帮拆红包信息
 *
 * @author Administrator
 */
public class HelpUnpackRedPackInfo {
    // 已拆得红包
    private int dismantleValue;
    // 剩余红包值
    private int surplusValue;
    // 开始时间
    private int startTime;
    // 结束时间
    private int endTime;


    public HelpUnpackRedPackInfo(int dismantleValue, int surplusValue, int startTime, int endTime) {
        super();
        this.dismantleValue = dismantleValue;
        this.surplusValue = surplusValue;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getDismantleValue() {
        return dismantleValue;
    }

    public void setDismantleValue(int dismantleValue) {
        this.dismantleValue = dismantleValue;
    }

    public int getSurplusValue() {
        return surplusValue;
    }

    public void setSurplusValue(int surplusValue) {
        this.surplusValue = surplusValue;
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


}
