package business.player.feature;

import BaseThread.BaseMutexObject;
import business.global.config.FriendsHelpUnfoldRedPackMgr;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.FriendsHelpUnfoldRedPackEnum.PondType;
import cenum.FriendsHelpUnfoldRedPackEnum.TargetType;
import cenum.FriendsHelpUnfoldRedPackEnum.TaskClassify;
import cenum.FriendsHelpUnfoldRedPackEnum.TaskStateEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.*;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.*;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.player.FriendsHelpUnfoldRedPackEvent;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.friendsredpack.*;
import jsproto.c2s.iclass.friendsredpack.SFriendsRedPack_Info;
import jsproto.c2s.iclass.friendsredpack.SFriendsRedPack_Notice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 玩家活动任务
 *
 * @author Administrator
 */
public class PlayerFriendsHelpUnfoldRedPack extends Feature {
    // 总金额 :1500分（15元）
    private final static int TOTAL_MONEY = 1500;
    // 玩家参与的活动任务
    private HashMap<Long, PlayerFriendsHelpUnfoldRedPackBO> taskInfoBOMap = new HashMap<>();
    // 玩家红包池列表
    private List<PlayerRedPackPondBO> redPackPondBOs = new ArrayList<>();
    // 玩家红包
    private HashMap<Integer, PlayerFriendsRedPackBO> friendsRedPackMap = new HashMap<>();
    // 玩家红包任务时间
    private PlayerRedPackTaskTimeBO redPackTaskTimeBO;
    // 玩家红包记录
    private List<PlayerRedPackRecordBO> redPackRecordBOs = new ArrayList<>();
    private final BaseMutexObject _lock = new BaseMutexObject();

    @Override
    public void lock() {
        _lock.lock();
    }

    @Override
    public void unlock() {
        _lock.unlock();
    }

    public PlayerFriendsHelpUnfoldRedPack(Player player) {
        super(player);
    }

    @Override
    public void loadDB() {
        if (!Config.DE_DEBUG() && this.getPlayer().isTourist()) {
            return;
        }
        // 玩家红包任务时间
        this.redPackTaskTimeBO = ContainerMgr.get().getComponent(PlayerRedPackTaskTimeBOService.class)
                .findOne(Restrictions.eq("pid", this.getPid()), null);
        // 玩家好友帮拆红包信息
        ContainerMgr.get().getComponent(PlayerFriendsHelpUnfoldRedPackBOService.class)
                .findAll(Restrictions.eq("pid", this.getPid())).stream()
                .forEach(k -> this.taskInfoBOMap.put(k.getTaskId(), k));
        // 玩家红包池
        ContainerMgr.get().getComponent(PlayerRedPackPondBOService.class).findAll(Restrictions.eq("pid", this.getPid()))
                .forEach(k -> this.redPackPondBOs.add(k));
        // 玩家好友帮拆红包
        ContainerMgr.get().getComponent(PlayerFriendsRedPackBOService.class)
                .findAll(Restrictions.eq("pid", this.getPid())).stream()
                .forEach(k -> this.friendsRedPackMap.put(k.getPondType(), k));
        // 玩家红包记录
        ContainerMgr.get().getComponent(PlayerRedPackRecordBOService.class)
                .findAll(Restrictions.eq("pid", this.getPid())).stream().forEach(k -> this.redPackRecordBOs.add(k));
    }

    /**
     * 获取红包
     *
     * @param pondType 红包池类型
     * @return
     */
    private PlayerFriendsRedPackBO getPlayerFriendsRedPackBO(int pondType) {
        PlayerFriendsRedPackBO playerFriendsRedPackBO = this.friendsRedPackMap.get(pondType);
        if (null == playerFriendsRedPackBO) {
            playerFriendsRedPackBO = new PlayerFriendsRedPackBO();
            playerFriendsRedPackBO.setPid(this.getPid());
            playerFriendsRedPackBO.setPondType(pondType);
            if (playerFriendsRedPackBO.getBaseService().save(playerFriendsRedPackBO) > 0L) {
                this.friendsRedPackMap.put(playerFriendsRedPackBO.getPondType(), playerFriendsRedPackBO);
            }
        }
        return playerFriendsRedPackBO;
    }

    /**
     * 检查好友红包值是否满足
     *
     * @param pondType 红包池类型
     * @param value    值
     * @return
     */
    public boolean checkFriendsRedPackValue(int pondType, int value) {
        PlayerFriendsRedPackBO friendsRedPackBO = this.getPlayerFriendsRedPackBO(pondType);
//		不可能为空
//		if (null == friendsRedPackBO) {
//			return false;
//		}
        return friendsRedPackBO.getValue() >= value;
    }

    /**
     * 检查时间区间
     *
     * @return
     */
    private boolean checkTimeIntervale() {
        // 检查玩家的任务时间配置是否存在
        if (null == this.redPackTaskTimeBO) {
            // 新建一个时间配置
            return false;
        }
        // 检查当前时间是否在时间配置的区间内
        if (CommTime.checkTimeIntervale(this.redPackTaskTimeBO.getStartTime(), this.redPackTaskTimeBO.getEndTime())) {
            // 在指定时间区间内
            return true;
        }
        return false;
    }

    /**
     * 玩家红包任务时间
     *
     * @return
     */
    public PlayerRedPackTaskTimeBO getRedPackTaskTimeBO() {
        return redPackTaskTimeBO;
    }

    /**
     * 关闭红包任务
     */
    private void closeRedPackTask() {
        // 检查玩家的任务时间配置是否存在
        if (null == this.redPackTaskTimeBO) {
            return;
        }
        // 删除
        this.redPackTaskTimeBO.getBaseService().delete(this.redPackTaskTimeBO.getId());
        // 清空数据
        this.redPackTaskTimeBO = null;
        // 遍历删除数据
        this.removePlayerFriendsHelpUnfoldRedPackBO();
        // 清除玩家好友帮拆红包
        ContainerMgr.get().getComponent(PlayerFriendsRedPackBOService.class)
                .delete(Restrictions.eq("pid", this.getPid()));
        this.friendsRedPackMap.clear();
        // 指定玩家的红包池里面的数据
        ContainerMgr.get().getComponent(PlayerRedPackPondBOService.class).delete(Restrictions.eq("pid", this.getPid()));
        this.redPackPondBOs.clear();
        // 玩家红包记录
        ContainerMgr.get().getComponent(PlayerRedPackRecordBOService.class)
                .delete(Restrictions.eq("pid", this.getPid()));
        this.redPackRecordBOs.clear();
    }

    private void removePlayerFriendsHelpUnfoldRedPackBO() {
        this.taskInfoBOMap.entrySet().removeIf(entry -> {
            PlayerFriendsHelpUnfoldRedPackBO value = entry.getValue();
            if (value.getTaskType() == TaskClassify.Novice.ordinal()) {
                value.saveState(TaskStateEnum.End.value());
                return false;
            } else {
                value.getBaseService().delete(value.getId());
                return true;
            }
        });
    }

    /**
     * 检查时间区间
     *
     * @return
     */
    private void timeIntervale() {
        // 检查玩家的任务时间配置是否存在
        if (null == this.redPackTaskTimeBO) {
            // 新建一个时间配置
            this.redPackTaskTimeBO = new PlayerRedPackTaskTimeBO();
        }
        // 检查当前时间是否在时间配置的区间内
        if (CommTime.checkTimeIntervale(this.redPackTaskTimeBO.getStartTime(), this.redPackTaskTimeBO.getEndTime())) {
            // 在指定时间区间内
            return;
        }
        // 时间
        int startTime = CommTime.nowSecond();
        // 设置用户PID
        this.redPackTaskTimeBO.setPid(this.getPid());
        // 开始时间
        this.redPackTaskTimeBO.setStartTime(startTime);
        // 结束时间
        this.redPackTaskTimeBO.setEndTime(FriendsHelpUnfoldRedPackMgr.getInstance().getEndLimitTime(startTime));
        // 保存数据
        this.redPackTaskTimeBO.getBaseService().saveOrUpDate(this.redPackTaskTimeBO,
                new AsyncInfo(this.redPackTaskTimeBO.getId()));
        // 遍历删除数据
        this.removePlayerFriendsHelpUnfoldRedPackBO();
        // 清除玩家好友帮拆红包
        ContainerMgr.get().getComponent(PlayerFriendsRedPackBOService.class)
                .delete(Restrictions.eq("pid", this.getPid()));
        this.friendsRedPackMap.clear();
        // 指定玩家的红包池里面的数据
        ContainerMgr.get().getComponent(PlayerRedPackPondBOService.class).delete(Restrictions.eq("pid", this.getPid()));
        this.redPackPondBOs.clear();
        // 玩家红包记录
        ContainerMgr.get().getComponent(PlayerRedPackRecordBOService.class)
                .delete(Restrictions.eq("pid", this.getPid()));
        this.redPackRecordBOs.clear();
    }

    /**
     * 添加玩家红包记录
     *
     * @param toPid   目标pid
     * @param redPack 红包（单位：分）
     * @param isHu    1：胡牌
     */
    public boolean insertPlayerRedPackRecordBO(long toPid, int redPack, int isHu) {
        if (this.redPackRecordBOs.stream()
                .filter(k -> k.getIsHu() == 1 && this.getPid() != toPid && k.getToPid() == toPid).findAny()
                .isPresent()) {
            // 胡牌，并且不是操作玩家本身
            return false;
        }
        PlayerRedPackRecordBO redPackRecordBO = this.redPackRecordBOs.stream().filter(k -> k.getToPid() == toPid)
                .findFirst().orElse(null);
        if (null == redPackRecordBO) {
            // 当前操作玩家和被操作PID不相等 并且 是胡，不记录。
            if (this.getPid() != toPid && isHu == 1) {
                return false;
            }
            redPackRecordBO = new PlayerRedPackRecordBO();
            redPackRecordBO.setPid(this.getPid());
            redPackRecordBO.setToPid(toPid);
            redPackRecordBO.setValue(redPack);
            if (this.getPid() == toPid) {
                redPackRecordBO.setIsHu(1);
            }
            redPackRecordBO.getBaseService().save(redPackRecordBO);
            this.redPackRecordBOs.add(redPackRecordBO);
        } else {
            redPackRecordBO.setValue(redPackRecordBO.getValue() + redPack);
            redPackRecordBO.setIsHu(isHu);
            redPackRecordBO.getBaseService().update(redPackRecordBO);
        }
        return true;

    }

    /**
     * 获取红包记录项
     *
     * @param packRecordBO
     * @return
     */
    private PlayerRedPackRecordItem getPlayerRedPackRecordItem(PlayerRedPackRecordBO packRecordBO) {
        // 获取玩家信息
        Player player = PlayerMgr.getInstance().getPlayer(packRecordBO.getToPid());
        if (null == player) {
            return null;
        }
        // 创建新的红包记录项
        return new PlayerRedPackRecordItem(player.getShortPlayer(), packRecordBO.getIsHu() == 1 ? true : false,
                this.getPid() == packRecordBO.getToPid(), packRecordBO.getValue());
    }

    /**
     * 当前回合（本轮）打开红包记录项
     *
     * @param playerRedPackPondBO
     * @return
     */
    private CurRoundOpenRedPackRecordItem getCurRoundOpenRedPackRecordItem(PlayerRedPackPondBO playerRedPackPondBO) {
        // 获取玩家信息
        Player player = PlayerMgr.getInstance().getPlayer(playerRedPackPondBO.getToPid());
        if (null == player) {
            return null;
        }
        return new CurRoundOpenRedPackRecordItem(player.getShortPlayer(), playerRedPackPondBO.getGetTime(),
                playerRedPackPondBO.getRewards(), playerRedPackPondBO.getValue());
    }

    /**
     * 检查前置任务
     *
     * @param taskConfig
     * @return
     */
    private boolean checkPreTask(FriendsHelpUnfoldRedPackBO taskConfig) {
        PlayerFriendsHelpUnfoldRedPackBO taskInfoBO = null;
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
     *
     * @param targetType 任务类型
     * @param toPid      目标玩家PID
     */
    public boolean exeTask(int targetType, long toPid) {
        // 检查是否在任务时间区间内
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            // 不在任务时间区间内
            return false;
        }
        // 游客用户不能做任务
        if (!Config.DE_DEBUG() && this.getPlayer().isTourist()) {
            return false;
        }

        // 获取同类型的任务ID
        List<Long> taskIDList = FriendsHelpUnfoldRedPackMgr.getInstance().getTaskIDList(targetType);
        // 检查指定的任务目标类型是否不相应的任务ID列表
        if (null == taskIDList || taskIDList.size() <= 0) {
            return false;
        }
        boolean isSign = false;
        // 所有的任务ID,并执行
        for (Long id : taskIDList) {
            // 检查任务执行
            isSign = this.exeTaskID(id, toPid);
            if (isSign) {
                // 执行成功
                return isSign;
            }
        }
        return isSign;
    }

    /**
     * 执行任务
     *
     * @param targetType 任务类型
     */
    public boolean exeTask(int targetType) {
        return this.exeTask(targetType, this.getPid());
    }

    /**
     * 执行任务
     *
     * @param id    任务ID
     * @param toPid 次数
     */
    public boolean exeTaskID(long id, long toPid) {
        FriendsHelpUnfoldRedPackBO taskConfig = FriendsHelpUnfoldRedPackMgr.getInstance().taskConfigMap(id);
        // 检查指定的任务配置是否存在
        if (null == taskConfig) {
            return false;
        }

        // 检查前置任务
        if (!this.checkPreTask(taskConfig)) {
            return false;
        }
        if (taskConfig.getTargetType() == TargetType.OpenRedPack.ordinal()) {
            if (!this.checkTimeIntervale()) {
                // 拆红包
                this.timeIntervale();
                // 检查是否有直接推荐人
                if (this.getPlayer().getPlayerBO().getRealReferer() > 0 && CommTime
                        .hourTimeDifference(this.getPlayer().getPlayerBO().getCreateTime(), CommTime.nowMS()) <= 24) {
                    DispatcherComponent.getInstance().publish(new FriendsHelpUnfoldRedPackEvent(
                            this.getPid(), TargetType.valueOf(taskConfig.getTargetType()),
                            this.getPlayer().getPlayerBO().getRealReferer()));
                }
            }
        } else {
            // 检查时间区间
            if (!this.checkTimeIntervale()) {
                return false;
            }
        }
        // 获取玩家任务信息
        PlayerFriendsHelpUnfoldRedPackBO taskInfoBO = this.taskInfoBOMap.get(taskConfig.getId());
        // 玩家任务进度信息
        if (null == taskInfoBO) {
            taskInfoBO = new PlayerFriendsHelpUnfoldRedPackBO();
            taskInfoBO.setPid(this.getPid());
            taskInfoBO.setTaskId(taskConfig.getId());
            this.taskInfoBOMap.put(taskConfig.getId(), taskInfoBO);
        } else {
            // 检查是否已做过该任务。
            if (taskInfoBO.getState() > TaskStateEnum.None.value()) {
                return false;
            }
        }

        // 红包（单位：分）
        int redPack = taskConfig.getValue();
        if (taskConfig.getTaskType() == TaskClassify.Novice.ordinal()) {
            if (CommTime.hourTimeDifference(this.getPlayer().getPlayerBO().getCreateTime(), CommTime.nowMS()) <= 24) {
                redPack += CommMath.randomInt(0, 9);
                // 1-新手任务
                taskInfoBO.setState(TaskStateEnum.Receive.value());
            } else {
                taskInfoBO.setState(TaskStateEnum.End.value());
            }
        } else if (taskConfig.getTaskType() == TaskClassify.BranchLine.ordinal()) {
            // 第一次帮拆红包
            if (taskConfig.getTargetType() == TargetType.HelpToUnpackRedEnvelopes.ordinal()) {
                if (!this.infinite(taskConfig, toPid, redPack, false)) {
                    return false;
                }
            } else {
                // 添加玩家红包记录
                if (this.insertPlayerRedPackRecordBO(toPid, redPack, 1)) {
                    // 加入红包池
                    this.insertRedPackPondBO(toPid, redPack, taskConfig.getTitle(), taskConfig.getPondType());
                } else {
                    return false;
                }
            }
            // 2-支线任务
            taskInfoBO.setState(TaskStateEnum.End.value());
        } else {
            // 3-无限任务
            if (!this.infinite(taskConfig, toPid, redPack,
                    TargetType.FriendHuPai.ordinal() == taskConfig.getTargetType())) {
                return false;
            }
        }
        taskInfoBO.setValue(redPack);
        taskInfoBO.setTaskType(taskConfig.getTaskType());
        taskInfoBO.setTargetType(taskConfig.getTargetType());
        taskInfoBO.getBaseService().saveOrUpDate(taskInfoBO);
        return true;
    }

    /**
     * 获取新人红包
     */
    public void exeFirstRegisterLoginGame() {
        // 检查是否在任务时间区间内
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            // 不在任务时间区间内
            return;
        }
        if (!this.checkTimeIntervale()) {
            // 重新一轮
            return;
        }

        // // 如果目标任务 == 首次注册登陆游戏 && 任务状态 == 完成，则任务不在进行。
        // if (this.taskInfoBOMap.values().stream().filter(k->k.getPid() ==
        // this.getPid() && k.getTargetType() ==
        // TargetType.FirstRegisterLoginGame.ordinal() && k.getState() ==
        // TaskStateEnum.End.value()).findAny().isPresent()) {
        // return;
        // }

        PlayerFriendsHelpUnfoldRedPackBO friendsHelpUnfoldRedPackBO = this.taskInfoBOMap.values().stream()
                .filter(k -> k.getTargetType() == TargetType.FirstRegisterLoginGame.ordinal()
                        && k.getState() == TaskStateEnum.Receive.value())
                .findAny().orElse(null);
        if (null == friendsHelpUnfoldRedPackBO) {
            return;
        }
        // 检查是否已经获得新人红包奖励
        if (this.checkFriendsRedPackValue(PondType.NOVICE_POND.ordinal(), friendsHelpUnfoldRedPackBO.getValue())) {
            CommLogD.error("exeFirstRegisterLoginGame checkFriendsRedPackValue");
            return;
        }
        lock();
        friendsHelpUnfoldRedPackBO.saveState(TaskStateEnum.End.value());
        unlock();
        this.gainRoomCard(PondType.NOVICE_POND.ordinal(), friendsHelpUnfoldRedPackBO.getValue());
    }

    /**
     * 加入红包池
     *
     * @param toPid    目标Pid
     * @param redPack  红包
     * @param rewards  奖励
     * @param pondType 红包池类型
     */
    private void insertRedPackPondBO(long toPid, int redPack, String rewards, int pondType) {
        PlayerRedPackPondBO playerRedPackPondBO = new PlayerRedPackPondBO(this.getPid(), toPid, CommTime.nowSecond(),
                redPack, rewards, pondType);

        if (playerRedPackPondBO.getBaseService().save(playerRedPackPondBO) > 0L) {
            this.redPackPondBOs.add(playerRedPackPondBO);
            // 获得红包
            this.gainRoomCard(pondType, redPack);
        }
    }

    /**
     * 无限任务
     *
     * @param taskConfig 任务配置
     * @param toPid      目标Pid
     * @param redPack    红包
     * @return
     */
    private boolean infinite(FriendsHelpUnfoldRedPackBO taskConfig, long toPid, int redPack, boolean isHu) {
        // 3-无限任务
        if (this.redPackPondBOs.stream().filter(k -> k.getPondType() == taskConfig.getPondType() && k.getPid() == toPid)
                .findAny().isPresent()) {
            // 玩家已经执行过该任务。
            return false;
        }
        // 被邀请的玩家信息
        Player toPlayer = PlayerMgr.getInstance().getPlayer(toPid);
        if (null == toPlayer) {
            return false;
        }
        // 添加玩家红包记录
        if (this.insertPlayerRedPackRecordBO(toPid, redPack, isHu ? 1 : 0)) {
            // 加入红包池
            this.insertRedPackPondBO(toPid, redPack, taskConfig.getTitle(), taskConfig.getPondType());
        } else {
            return false;
        }
        // 帮拆红包通知
        this.getPlayer().pushProto(SFriendsRedPack_Notice.make(toPlayer.getShortPlayer(), redPack,
                TargetType.valueOf(taskConfig.getTargetType()), isHu));
        return true;
    }

    /**
     * 获得红包
     *
     * @param pondType 红包池类型
     * @param value    值
     * @return
     */
    private int gainRoomCard(int pondType, int value) {
        if (value <= 0) {
            return 0;
        }
        PlayerFriendsRedPackBO playerFriendsRedPackBO = this.getPlayerFriendsRedPackBO(pondType);
//		不可能为空
//		if (null == playerFriendsRedPackBO) {
//			return 0;
//		}
        this.lock();
        int before = 0;
        int finalValue = 0;
        before = playerFriendsRedPackBO.getValue();
        finalValue = Math.min(1999999999, before + value);
        playerFriendsRedPackBO.saveValue(finalValue);
        this.unlock();
        CommLogD.info("==获得金额== 玩家：{},消耗房卡数量：{},类型：{}", player.getId(), value, pondType);
        return finalValue - before;
    }

    /**
     * 消耗红包
     *
     * @param pondType 红包池类型
     * @param value    值
     * @return
     */
    private boolean consumeRoomCard(int pondType, int value) {
        if (value <= 0) {
            return false;
        }
        // 获取玩家圈卡数据
        PlayerFriendsRedPackBO playerFriendsRedPackBO = this.getPlayerFriendsRedPackBO(pondType);
        //不可能为空
//		if (null == playerFriendsRedPackBO) {
//			return false;
//		}
        int before = playerFriendsRedPackBO.getValue();
        if (before - value < 0) {
            return false;
        }
        this.lock();
        int finalValue = 0;
        finalValue = Math.max(0, before - value);
        playerFriendsRedPackBO.saveValue(finalValue);
        this.unlock();
        CommLogD.info("==消耗金额== 玩家：{},消耗房卡数量：{},类型：{}", player.getId(), value, pondType);
        return true;
    }

    /**
     * 提款信息
     *
     * @return
     */
    private DrawMoneyInfo getDrawMoneyInfo() {
        int friendHuPaiCount = 0;
        PlayerFriendsRedPackBO redPackBO = this.friendsRedPackMap.get(PondType.FRIZEND_HU_PAI_POND.ordinal());
        if (null == redPackBO) {
            friendHuPaiCount = 0;
        } else {
            // 好友胡牌
            friendHuPaiCount = redPackBO.getValue() / 100;
        }
        int dismantleValue = this.dismantleValue();
        // 无门槛红包计数
        int noThresholdCount = dismantleValue / TOTAL_MONEY;
        return new DrawMoneyInfo(getPlayer().getShortPlayer(), dismantleValue, friendHuPaiCount, noThresholdCount);
    }

    /**
     * 已拆得红包值
     *
     * @return
     */
    public int dismantleValue() {
        return this.friendsRedPackMap.values().stream().filter(k -> k.getPondType() != PondType.NOVICE_POND.ordinal())
                .map(k -> k.getValue()).reduce(0, Integer::sum);
    }

    /**
     * 执行取钱
     */
    public SData_Result<?> checkDrawMoney(int pondType) {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!Config.DE_DEBUG() && this.getPlayer().isTourist()) {
            return SData_Result.make(ErrorCode.NotAllow, "Tourist");
        }
        // 检查是否实名认证
        if (this.getPlayer().getPlayerBO().getPhone() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow, "error Phone");
        }
        int value = 0;
        switch (PondType.valueOf(pondType)) {
            case NOVICE_POND:
                value = this.getPlayerFriendsRedPackBO(PondType.NOVICE_POND.ordinal()).getValue();
                if (value > 0) {
                    return SData_Result.make(ErrorCode.Success, value);
                }
                break;
            case FRIZEND_HU_PAI_POND:
                // 新人红包
                if (this.checkFriendsRedPackValue(pondType, 100)) {
                    // 检查好友红包值是否满足
                    return SData_Result.make(ErrorCode.Success, 100);
                }
                break;
            case NOT_POND:
                // 无门槛红包提现
                value = dismantleValue();
                if (value >= TOTAL_MONEY) {
                    return SData_Result.make(ErrorCode.Success, value);
                }
                break;
            default:
                return SData_Result.make(ErrorCode.NotAllow, "error pondType");
        }
        return SData_Result.make(ErrorCode.NotAllow, String.format("error value:{%d}", value));
    }

    /**
     * 执行取钱
     */
    public SData_Result<?> exeDrawMoney(int pondType) {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!Config.DE_DEBUG() && this.getPlayer().isTourist()) {
            return SData_Result.make(ErrorCode.NotAllow, "Tourist");
        }
        // 检查是否实名认证
        if (this.getPlayer().getPlayerBO().getPhone() <= 0L) {
            return SData_Result.make(ErrorCode.NotAllow, "error Phone");
        }
        int value = 0;
        switch (PondType.valueOf(pondType)) {
            case NOVICE_POND:
                // 新人红包
                value = this.getPlayerFriendsRedPackBO(PondType.NOVICE_POND.ordinal()).getValue();
                if (value <= 0) {
                    return SData_Result.make(ErrorCode.NotAllow, "error NOVICE_POND");
                }
                this.consumeRoomCard(pondType, value);
                new PlayerRedPackDrawMoneyBO(this.getPid(), CommTime.nowSecond(), value, 0, 0).insert_sync();
                return SData_Result.make(ErrorCode.Success, value);
            case FRIZEND_HU_PAI_POND:
                // 好友胡牌池
                if (this.checkFriendsRedPackValue(pondType, 100)) {
                    // 检查好友红包值是否满足
                    this.consumeRoomCard(pondType, 100);
                    new PlayerRedPackDrawMoneyBO(this.getPid(), CommTime.nowSecond(), 100, 0, 0).insert_sync();
                    return SData_Result.make(ErrorCode.Success, 100);
                }
                break;
            case NOT_POND:
                // 无门槛红包提现
                value = dismantleValue();
                if (value >= TOTAL_MONEY) {
                    consumeRoomCard(PondType.NOT_POND.ordinal(),
                            this.getPlayerFriendsRedPackBO(PondType.NOT_POND.ordinal()).getValue());
                    consumeRoomCard(PondType.FRIZEND_HU_PAI_POND.ordinal(),
                            this.getPlayerFriendsRedPackBO(PondType.FRIZEND_HU_PAI_POND.ordinal()).getValue());
                    new PlayerRedPackDrawMoneyBO(this.getPid(), CommTime.nowSecond(), value, 0, 0).insert_sync();
                    return SData_Result.make(ErrorCode.Success, value);
                }
                break;
            default:
                return SData_Result.make(ErrorCode.NotAllow, "error pondType");
        }
        return SData_Result.make(ErrorCode.NotAllow, String.format("error value:{%d}", 0));
    }

    /**
     * 红包提现金额
     *
     * @return
     */
    public SData_Result<?> getPlayerRedPackDrawMoney() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }

        return SData_Result.make(ErrorCode.Success,
                ContainerMgr.get().getComponent(PlayerRedPackDrawMoneyBOService.class)
                        .findAll(Restrictions.eq("pid", this.getPid())).stream()
                        .map(k -> new PlayerRedPackDrawMoneyItem(k.getGetTime(), k.getValue()))
                        .collect(Collectors.toList()));
    }

    /**
     * 当前回合（本轮）打开红包记录
     *
     * @return
     */
    public SData_Result<?> getCurRoundOpenRedPackRecord() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.checkTimeIntervale()) {
            // 重新一轮
            return SData_Result.make(ErrorCode.NotAllow, "getCurRoundOpenRedPackRecord checkTimeIntervale");
        }
        return SData_Result.make(ErrorCode.Success, this.redPackPondBOs.stream()
                .map(k -> getCurRoundOpenRedPackRecordItem(k)).filter(k -> null != k).collect(Collectors.toList()));
    }

    /**
     * 获取好友帮拆红包主界面
     *
     * @return
     */
    public SData_Result<?> getFriendsHelpUnfoldRedPackInterface() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.checkTimeIntervale()) {
            // 重新一轮
            return SData_Result.make(ErrorCode.NotAllow, "");
        }
        boolean isNewPeople = !this.taskInfoBOMap.values().stream().filter(
                        k -> k.getTaskType() == TaskClassify.Novice.ordinal() && k.getState() == TaskStateEnum.End.value())
                .findAny().isPresent();

        isNewPeople = isNewPeople == !checkFriendsRedPackValue(PondType.NOVICE_POND.ordinal(), 1) ? true : false;

        // 红包将失效，赶紧拆
        DrawMoneyInfo drawMoneyInfo = this.getDrawMoneyInfo();
        return SData_Result.make(ErrorCode.Success,
                new FriendsHelpUnfoldRedPackInterfaceInfo(drawMoneyInfo.getDismantleValue(),
                        this.redPackTaskTimeBO.getStartTime(), this.redPackTaskTimeBO.getEndTime(),
                        drawMoneyInfo.getFriendHuPaiCount(), isNewPeople));
    }

    /**
     * 提款信息
     *
     * @return
     */
    public SData_Result<?> getDrawMoneyInfoResult() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        return SData_Result.make(ErrorCode.Success, this.getDrawMoneyInfo());
    }

    /**
     * 获取玩家红包记录
     *
     * @return
     */
    public SData_Result<?> getPlayerRedPackRecord() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.checkTimeIntervale()) {
            // 重新一轮
            return SData_Result.make(ErrorCode.NotAllow, "");
        }
        return SData_Result.make(ErrorCode.Success, this.redPackRecordBOs.stream()
                .map(k -> getPlayerRedPackRecordItem(k)).filter(k -> null != k).collect(Collectors.toList()));
    }

    /**
     * 获取新人红包
     *
     * @return
     */
    public SData_Result<?> getNewPeopleRedPack() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.checkTimeIntervale()) {
            // 重新一轮
            return SData_Result.make(ErrorCode.NotAllow, "getNewPeopleRedPack  checkTimeIntervale");
        }
        PlayerFriendsHelpUnfoldRedPackBO packBO = this.taskInfoBOMap.values().stream()
                .filter(k -> k.getTaskType() == TaskClassify.Novice.ordinal()).findAny().orElse(null);
        if (null == packBO) {
            return SData_Result.make(ErrorCode.NotAllow, "getNewPeopleRedPack null == packBO");
        }
        return SData_Result.make(ErrorCode.Success, SFriendsRedPack_Notice.make(this.getPlayer().getShortPlayer(),
                packBO.getValue(), TargetType.FirstRegisterLoginGame, packBO.getState() == TaskStateEnum.End.value()));
    }

    /**
     * 检查帮拆帮拆红包 1:用户第一个请求该接口，该接口返回错误，就现在拆红包界面。
     *
     * @return
     */
    public SData_Result<?> checkHelpUnpackRedPack() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.checkTimeIntervale()) {
            // 显示拆界面
            return SData_Result.make(ErrorCode.Success, SFriendsRedPack_Info.make(TargetType.OpenRedPack, null));
        }

        if (!this.taskInfoBOMap.values().stream().filter(k -> k.getTargetType() == TargetType.SharingGames.ordinal()
                && k.getState() > TaskStateEnum.None.value()).findAny().isPresent()) {
            // 显示分享再拆一个红包界面
            return SData_Result.make(ErrorCode.Success,
                    SFriendsRedPack_Info.make(TargetType.SharingGames,
                            new SharingGameInfo(this.getPlayer().getShortPlayer(), TOTAL_MONEY,
                                    FriendsHelpUnfoldRedPackMgr.getInstance()
                                            .getTargetTypeValue(TargetType.OpenRedPack.ordinal()))));
        }

        // 红包将失效，赶紧拆
        DrawMoneyInfo drawMoneyInfo = this.getDrawMoneyInfo();
        // 剩余红包
        int surplusValue = TOTAL_MONEY - drawMoneyInfo.getDismantleValue();
        if (surplusValue > 0) {
            // 显示界面
            return SData_Result
                    .make(ErrorCode.Success,
                            SFriendsRedPack_Info.make(TargetType.HelpToUnpackRedEnvelopes,
                                    new HelpUnpackRedPackInfo(drawMoneyInfo.getDismantleValue(), surplusValue,
                                            this.redPackTaskTimeBO.getStartTime(),
                                            this.redPackTaskTimeBO.getEndTime())));
        }
        return SData_Result.make(ErrorCode.Success, SFriendsRedPack_Info.make(TargetType.None, null));
    }

    /**
     * 拆红包
     *
     * @return
     */
    public SData_Result<?> openRedPack() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.exeTask(TargetType.OpenRedPack.ordinal())) {
            return SData_Result.make(ErrorCode.NotAllow, "exeTask OpenRedPack error");
        }
        // 新人红包
        this.exeTask(TargetType.FirstRegisterLoginGame.ordinal());
        return SData_Result.make(ErrorCode.Success, new SharingGameInfo(this.getPlayer().getShortPlayer(), TOTAL_MONEY,
                FriendsHelpUnfoldRedPackMgr.getInstance().getTargetTypeValue(TargetType.OpenRedPack.ordinal())));
    }

    /**
     * 分享游戏
     *
     * @return
     */
    public SData_Result<?> sharingGames() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!this.checkTimeIntervale()) {
            // 重新一轮
            return SData_Result.make(ErrorCode.NotAllow, "sharingGames checkTimeIntervale error");
        }
        if (!this.exeTask(TargetType.SharingGames.ordinal())) {
            return SData_Result.make(ErrorCode.NotAllow, "exeTask SharingGames error");
        }
        // 红包将失效，赶紧拆
        DrawMoneyInfo drawMoneyInfo = this.getDrawMoneyInfo();
        // 剩余红包
        int surplusValue = TOTAL_MONEY - drawMoneyInfo.getDismantleValue();
        return SData_Result.make(ErrorCode.Success,
                new SharingGameFriendsHelpInfo(
                        FriendsHelpUnfoldRedPackMgr.getInstance().getTargetTypeValue(TargetType.SharingGames.ordinal()),
                        surplusValue));
    }

    /**
     * 检查红包任务
     *
     * @return
     */
    public SData_Result<?> checkRedPackTaskShow() {
        if (!FriendsHelpUnfoldRedPackMgr.getInstance().checkTaskTimeIntervale()) {
            // 关闭红包任务
            this.closeRedPackTask();
            return SData_Result.make(ErrorCode.Activity_Close, "closeRedPackTask");
        }
        if (!Config.DE_DEBUG() && this.getPlayer().isTourist()) {
            return SData_Result.make(ErrorCode.Activity_Close, "Tourist closeRedPackTask");
        }
        return SData_Result.make(ErrorCode.Success);
    }

}
