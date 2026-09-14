package business.player.feature;

import BaseThread.BaseMutexObject;
import business.global.config.TaskConfigMgr;
import business.player.Player;
import business.player.feature.achievement.AchievementFeature;
import cenum.ItemFlow;
import cenum.PrizeType;
import cenum.RebateEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.db.entity.clarkGame.PlayerTaskInfoBO;
import core.db.entity.clarkGame.RebateBO;
import core.db.entity.clarkGame.TaskConfigBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerTaskInfoBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.RewardInfo;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskClassify;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskStateEnum;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskTargetType;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskTimeEnum;
import jsproto.c2s.cclass.task.TaskItemInfo;
import jsproto.c2s.iclass.task.STaks_TaskList;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * 玩家活动任务
 *
 * @author Administrator
 */
public class PlayerTask extends Feature {
    // 玩家参与的活动任务
    private Hashtable<Long, PlayerTaskInfoBO> taskInfoBOMap = new Hashtable<>();
    private PlayerTaskInfoBOService playerTaskInfoBOService;
    private final BaseMutexObject _lock = new BaseMutexObject();

    @Override
    public void lock() {
        _lock.lock();
    }

    @Override
    public void unlock() {
        _lock.unlock();
    }

    public PlayerTask(Player player) {
        super(player);
        playerTaskInfoBOService = ContainerMgr.get().getComponent(PlayerTaskInfoBOService.class);
    }

    @Override
    public void loadDB() {
        List<PlayerTaskInfoBO> taskInfoList = playerTaskInfoBOService.findAll(Restrictions.eq("pid", this.getPid()), "");
        // 检查是否有任务数据
        if (null == taskInfoList || taskInfoList.size() <= 0) {
            return;
        }
        // 遍历数据库数据
        for (PlayerTaskInfoBO tInfoBO : taskInfoList) {
            this.taskInfoBOMap.put(tInfoBO.getTaskId(), tInfoBO);
        }
    }

    /**
     * 检查前置任务
     *
     * @param taskConfig
     * @return
     */
    private boolean checkPreTask(TaskConfigBO taskConfig) {
        PlayerTaskInfoBO taskInfoBO = null;
        // 检查是否有前置任务
        if (taskConfig.getPreTaskId() > 0) {
            // 获取前置任务信息
            taskInfoBO = this.taskInfoBOMap.get(taskConfig.getPreTaskId());
            // 检查玩家前置任务是否完成
            if (null == taskInfoBO || taskInfoBO.getState() < TaskStateEnum.End.value()) {
                return false;
            }
        }
        return true;
    }


    /**
     * 执行任务
     */
    public boolean exeTask(int targetType, int num) {
        List<Long> taskIDList = TaskConfigMgr.getInstance().getTaskIDList(targetType);
        // 检查指定的任务目标类型是否不相应的任务ID列表
        if (null == taskIDList || taskIDList.size() <= 0) {
            return false;
        }
        boolean isSign = false;
        // 所有的任务ID,并执行
        for (Long id : taskIDList) {
            // 检查任务执行
            isSign = this.exeTaskID(id, num);
            if (isSign) {
                // 执行成功
                return isSign;
            }
        }
        return isSign;
    }

    /**
     * 执行任务
     */
    public boolean exeTask(int targetType) {
        return this.exeTask(targetType, 1);
    }


    /**
     * 执行任务
     *
     * @param num 次数
     */
    public boolean exeTaskID(long id, int num) {
        TaskConfigBO taskConfig = TaskConfigMgr.getInstance().taskConfigMap(id);
        // 检查指定的任务配置是否存在
        if (null == taskConfig) {
            return false;
        }
        // 任务时间区间检查。
        if (!this.checkTimeinterval(taskConfig)) {
            return false;
        }
        // 检查前置任务
        if (!this.checkPreTask(taskConfig)) {
            return false;
        }
        PlayerTaskInfoBO taskInfoBO = null;
        // 获取玩家任务信息
        taskInfoBO = this.taskInfoBOMap.get(taskConfig.getId());
        // 玩家任务进度信息
        if (null == taskInfoBO) {
            taskInfoBO = new PlayerTaskInfoBO();
            taskInfoBO.setPid(this.getPid());
            taskInfoBO.setTaskId(taskConfig.getId());
        }

        // 如果任务类型 == 日常任务
        if (taskConfig.getTaskType() == TaskClassify.Daily.ordinal()) {
            // 每天可重复的任务
            // 检查更新时间是否不是当天，如果不是当天时间，则状态和任务次数重新开始。
            if (!CommTime.isSameDayWithInTimeZone(taskInfoBO.getUpdateTime(), CommTime.nowSecond())) {
                taskInfoBO.setState(TaskStateEnum.None.value());
                taskInfoBO.setCount(0);
            }
        } else if (taskConfig.getTaskType() == TaskClassify.Special.ordinal()) {
            // 任务类型 == 特殊任务
            // 执行特殊任务
            return this.exeSpecialTask(taskConfig, taskInfoBO, num);
        }
        // 如果玩家的任务状态 == 结束 或者 == 可领取,直接退出
        if (taskInfoBO.getState() > TaskStateEnum.None.value()) {
            return false;
        }
        // 1-邀请新用户
        if (taskConfig.getTargetType() == TaskTargetType.Invite.ordinal()) {
            return false;
        }

        // 10-成功邀请新用户统计
        if (taskConfig.getTargetType() == TaskTargetType.InviteCount.ordinal()) {
            int count = this.player.getFeature(AchievementFeature.class).refererCount();
            if (count <= 0) {
                return false;
            }
            // 检查计数
            if (!taskInfoBO.setCount(count)) {
                return false;
            }
        } else {
            taskInfoBO.setCount(taskInfoBO.getCount() + num);
        }
        // 任务次数
        if (taskInfoBO.getCount() >= taskConfig.getTargetValue()) {
            taskInfoBO.setState(TaskStateEnum.Receive.value());
        }
        taskInfoBO.setUpdateTime(CommTime.nowSecond());
        taskInfoBO.getBaseService().saveOrUpDate(taskInfoBO, new AsyncInfo(taskInfoBO.getId()));
        this.taskInfoBOMap.put(taskConfig.getId(), taskInfoBO);
        return true;
    }

    /**
     * 检查任务时间区间
     * 开始、结束时间 <=0,不限制活动时间
     *
     * @return T:在活动时间内,F:不在活动时间内
     */
    private boolean checkTimeinterval(TaskConfigBO taskConfig) {
        TaskTimeEnum timeEnum = TaskTimeEnum.valueOf(taskConfig.getTimeType());
        switch (timeEnum) {
            case Not:
                // 无
                return true;
            case Everyday:
                // 每天
                return CommTime.checkEveryDayTimeIntervale(taskConfig.getStartTime(), taskConfig.getEndTime());
            case Weekly:
                // 每周
                return CommTime.checkWeeklyTimeIntervale(taskConfig.getStartTime(), taskConfig.getEndTime());
            case TimeTnterval:
                // 时间区间
                return CommTime.checkTimeIntervale(taskConfig.getStartTime(), taskConfig.getEndTime());
            default:
                break;
        }
        return true;
    }

    /**
     * 执行特殊任务
     *
     * @return
     */
    private boolean exeSpecialTask(TaskConfigBO taskConfig, PlayerTaskInfoBO taskInfoBO, int num) {

        // 检查更新时间是否不是当天，如果不是当天时间，则状态和任务次数重新开始。
        if (!CommTime.isSameDayWithInTimeZone(taskInfoBO.getUpdateTime(), CommTime.nowSecond())) {
            // 检查特殊任务的指定目标类型
            if (!this.checkSpecialTargetType(taskConfig.getTargetType())) {
                return false;
            }
            taskInfoBO.setCount(0);
        } else {
            // 如果玩家的任务状态 == 结束 或者 == 可领取,直接退出
            if (taskInfoBO.getState() > TaskStateEnum.None.value()) {
                return false;
            }
            // 任务次数
            if (taskInfoBO.getCount() >= taskConfig.getTargetValue()) {
                return false;
            }
            // 检查特殊任务的指定目标类型
            if (!this.checkSpecialTargetType(taskConfig.getTargetType())) {
                return false;
            }
        }
        taskInfoBO.setState(TaskStateEnum.Receive.value());
        taskInfoBO.setCount(taskInfoBO.getCount() + num);
        taskInfoBO.setUpdateTime(CommTime.nowSecond());
        taskInfoBO.getBaseService().saveOrUpDate(taskInfoBO, new AsyncInfo(taskInfoBO.getId()));
        this.taskInfoBOMap.put(taskConfig.getId(), taskInfoBO);
        return true;
    }


    /**
     * 检查特殊任务的指定目标类型
     *
     * @return
     */
    private boolean checkSpecialTargetType(int targetType) {
        // 邀请新用户
        if (TaskTargetType.Invite.ordinal() == targetType) {
            return this.player.getFeature(AchievementFeature.class).checkReferer();
        }
        return false;
    }


    /**
     * 任务列表
     */
    public void taskList(WebSocketRequest request) {
        // 执行任务
        this.exeTask(TaskTargetType.Invite.ordinal());
        this.exeTask(TaskTargetType.InviteCount.ordinal());
        List<TaskConfigBO> taskList = TaskConfigMgr.getInstance().getTaskList();
        // 玩家任务信息列表
        List<TaskItemInfo> taskItemInfos = new ArrayList<>();
        // 任务项信息
        TaskItemInfo taskItemInfo = null;
        // 玩家任务信息
        PlayerTaskInfoBO taskInfoBO = null;
        // 遍历所有任务
        for (TaskConfigBO taskConfig : taskList) {
            // 检查前置任务
            if (!this.checkPreTask(taskConfig)) {
                continue;
            }
            taskItemInfo = new TaskItemInfo();
            // 检查玩家是否有该任务
            taskInfoBO = this.taskInfoBOMap.get(taskConfig.getId());
            // 检查任务数据是否存在
            if (null != taskInfoBO) {
                if (taskInfoBO.getState() == TaskStateEnum.End.value()) {
                    continue;
                }
                // 如果任务类型 == 日常任务
                if (taskConfig.getTaskType() == TaskClassify.Daily.ordinal()) {
                    // 每天可重复的任务
                    // 检查更新时间是否不是当天，如果不是当天时间，则状态和任务次数重新开始。
                    if (CommTime.isSameDayWithInTimeZone(taskInfoBO.getUpdateTime(), CommTime.nowSecond())) {
                        taskItemInfo.setValue(taskInfoBO.getCount());
                        taskItemInfo.setTaskState(taskInfoBO.getState());
                    }
                } else {
                    taskItemInfo.setValue(taskInfoBO.getCount());
                    taskItemInfo.setTaskState(taskInfoBO.getState());
                }
            }
            taskItemInfo.setId(taskConfig.getId());
            taskItemInfo.setTaskType(taskConfig.getTaskType());
            taskItemInfo.setRewardInfo(taskConfig.getRewardInfo());
            taskItemInfo.setContent(taskConfig.getContent());
            taskItemInfo.setTitle(taskConfig.getTitle());
            taskItemInfo.setUrl(taskConfig.getUrl());
            taskItemInfo.setTargetType(taskConfig.getTargetType());
            taskItemInfo.setTargetValue(taskConfig.getTargetValue());
            taskItemInfos.add(taskItemInfo);
        }
        int drawMoney = this.player.getFeature(PlayerRebate.class).drawMoney();
        request.response(STaks_TaskList.make(taskItemInfos, drawMoney));
    }

    /**
     * 领取任务奖励
     *
     * @param request
     */
    public void receiveTaskReward(WebSocketRequest request, long id) {
        TaskConfigBO taskConfig = TaskConfigMgr.getInstance().taskConfigMap(id);
        // 检查指定的任务配置是否存在
        if (null == taskConfig) {
            request.error(ErrorCode.Task_Error, "null == taskConfig");
            return;
        }
        // 获取玩家任务信息
        PlayerTaskInfoBO taskInfoBO = this.taskInfoBOMap.get(taskConfig.getId());
        if (null == taskInfoBO) {
            request.error(ErrorCode.Task_Error, "null == taskInfoBO");
            return;
        }
        TaskStateEnum stateEnum = TaskStateEnum.End;
        if (TaskClassify.Special.ordinal() == taskConfig.getTaskType()) {
            // 领取特殊任务类型
            stateEnum = TaskStateEnum.None;
        }
        // 检查是否可领取奖励
        if (taskInfoBO.getState() == TaskStateEnum.Receive.value()) {
            lock();
            taskInfoBO.saveState(stateEnum.value());
            unlock();
            // 领取奖励
            this.receiveReward(taskConfig.getRewardInfo());
            this.taskInfoBOMap.put(taskConfig.getId(), taskInfoBO);
            request.response(taskConfig.getRewardInfo());
        } else {
            request.error(ErrorCode.Task_Reward, "Task_Reward");
        }
    }


    /**
     * 领取奖励
     *
     * @param reward
     */
    public void receiveReward(List<RewardInfo> reward) {
        PrizeType prizeType = PrizeType.None;
        for (RewardInfo rInfo : reward) {
            // 获取消耗类型
            prizeType = PrizeType.valueOf(rInfo.getPrizeType());
            // 找不到消耗类型
            if (PrizeType.None.equals(prizeType)) {
                continue;
            }
            // 检查红包类型
            if (PrizeType.RedEnvelope.equals(prizeType)) {
                // TODO 2018/8/22 明天写,奖品红包加入到返利中
                // 红包
                RebateBO rebateBO = new RebateBO();
                rebateBO.setAccountID(this.player.getAccountID());
                rebateBO.setSourceOfTime(CommTime.nowSecond() + "");
                rebateBO.setRebateType(RebateEnum.RebateType.REBATETYPE_TASK.value());
                rebateBO.setSourceOfAccount(this.player.getAccountID());
                rebateBO.setApp_price(rInfo.getCount());
                rebateBO.setFamilyID(this.player.getFamiliID());
                rebateBO.setClassificationRegionId(0);
                rebateBO.getBaseService().saveOrUpDate(rebateBO);
            } else {
                this.getPlayer().getFeature(PlayerCurrency.class).gainItemFlow(prizeType, rInfo.getCount(), ItemFlow.TaskActiveGain);
            }
        }
    }

    /**
     * 获取任务奖励
     *
     * @param request
     * @param id
     */
    public void getTaskReward(WebSocketRequest request, long id) {
        TaskConfigBO taskConfig = TaskConfigMgr.getInstance().taskConfigMap(id);
        // 检查指定的任务配置是否存在
        if (null == taskConfig) {
            request.error(ErrorCode.Task_Error, "null == taskConfig");
            return;
        }
        List<RewardInfo> reward = taskConfig.getRewardInfo();
        // 获取奖励数据是否存在
        if (null == reward || reward.size() <= 0) {
            request.error(ErrorCode.Task_Error, "null == reward || reward.size() <= 0");
            return;
        }
        request.response(reward);
    }

}
