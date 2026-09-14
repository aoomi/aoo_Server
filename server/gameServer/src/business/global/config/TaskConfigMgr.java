package business.global.config;

import BaseThread.BaseMutexObject;
import com.alibaba.druid.util.StringUtils;
import com.ddm.server.common.CommLogD;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.db.entity.clarkGame.TaskConfigBO;
import core.db.other.AsyncInfo;
import core.db.service.clarkGame.PlayerTaskInfoBOService;
import core.db.service.clarkGame.TaskConfigBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活动任务配置管理
 *
 * @author Administrator
 */
public class TaskConfigMgr {
    private static TaskConfigMgr instance = new TaskConfigMgr();
    private TaskConfigBOService taskConfigBOService;
    private PlayerTaskInfoBOService playerTaskInfoBOService;
    // 任务类型:任务配置
    private Map<Long, TaskConfigBO> taskConfigMap = new ConcurrentHashMap<>();
    private Map<Integer, List<Long>> taskIdListMap = new ConcurrentHashMap<>();

    public static TaskConfigMgr getInstance() {
        return instance;
    }

    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    public TaskConfigMgr() {
        taskConfigBOService = ContainerMgr.get().getComponent(TaskConfigBOService.class);
        playerTaskInfoBOService = ContainerMgr.get().getComponent(PlayerTaskInfoBOService.class);
    }

    /**
     * 初始化
     */
    public void init() {
        try {
            lock();
            // 获取数据的列表
            List<TaskConfigBO> activityTaskConfigBOs = taskConfigBOService.findAll(null, "");
            if (null != activityTaskConfigBOs) {
                // 遍历活动任务配置

                for (TaskConfigBO taskBO : activityTaskConfigBOs) {
                    // 增加任务ID
                    this.addTaskIDList(taskBO);
                    this.taskConfigMap.put(taskBO.getId(), this.newTaskConfig(taskBO));
                }
            }
        } finally {
            unlock();
        }
    }

    /**
     * 增加任务ID
     *
     * @param taskBO
     */
    public void addTaskIDList(TaskConfigBO taskBO) {
        List<Long> taskIDList = this.taskIdListMap.get(taskBO.getTargetType());
        if (null == taskIDList || taskIDList.size() <= 0) {
            taskIDList = new ArrayList<>();
        }
        if (taskIDList.contains(taskBO.getId())) {
            return;
        }
        taskIDList.add(taskBO.getId());
        this.taskIdListMap.put(taskBO.getTargetType(), taskIDList);
    }

    /**
     * 移除任务ID
     *
     * @param taskBO
     */
    public void removeTaskIDList(TaskConfigBO taskBO) {
        List<Long> taskIDList = this.taskIdListMap.get(taskBO.getTargetType());
        if (null == taskIDList || taskIDList.size() <= 0) {
            return;
        }
        if (!taskIDList.contains(taskBO.getId())) {
            return;
        }
        taskIDList.remove(taskBO.getId());
        this.taskIdListMap.put(taskBO.getTargetType(), taskIDList);
    }


    /**
     * 新建任务配置
     *
     * @param taskBO
     * @return
     */
    private TaskConfigBO newTaskConfig(TaskConfigBO taskBO) {
        taskBO.setRewardInfo(this.rewardInfo(taskBO.getReward()));
        return taskBO;
    }


    /**
     * 解析奖励信息
     *
     * @param rewardInfos
     * @return
     */
    private List<RewardInfo> rewardInfo(String rewardInfos) {
        List<RewardInfo> rInfos = new ArrayList<>();
        // 检查字符串是否为空
        if (StringUtils.isEmpty(rewardInfos)) {
            return rInfos;
        }
        try {
            // 解析奖励信息
            rInfos = new Gson().fromJson(rewardInfos, new TypeToken<List<RewardInfo>>() {
            }.getType());
            if (null == rInfos) {
                rInfos = new ArrayList<>();
                return rInfos;
            }
        } catch (Exception e) {
            CommLogD.error("Exception not rewardInfo : {}", rewardInfos);
            return rInfos;
        }
        return rInfos;
    }


    /**
     * 删除任务
     *
     * @return
     */
    public boolean delTask(long id) {
        try {
            lock();
            TaskConfigBO tConfig = this.taskConfigMap.remove(id);
            // 检查移除的数据是否存在
            if (null != tConfig) {
                taskConfigBOService.delete(id);
                playerTaskInfoBOService.delete(tConfig.getId(), "taskId");
                // 移除任务ID
                this.removeTaskIDList(tConfig);
                return true;
            }
        } finally {
            unlock();
        }
        return false;
    }

    /**
     * 设置任务
     * ID不存在新增，否则修改
     *
     * @return
     */
    public boolean setTask(long id, int taskType, String title, String content, String url, int targetValue, int targetType, long preTaskId, String reward, int timeType, int startTime, int endTime) {
        // 解析是否配置奖励
        List<RewardInfo> rewardInfo = this.rewardInfo(reward);
        if (null == rewardInfo || rewardInfo.size() <= 0) {
            return false;
        }
        try {
            lock();
            TaskConfigBO tConfig = this.taskConfigMap.get(id);
            if (null == tConfig) {
                tConfig = new TaskConfigBO();
            }
            tConfig.setTaskType(taskType);
            tConfig.setTitle(title);
            tConfig.setContent(content);
            tConfig.setUrl(url);
            tConfig.setTargetValue(targetValue);
            tConfig.setTargetType(targetType);
            tConfig.setPreTaskId(preTaskId);
            tConfig.setReward(reward);
            tConfig.setRewardInfo(rewardInfo);
            tConfig.setTimeType(timeType);
            tConfig.setStartTime(startTime);
            tConfig.setEndTime(endTime);
            tConfig.getBaseService().saveOrUpDate(tConfig, new AsyncInfo(tConfig.getId()));
            // 增加任务ID
            this.addTaskIDList(tConfig);
            this.taskConfigMap.put(tConfig.getId(), tConfig);
        } finally {
            unlock();
        }
        return true;
    }

    /**
     * 获取任务列表数据
     *
     * @return
     */
    public List<TaskConfigBO> getTaskList() {
        List<TaskConfigBO> taskList = new ArrayList<>();
        taskList.addAll(this.taskConfigMap.values());
        return taskList;
    }

    /**
     * 任务配置
     *
     * @return
     */
    public TaskConfigBO taskConfigMap(long id) {
        // 任务配置
        return this.taskConfigMap.get(id);
    }

    /**
     * 获取同类型的任务ID
     *
     * @param targetType
     * @return
     */
    public List<Long> getTaskIDList(int targetType) {
        return this.taskIdListMap.get(targetType);

    }

}
