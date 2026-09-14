package jsproto.c2s.cclass.club;


/**
 * 联赛区间id
 */
public class PromotionShareSectionItem {
    private long id;
    private long unionSectionId;
    private long pid;
    private long clubId;
    private int updateTime;
    private int createTime;
    /**
     * 可分配
     */
    private double allowShareToValue;
    /**
     * 分配给自己的值
     */
    private double shareToSelfValue;
    /**
     * 报名费区间开始值
     */
    private double beginValue;
    /**
     * 报名费区间结束值
     */
    private double endValue;
    /**
     * 是否是最后一个区间
     */
    private int endFlag;
    /**
     * 2人
     */
    private double twoValue;
    /**
     * 3人
     */
    private double threeValue;
    /**
     * 4人
     */
    private double fourValue;
    /**
     * 10人
     */
    private double tenValue;

    public double getAllowShareToValue() {
        return allowShareToValue;
    }

    public void setAllowShareToValue(double allowShareToValue) {
        this.allowShareToValue = allowShareToValue;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUnionSectionId() {
        return unionSectionId;
    }

    public void setUnionSectionId(long unionSectionId) {
        this.unionSectionId = unionSectionId;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public double getShareToSelfValue() {
        return shareToSelfValue;
    }

    public void setShareToSelfValue(double shareToSelfValue) {
        this.shareToSelfValue = shareToSelfValue;
    }

    public double getBeginValue() {
        return beginValue;
    }

    public void setBeginValue(double beginValue) {
        this.beginValue = beginValue;
    }

    public double getEndValue() {
        return endValue;
    }

    public void setEndValue(double endValue) {
        this.endValue = endValue;
    }

    public int getEndFlag() {
        return endFlag;
    }

    public void setEndFlag(int endFlag) {
        this.endFlag = endFlag;
    }

    public double getTwoValue() {
        return twoValue;
    }

    public void setTwoValue(double twoValue) {
        this.twoValue = twoValue;
    }

    public double getThreeValue() {
        return threeValue;
    }

    public void setThreeValue(double threeValue) {
        this.threeValue = threeValue;
    }

    public double getFourValue() {
        return fourValue;
    }

    public void setFourValue(double fourValue) {
        this.fourValue = fourValue;
    }

    public double getTenValue() {
        return tenValue;
    }

    public void setTenValue(double tenValue) {
        this.tenValue = tenValue;
    }

    public static String getItemsName() {
        return "id,unionSectionId,pid,clubId,updateTime,createTime,shareToSelfValue,beginValue,endValue,endFlag,allowShareToValue";
    }


}
