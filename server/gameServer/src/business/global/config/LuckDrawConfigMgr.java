package business.global.config;

import cenum.LuckDrawEnum.LuckDrawAssignCrowd;
import cenum.LuckDrawEnum.LuckDrawDateType;
import cenum.LuckDrawEnum.LuckDrawTimeSlot;
import cenum.LuckDrawEnum.LuckDrawType;
import com.ddm.server.common.utils.CommTime;
import com.google.common.collect.Lists;
import core.db.entity.clarkGame.LuckDrawConfigBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.LuckDrawConfigBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.luckdraw.AssignCrowdAndConditionItem;
import lombok.Data;

import java.util.List;
import java.util.Objects;


@Data
public class LuckDrawConfigMgr {


    private static LuckDrawConfigMgr instance = new LuckDrawConfigMgr();
    /**
     * 抽奖配置表
     */
    private LuckDrawConfigBO luckDrawConfigBO;

    public static LuckDrawConfigMgr getInstance() {
        return instance;
    }


    public void init() {
        this.setLuckDrawConfigBO(ContainerMgr.get().getComponent(LuckDrawConfigBOService.class).findOne(Restrictions.ge("id", 1).desc("id"), null));
    }

    public boolean updateConfig(long id) {
        this.setLuckDrawConfigBO(ContainerMgr.get().getComponent(LuckDrawConfigBOService.class).findOne(Restrictions.ge("id", id), null));
        return true;
    }

    /**
     * 检查抽奖活动是否开启
     *
     * @return
     */
    public boolean checkLuckDraw() {
        if (Objects.isNull(this.getLuckDrawConfigBO())) {
            return false;
        }
        if (getLuckDrawConfigBO().getEndTime() <= 0) {
            // 结束时间 <= 0，结束状态
            return false;
        }
        LuckDrawDateType dateTypeEnum = LuckDrawDateType.valueOf(this.getLuckDrawConfigBO().getDateType());
        switch (dateTypeEnum) {
            // 每周
            case WEEKLY:
                return weekly(this.getLuckDrawConfigBO().getStartTime(), this.getLuckDrawConfigBO().getEndTime(), LuckDrawTimeSlot.valueOf(this.getLuckDrawConfigBO().getTimeSlot()));
            // 每天
            case EVERYDAY:
                return everyDay(this.getLuckDrawConfigBO().getStartTime(), this.getLuckDrawConfigBO().getEndTime(), LuckDrawTimeSlot.valueOf(this.getLuckDrawConfigBO().getTimeSlot()));
            // 具体日期
            case EXACT_DATE:
                return exctDate(this.getLuckDrawConfigBO().getStartTime(), this.getLuckDrawConfigBO().getEndTime(), LuckDrawTimeSlot.valueOf(this.getLuckDrawConfigBO().getTimeSlot()));
            default:
                return false;
        }
    }

    /**
     * 检查抽奖
     *
     * @param assignCrowdEnum 人群
     * @param id              id
     * @return
     */
    public boolean checkLuckDraw(long pid, LuckDrawAssignCrowd assignCrowdEnum, long id) {
        return this.checkLuckDraw(pid, assignCrowdEnum, Lists.newArrayList(id));
    }

    /**
     * 检查抽奖
     *
     * @param assignCrowdEnum 人群
     * @param ids             列表
     * @return
     */
    public boolean checkLuckDraw(long pid, LuckDrawAssignCrowd assignCrowdEnum, List<Long> ids) {
        if (checkLuckDraw()) {
            return this.checkAssignCrowd(pid, assignCrowdEnum, ids);
        }
        return false;
    }

    /**
     * 检查人群
     *
     * @param assignCrowdEnum 人群
     * @param id              id
     * @return
     */
    public boolean checkAssignCrowd(long pid, LuckDrawAssignCrowd assignCrowdEnum, long id) {
        return checkAssignCrowd(pid, assignCrowdEnum, Lists.newArrayList(id));
    }

    /**
     * 检查人群
     *
     * @param assignCrowdEnum 人群
     * @param ids             人群列表
     * @return
     */
    public boolean checkAssignCrowd(long pid, LuckDrawAssignCrowd assignCrowdEnum, List<Long> ids) {
        LuckDrawAssignCrowd luckDrawAssignCrowd = LuckDrawAssignCrowd.valueOf(this.getLuckDrawConfigBO().getAssignCrowd());
//        String key = String.format(DataConstants.LUCK_DRAW_ASSIGN_CROWD, pid, luckDrawAssignCrowd.name());
//        AssignCrowdResult result = EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).get(key, AssignCrowdResult.class);
//        if (Objects.nonNull(result)) {
//            return result.isExist();
//        }
        if (LuckDrawAssignCrowd.ALL.equals(luckDrawAssignCrowd)) {
            // 针对所有人群
            return true;
        }
        if (assignCrowdEnum.equals(luckDrawAssignCrowd)) {
            // 针对指定人群
            boolean isExist = ids.stream().anyMatch(k -> this.getLuckDrawConfigBO().getAssignCrowdValueList().contains(k));
//            EhCacheFactory.getCacheApi(SqlCacheConfiguration.class).put(key, new AssignCrowdResult(isExist));
            return isExist;
        }
        return false;
    }


    /**
     * 每天
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param timeSlot  时间段
     * @return
     */
    private boolean everyDay(int startTime, int endTime, LuckDrawTimeSlot timeSlot) {
        if (LuckDrawTimeSlot.ALL_DAY.equals(timeSlot)) {
            return true;
        }
        return CommTime.checkEveryDayTimeIntervale(startTime, endTime);
    }


    /**
     * 每周
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param timeSlot  时间段
     * @return
     */
    private boolean weekly(int startTime, int endTime, LuckDrawTimeSlot timeSlot) {
        if (LuckDrawTimeSlot.ALL_DAY.equals(timeSlot)) {
            return CommTime.checkWeeklyTimeAllDay(startTime, endTime);
        }
        return CommTime.checkWeeklyTimeIntervale(startTime, endTime);

    }

    /**
     * 具体日期
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param timeSlot  时间段
     * @return
     */
    private boolean exctDate(int startTime, int endTime, LuckDrawTimeSlot timeSlot) {
        if (LuckDrawTimeSlot.ALL_DAY.equals(timeSlot)) {
            return CommTime.checkTimeAllDay(startTime, endTime);
        }
        return CommTime.checkTimeIntervale(startTime, endTime);

    }

    /**
     * 获取人群枚举类型
     *
     * @return
     */
    public LuckDrawAssignCrowd getAssignCrowd() {
        if (Objects.isNull(this.getLuckDrawConfigBO())) {
            return null;
        }
        return LuckDrawAssignCrowd.valueOf(this.getLuckDrawConfigBO().getAssignCrowd());
    }


    /**
     * 抽奖次数
     *
     * @return
     */
    public int luckDrawValue(int type) {
        LuckDrawType luckDrawType = LuckDrawType.valueOf(type);
        if (LuckDrawType.FREE.equals(luckDrawType)) {
            return this.getLuckDrawConfigBO().getCostFreeluckDrawValue();
        }
        return 0;
    }

    /**
     * 抽奖次数
     *
     * @return
     */
    public int luckDrawValue() {
        if (Objects.isNull(this.getLuckDrawConfigBO())) {
            return 0;
        }
        return this.getLuckDrawConfigBO().getLuckDrawValue();
    }

    public int getConditionValue() {
        if (Objects.isNull(this.getLuckDrawConfigBO())) {
            return 0;
        }
        return this.getLuckDrawConfigBO().getConditionValue();
    }


    /**
     * 获取抽奖条件
     *
     * @return
     */
    public int getCondition() {
        if (Objects.isNull(this.getLuckDrawConfigBO())) {
            return 0;
        }
        return this.getLuckDrawConfigBO().getCondition();
    }

    /**
     * 指定人群和抽奖条件
     */
    public AssignCrowdAndConditionItem getAssignCrowdAndCondition() {
        LuckDrawAssignCrowd assignCrowd = LuckDrawConfigMgr.getInstance().getAssignCrowd();
        if (Objects.isNull(assignCrowd)) {
            // 没有配置
            return null;
        }
        LuckDrawType luckDrawType = LuckDrawType.valueOf(getCondition());
        if (LuckDrawType.FREE.equals(luckDrawType)) {
            // 没有配置
            return null;
        }
        return new AssignCrowdAndConditionItem(luckDrawType, assignCrowd);
    }

}
