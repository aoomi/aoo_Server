package business.global.config;

import BaseThread.BaseMutexObject;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.FriendsHelpUnfoldRedPackBO;
import core.db.entity.clarkGame.FriendsHelpUnfoldRedPackConfigBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.FriendsHelpUnfoldRedPackBOService;
import core.db.service.clarkGame.FriendsHelpUnfoldRedPackConfigBOService;
import core.db.service.clarkGame.PlayerFriendsHelpUnfoldRedPackBOService;
import core.ioc.ContainerMgr;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 好友帮拆红包
 *
 * @author Administrator
 */
@Data
public class FriendsHelpUnfoldRedPackMgr {
    private static FriendsHelpUnfoldRedPackMgr instance = new FriendsHelpUnfoldRedPackMgr();
    /**
     * 任务类型:任务配置s
     */
    private Map<Long, FriendsHelpUnfoldRedPackBO> taskConfigMap = new ConcurrentHashMap<>();
    /**
     * 好友帮拆红包配置
     */
    private FriendsHelpUnfoldRedPackConfigBO friendsHelpUnfoldRedPackConfigBO;

    public static FriendsHelpUnfoldRedPackMgr getInstance() {
        return instance;
    }

    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    /**
     * 初始化
     */
    public void init() {
        // 好友帮拆红包任务
        ContainerMgr.get().getComponent(FriendsHelpUnfoldRedPackBOService.class).findAll(null).stream().filter(k -> null != k).forEach(k -> {
            this.taskConfigMap.put(k.getId(), k);
        });
        // 好友帮拆红包配置
        this.setFriendsHelpUnfoldRedPackConfigBO(ContainerMgr.get().getComponent(FriendsHelpUnfoldRedPackConfigBOService.class).findAll(null).stream().findFirst().orElse(null));
    }

    /**
     * 获取好友帮拆红包配置
     *
     * @return
     */
    public FriendsHelpUnfoldRedPackConfigBO getFriendsHelpUnfoldRedPackConfigBO() {
        return friendsHelpUnfoldRedPackConfigBO;
    }

    /**
     * 获取好友帮拆红包配置
     *
     * @return
     */
    public FriendsHelpUnfoldRedPackConfigBO getFriendsHelpUnfoldRedPackConfigInfo() {
        if (null == friendsHelpUnfoldRedPackConfigBO) {
            return new FriendsHelpUnfoldRedPackConfigBO();
        }
        return friendsHelpUnfoldRedPackConfigBO;
    }


    /**
     * 删除任务
     *
     * @param id 任务ID
     * @return
     */
    public boolean delTask(long id) {
        try {
            lock();
            FriendsHelpUnfoldRedPackBO tConfig = this.taskConfigMap.remove(id);
            // 检查移除的数据是否存在
            if (null != tConfig) {
                ContainerMgr.get().getComponent(FriendsHelpUnfoldRedPackBOService.class).delete(id);
                ContainerMgr.get().getComponent(PlayerFriendsHelpUnfoldRedPackBOService.class).delete(Restrictions.eq("taskId", tConfig.getId()));
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
    public boolean setFriendsHelpUnfoldRedPackConfig(int startTime, int endTime, int limitType, int limitTime) {
        try {
            lock();
            if (null == this.friendsHelpUnfoldRedPackConfigBO) {
                this.setFriendsHelpUnfoldRedPackConfigBO(new FriendsHelpUnfoldRedPackConfigBO());
            }
            this.friendsHelpUnfoldRedPackConfigBO.setStartTime(startTime);
            this.friendsHelpUnfoldRedPackConfigBO.setEndTime(endTime);
            this.friendsHelpUnfoldRedPackConfigBO.setLimitType(limitType);
            this.friendsHelpUnfoldRedPackConfigBO.setLimitTime(limitTime);
            this.friendsHelpUnfoldRedPackConfigBO.getBaseService().saveOrUpDate(friendsHelpUnfoldRedPackConfigBO, new AsyncInfo(this.friendsHelpUnfoldRedPackConfigBO.getId()));
        } finally {
            unlock();
        }
        return true;
    }

    /**
     * 设置任务
     * ID不存在新增，否则修改
     *
     * @return
     */
    public boolean setTask(long id, int taskType, String title, String content, int targetValue, int targetType, long preTaskId, int redPack, int pondType) {
        try {
            lock();
            FriendsHelpUnfoldRedPackBO tConfig = this.taskConfigMap.get(id);
            if (null == tConfig) {
                tConfig = new FriendsHelpUnfoldRedPackBO();
            }
            tConfig.setTaskType(taskType);
            tConfig.setTitle(title);
            tConfig.setContent(content);
            tConfig.setTargetValue(targetValue);
            tConfig.setTargetType(targetType);
            tConfig.setPreTaskId(preTaskId);
            tConfig.setValue(redPack);
            tConfig.setPondType(pondType);
            tConfig.getBaseService().saveOrUpDate(tConfig, new AsyncInfo(tConfig.getId()));
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
    public List<FriendsHelpUnfoldRedPackBO> getTaskList() {
        return this.taskConfigMap.values().stream().sorted(Comparator.comparing(FriendsHelpUnfoldRedPackBO::getId)).collect(Collectors.toList());
    }

    /**
     * 任务配置
     *
     * @param id 任务ID
     * @return
     */
    public FriendsHelpUnfoldRedPackBO taskConfigMap(long id) {
        // 任务配置
        return this.taskConfigMap.get(id);
    }

    /**
     * 获取同类型的任务ID
     *
     * @param targetType 目标类型
     * @return
     */
    public List<Long> getTaskIDList(int targetType) {
        return this.taskConfigMap.values().stream().filter(k -> k.getTargetType() == targetType).map(k -> k.getId()).collect(Collectors.toList());
    }

    /**
     * 检查是否在任务时间区间内
     *
     * @return
     */
    public boolean checkTaskTimeIntervale() {
        if (null == this.friendsHelpUnfoldRedPackConfigBO) {
            return false;
        }
        return CommTime.checkTimeIntervale(this.getFriendsHelpUnfoldRedPackConfigBO().getStartTime(), this.getFriendsHelpUnfoldRedPackConfigBO().getEndTime());
    }

    /**
     * 获取玩家限制时间
     *
     * @param startTime 开始时间
     * @return
     */
    public int getEndLimitTime(int startTime) {
        return (int) (new DateTime(startTime * 1000L).plusHours(this.getFriendsHelpUnfoldRedPackConfigBO().getLimitTime()).getMillis() / 1000);
    }


    /**
     * 获取指定目标任务类型值
     *
     * @param targetType
     * @return
     */
    public int getTargetTypeValue(int targetType) {
        FriendsHelpUnfoldRedPackBO friendsHelpUnfoldRedPackBO = this.taskConfigMap.values().stream().filter(k -> k.getTargetType() == targetType).findAny().orElse(null);
        if (null == friendsHelpUnfoldRedPackBO) {
            return 0;
        } else {
            return friendsHelpUnfoldRedPackBO.getValue();
        }
    }

}
