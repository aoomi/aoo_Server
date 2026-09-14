package jsproto.c2s.cclass.activity;

import java.util.List;

/**
 * 活动信息
 *
 * @author Administrator
 */
public class ActivityInfo {
    // ID
    private long id;
    // 标题
    private String title;
    // 类型
    private int type;
    // 启动时间
    private int beginTime;
    // 结束时间
    private int endTime;
    // 启动区间
    private int beginIntervalTime;
    // 结束区间
    private int endIntervalTime;
    // 奖励
    private List<ActivityRewardInfo> prize;
    // 描述
    private String content;
    // Url图片地址
    private String url;
    // 时间类型
    private int timeType;
    // 创建时间
    private int createTime;
    private int updateTime;
    // 状态
    private int state;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public List<ActivityRewardInfo> getPrize() {
        return prize;
    }

    public void setPrize(List<ActivityRewardInfo> prize) {
        this.prize = prize;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public int getTimeType() {
        return timeType;
    }

    public void setTimeType(int timeType) {
        this.timeType = timeType;
    }


    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }


    public int getBeginIntervalTime() {
        return beginIntervalTime;
    }

    public void setBeginIntervalTime(int beginIntervalTime) {
        this.beginIntervalTime = beginIntervalTime;
    }

    public int getEndIntervalTime() {
        return endIntervalTime;
    }

    public void setEndIntervalTime(int endIntervalTime) {
        this.endIntervalTime = endIntervalTime;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "ActivityInfo [id=" + id + ", title=" + title + ", type=" + type + ", beginTime=" + beginTime
                + ", endTime=" + endTime + ", prize=" + prize + ", content=" + content + ", url=" + url + ", timeType="
                + timeType + ", createTime=" + createTime + ", state=" + state + "]";
    }


}
