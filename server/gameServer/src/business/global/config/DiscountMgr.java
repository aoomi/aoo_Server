package business.global.config;

import BaseThread.BaseMutexObject;
import business.global.sharediscount.ShareDiscountMgr;
import cenum.CrowdTypeEnum;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.DiscountBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.DiscountBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.task.TaskConfigEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 打折（免费）活动
 *
 * @author Administrator
 */
public class DiscountMgr {
    private static class SingletonHolder {
        public static DiscountMgr instance = new DiscountMgr();
    }

    private DiscountMgr() {
    }

    public static DiscountMgr getInstance() {
        return SingletonHolder.instance;
    }

    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    // 打折字典
    private Map<Long, DiscountBO> discountMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    public void init() {
        List<DiscountBO> discountBOList = ContainerMgr.get().getComponent(DiscountBOService.class).findAll(null);
        if (CollectionUtils.isNotEmpty(discountBOList)) {
            discountMap = discountBOList.stream().collect(Collectors.toMap(k -> k.getId(), k -> k, (k1, k2) -> k1));
            for (DiscountBO discountBO : discountBOList) {
                ShareDiscountMgr.getInstance().addDiscount(discountBO);
            }
        }
    }

    /**
     * 修改打折（免费）活动
     *
     * @param id        打折ID
     * @param crowdType 1：全体玩家、2：代理及名下玩家、3：除代理及名下玩家外其他玩家
     * @param crowdList 代理ID列表
     * @param gameList  游戏ID列表
     * @param value     0-100，0表示免费，100表示无打折；
     * @param dateType  1：每日、2：每周、3：具体日期；
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public boolean setDiscount(long id, int crowdType, String crowdList, String gameList, int value,
                               int dateType, int startTime, int endTime, int state) {
        DiscountBO dBo;
        if (Config.isShare()) {
            dBo = ShareDiscountMgr.getInstance().getDiscountBO(id);
        } else {
            dBo = this.discountMap.get(id);
        }
        dBo = Objects.isNull(dBo) ? new DiscountBO() : dBo;
        dBo.setCrowdType(crowdType);
        dBo.setCrowdList(crowdList);
        dBo.setGameList(gameList);
        dBo.setValue(value);
        dBo.setDateType(dateType);
        dBo.setStartTime(startTime);
        dBo.setEndTime(endTime);
        dBo.setState(state);
        long saveOrUpdateValue = dBo.getBaseService().saveOrUpDate(dBo);
        if (saveOrUpdateValue > 0L) {
            this.discountMap.put(dBo.getId(), dBo);
            if (Config.isShare()) {
                ShareDiscountMgr.getInstance().addDiscount(dBo);
            }
            return true;
        }
        return false;
    }

    /**
     * 删除打折（免费）活动
     */
    public boolean delDiscount(long id) {
        DiscountBO dBo;
        if (Config.isShare()) {
            dBo = ShareDiscountMgr.getInstance().getDiscountBO(id);
        } else {
            dBo = this.discountMap.get(id);
        }
        if (Objects.nonNull(dBo)) {
            dBo.getBaseService().delete(Restrictions.eq("id", id));
            this.discountMap.remove(id);
            if (Config.isShare()) {
                ShareDiscountMgr.getInstance().deleteDiscount(id);
            }
            return true;
        }
        return false;

    }


    public int calculateRoomCardCost(List<Long> familyIdList, long clubId, long unionId, int gameId, int roomCard) {
        // 获取打折值
        int value = this.getDiscountValue(familyIdList, clubId, unionId, gameId);
        return (value < 0 || value >= 100) ? roomCard : (int) Math.ceil((value / 100D) * roomCard);
    }


    /**
     * 折扣值
     *
     * @param familyIdList
     * @param clubId
     * @param unionId
     * @param gameId
     * @return
     */
    public int getValue(List<Long> familyIdList, long clubId, long unionId, int gameId) {
        int value = getDiscountValue(familyIdList, clubId, unionId, gameId);
        return value >= 100 ? 100 : value;
    }


    /**
     * 获取打折信息值
     *
     * @param familyIdList 代理Id
     * @param clubId       亲友圈Id
     * @param unionId      赛事Id
     * @param gameId       游戏Id
     * @return -1:无效值
     */
    public int getDiscountValue(List<Long> familyIdList, long clubId, long unionId, int gameId) {
        Map<Long, DiscountBO> discountMap;
        if (Config.isShare()) {
            discountMap = ShareDiscountMgr.getInstance().getAllDiscount();
        } else {
            discountMap = this.discountMap;
        }
        if (MapUtils.isEmpty(discountMap)) {
            return -1;
        }
        Optional<DiscountBO> discountBOOptional = discountMap.values().stream().filter(k -> {
            if (k.getState() == 1 || !checkTimeinterval(k)) {
                // 活动未开启或者没在时间区间内
                return false;
            }
            if (k.getCrowdType() == CrowdTypeEnum.ALL_PLAYER.value()) {
                // 全体玩家
                return k.gameContains(gameId);
            } else if (k.getCrowdType() == CrowdTypeEnum.AGENTS_AND_PLAYER.value() && CollectionUtils.isNotEmpty(familyIdList)) {
                // 代理及名下玩家 || 亲友圈
                return familyIdList.stream().anyMatch(familyId -> k.crowdContains(familyId)) && k.gameContains(gameId);
            } else if (k.getCrowdType() == CrowdTypeEnum.NOT_AGENTS_AND_PLAYER.value()) {
                if (CollectionUtils.isEmpty(familyIdList)) {
                    return k.gameContains(gameId);
                }
                // 除代理及名下玩家外其他玩家
                return !familyIdList.stream().allMatch(familyId -> k.crowdContains(familyId)) && k.gameContains(gameId);
            } else if (k.getCrowdType() == CrowdTypeEnum.CLUB.value() && clubId > 0L) {
                return k.crowdContains(clubId) && k.gameContains(gameId);
            } else if (k.getCrowdType() == CrowdTypeEnum.UNION.value() && unionId > 0L) {
                return k.crowdContains(unionId) && k.gameContains(gameId);
            }
            return false;
        }).sorted(Comparator.comparing(DiscountBO::getValue)).findFirst();
        return discountBOOptional.map(DiscountBO::getValue).orElse(-1);
    }

    /**
     * 检查任务时间区间
     * 开始、结束时间 <=0,不限制活动时间
     *
     * @return T:在活动时间内,F:不在活动时间内
     */
    private boolean checkTimeinterval(DiscountBO dBO) {
        TaskConfigEnum.TaskTimeEnum timeEnum = TaskConfigEnum.TaskTimeEnum.valueOf(dBO.getDateType());
        switch (timeEnum) {
            case Everyday:
                // 每天
                return CommTime.checkEveryDayTimeIntervale(dBO.getStartTime(), dBO.getEndTime());
            case Weekly:
                // 每周
                return CommTime.checkWeeklyTimeIntervale(dBO.getStartTime(), dBO.getEndTime());
            case TimeTnterval:
                // 时间区间
                return CommTime.checkTimeIntervale(dBO.getStartTime(), dBO.getEndTime());
            default:
                break;
        }
        return false;
    }


}
