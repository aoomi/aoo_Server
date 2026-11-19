package jsproto.c2s.cclass.config;

import java.util.List;

/**
 * 任务项信息
 *
 * @author Administrator
 */
public class TaskItemInfo {
    private String title;// 标题
    private int taskType;// 任务类型
    private List<TaskConfigInfo> config;// 配置 
    private List<ActivityPrizeInfo> prize;// 奖励信息
    private int process;//进度
    private int count;//数量

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }

    public List<TaskConfigInfo> getConfig() {
        return config;
    }

    public void setConfig(List<TaskConfigInfo> config) {
        this.config = config;
    }

    public List<ActivityPrizeInfo> getPrize() {
        return prize;
    }

    public void setPrize(List<ActivityPrizeInfo> prize) {
        this.prize = prize;
    }

    public int getProcess() {
        return process;
    }

    public void setProcess(int process) {
        this.process = process;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


}
