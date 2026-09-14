package jsproto.c2s.cclass.task;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务项信息
 *
 * @author Administrator
 */
public class TaskItemInfo {
    private long id;
    private int taskType;
    private String title;
    private String content;
    private String url;
    private int value;
    private int targetValue;
    private int targetType;
    private List<RewardInfo> rewardInfo = new ArrayList<>();
    private int taskState;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
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

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    public List<RewardInfo> getRewardInfo() {
        return rewardInfo;
    }

    public void setRewardInfo(List<RewardInfo> rewardInfo) {
        this.rewardInfo = rewardInfo;
    }

    public int getTaskState() {
        return taskState;
    }

    public void setTaskState(int taskState) {
        this.taskState = taskState;
    }


}
