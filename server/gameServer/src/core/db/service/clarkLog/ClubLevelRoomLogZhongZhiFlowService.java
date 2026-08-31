package core.db.service.clarkLog;

import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.annotation.Service;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubLevelRoomLogZhongZhiFlow;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.ClubPromotionLevelItem;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service(source = "clark_log")
public class ClubLevelRoomLogZhongZhiFlowService implements BaseService<ClubLevelRoomLogZhongZhiFlow> {
    private BaseClarkLogDao<ClubLevelRoomLogZhongZhiFlow> clubLevelRoomLogFlowDao = new BaseClarkLogDao<>(ClubLevelRoomLogZhongZhiFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubLevelRoomLogFlowDao;
    }


    /**
     * 查询所有（同步
     *
     * @param criteria   criteria 策略器
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @return
     */
    public <E> List<E> getRoomSizeList(Criteria criteria, Class<E> clazz, String selectHead) {
        String sql = "select t.clubId,t.date_time,sum(t.roomId > 0) as roomSize from (SELECT roomId,clubId,date_time FROM `ClubLevelRoomLogZhongZhi` where date_time = ?  GROUP BY roomId,clubId) as t GROUP BY t.clubId";
        return (List<E>) getDefaultDao().listBeanByClass(sql, clazz, Arrays.asList(CommTime.getYesterDayStringYMD(1)).toArray(new Object[1]));
    }

    /**
     * 创建者数据统计
     *
     * @param clubId
     */
    public ClubPromotionLevelItem findOneClubCreate(long clubId) {
        // 缓存key
        String cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_CREATE.getKey(CommTime.getNowTimeStringYMD(), clubId);
        // 获取缓存数据
        ClubPromotionLevelItem clubPromotionLevelCacheItem = this.getClubPromotionLevelCacheItem(cacheKey);
        if (clubPromotionLevelCacheItem.getMaxId() > 0L && CommTime.nowSecond() - clubPromotionLevelCacheItem.getTimestamp() <= 10) {
            // 10秒内的缓存数据
            return clubPromotionLevelCacheItem;
        }
        // 查询实时表数据
        ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.gt("id", clubPromotionLevelCacheItem.getMaxId())), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsNameMaxId());
        return this.resultClubPromotionLevelItem(cacheKey, clubPromotionLevelCacheItem, Objects.nonNull(clubPromotionLevelItem) ? clubPromotionLevelItem : new ClubPromotionLevelItem());
    }

    /**
     * 推广员数据统计
     *
     * @param clubId
     */
    public ClubPromotionLevelItem findOneClubPromotionLevel(long clubId, long memberId, long upLevelId, int level, List<Long> uidList) {
        // 缓存key
        String cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_LEVEL.getKey(CommTime.getNowTimeStringYMD(), clubId, upLevelId, memberId, level);
        // 获取缓存数据
        ClubPromotionLevelItem clubPromotionLevelCacheItem = this.getClubPromotionLevelCacheItem(cacheKey);
        if (clubPromotionLevelCacheItem.getMaxId() > 0L && CommTime.nowSecond() - clubPromotionLevelCacheItem.getTimestamp() <= 10) {
            // 10秒内的缓存数据
            return clubPromotionLevelCacheItem;
        }
        // 查询实时表数据
        ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.gt("id", clubPromotionLevelCacheItem.getMaxId()), Restrictions.in("memberId", uidList)), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsNameMaxId());
        return this.resultClubPromotionLevelItem(cacheKey, clubPromotionLevelCacheItem, Objects.nonNull(clubPromotionLevelItem) ? clubPromotionLevelItem : new ClubPromotionLevelItem());

    }

    /**
     * 创建者统计普通成员数据
     *
     * @param clubId
     */
    public ClubPromotionLevelItem findOneClubGeneral(long clubId, long memberId, long upLevelId, int level, List<Long> uidList) {
        // 缓存key
        String cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_GENERAL.getKey(CommTime.getNowTimeStringYMD(), clubId, upLevelId, memberId, level);
        // 获取缓存数据
        ClubPromotionLevelItem clubPromotionLevelCacheItem = this.getClubPromotionLevelCacheItem(cacheKey);
        if (clubPromotionLevelCacheItem.getMaxId() > 0L && CommTime.nowSecond() - clubPromotionLevelCacheItem.getTimestamp() <= 10) {
            // 10秒内的缓存数据
            return clubPromotionLevelCacheItem;
        }
        // 查询实时表数据
        ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.gt("id", clubPromotionLevelCacheItem.getMaxId()), Restrictions.in("memberId", uidList)), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsNameMaxId());
        return this.resultClubPromotionLevelItem(cacheKey, clubPromotionLevelCacheItem, Objects.nonNull(clubPromotionLevelItem) ? clubPromotionLevelItem : new ClubPromotionLevelItem());
    }

    /**
     * 普通成员数据统计
     *
     * @param clubId
     */
    public ClubPromotionLevelItem findOneClubGeneral(long clubId, long memberId, long upLevelId, int level) {
//        // 缓存key
//        String cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_GENERAL.getKey(clubId, memberId, upLevelId, level,CommTime.getNowTimeStringYMD());
//        // 获取缓存数据
//        ClubPromotionLevelItem clubPromotionLevelCacheItem = this.getClubPromotionLevelCacheItem(cacheKey);
//        if (clubPromotionLevelCacheItem.getMaxId() > 0L && CommTime.nowSecond() - clubPromotionLevelCacheItem.getTimestamp() <= 10) {
//            // 10秒内的缓存数据
//            return clubPromotionLevelCacheItem;
//        }
        // 查询实时表数据
        ClubPromotionLevelItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).findOneE(Restrictions.and(Restrictions.eq("clubID", clubId), Restrictions.eq("memberId", memberId)), ClubPromotionLevelItem.class, ClubPromotionLevelItem.getItemsNameMaxId());
        return Objects.nonNull(clubPromotionLevelItem) ? clubPromotionLevelItem : new ClubPromotionLevelItem();
    }

    /**
     * 获取缓存数据
     *
     * @param cacheKey 缓存key
     * @return
     */
    private ClubPromotionLevelItem getClubPromotionLevelCacheItem(String cacheKey) {
        // 获取缓存数据
        ClubPromotionLevelItem clubPromotionLevelCacheItem = ContainerMgr.get().getRedis().getObject(cacheKey, ClubPromotionLevelItem.class);
        clubPromotionLevelCacheItem = Objects.isNull(clubPromotionLevelCacheItem) ? new ClubPromotionLevelItem() : clubPromotionLevelCacheItem;
        return clubPromotionLevelCacheItem;
    }

    /**
     * 结果数据
     *
     * @param cacheKey                    缓存key
     * @param clubPromotionLevelCacheItem 缓存数据
     * @param clubPromotionLevelItem      实时数据
     * @return
     */
    public ClubPromotionLevelItem resultClubPromotionLevelItem(String cacheKey, ClubPromotionLevelItem clubPromotionLevelCacheItem, ClubPromotionLevelItem clubPromotionLevelItem) {
        if (clubPromotionLevelItem.getMaxId() <= 0) {
            // 没有数据
            return clubPromotionLevelCacheItem;
        }
        // 时间
        clubPromotionLevelCacheItem.setTimestamp(CommTime.nowSecond());
        //最大id
        clubPromotionLevelCacheItem.setMaxId(clubPromotionLevelCacheItem.getMaxId() > 0L && clubPromotionLevelItem.getMaxId() <= 0L ? clubPromotionLevelCacheItem.getMaxId() : clubPromotionLevelItem.getMaxId());
        // 局数
        clubPromotionLevelCacheItem.setSetCount(clubPromotionLevelCacheItem.getSetCount() + clubPromotionLevelItem.getSetCount());
        // 赢数
        clubPromotionLevelCacheItem.setWinner(clubPromotionLevelCacheItem.getWinner() + clubPromotionLevelItem.getWinner());
        // 报名费
        clubPromotionLevelCacheItem.setEntryFee(CommMath.addDouble(clubPromotionLevelCacheItem.getEntryFee(), clubPromotionLevelItem.getEntryFee()));
        // 消耗
        clubPromotionLevelCacheItem.setConsume(clubPromotionLevelCacheItem.getConsume() + clubPromotionLevelItem.getConsume());
        // 消耗比赛分(理论报名费  房间消耗/房间人数)
        clubPromotionLevelCacheItem.setSportsPointConsume(CommMath.addDouble(clubPromotionLevelCacheItem.getSportsPointConsume(), clubPromotionLevelItem.getSportsPointConsume()));
        // 推广员战绩分成
        clubPromotionLevelCacheItem.setPromotionShareValue(CommMath.addDouble(clubPromotionLevelCacheItem.getPromotionShareValue(), clubPromotionLevelItem.getPromotionShareValue()));
        // 实际报名费
        clubPromotionLevelCacheItem.setActualEntryFee(CommMath.addDouble(clubPromotionLevelCacheItem.getActualEntryFee(), clubPromotionLevelItem.getActualEntryFee()));
        ContainerMgr.get().getRedis().putWithTime(cacheKey, CommTime.RemainingTime() + CommMath.randomInt(100, 500), new Gson().toJson(clubPromotionLevelCacheItem));
        return clubPromotionLevelCacheItem;
    }

    /**
     * 推广员数据统计
     * 包括推广员的数据
     *
     * @param clubId
     */
    public ClubPromotionLevelItem findOneClubPromotionLevelPlayGameId(long clubId, long memberId, long upLevelId, int level, long playGamePid, long upPid, int type) {
        // 缓存key
        String cacheKey = RedisBydrKeyEnum.CLUB_PROMOTION_LEVEL_PLAYGAMID.getKey(CommTime.getYesterDayStringYMD(type), clubId, upLevelId, memberId, level);
        // 获取缓存数据
        ClubPromotionLevelItem clubPromotionLevelCacheItem = this.getClubPromotionLevelCacheItem(cacheKey);

        if (clubPromotionLevelCacheItem.getMaxId() > 0L && CommTime.nowSecond() - clubPromotionLevelCacheItem.getTimestamp() <= 10) {
            // 10秒内的缓存数据
            return clubPromotionLevelCacheItem;
        }
        String dateTime;
        if (type == 0) {
            dateTime = String.valueOf(CommTime.getCycleNowTime6YMD());
        } else {
            dateTime = CommTime.getYesterDay6ByCount(type);

        }
        //根据type 获取表名
        // 查询实时表数据
        List<ClubPromotionLevelItem> clubPromotionLevelItemList = ContainerMgr.get().getComponent(ClubLevelRoomLogZhongZhiFlowService.class).getClubPromotionListZhongZhi(dateTime, clubId, upPid, playGamePid, ClubPromotionLevelItem.class);
        ClubPromotionLevelItem clubPromotionLevelItem = null;
        if (CollectionUtils.isNotEmpty(clubPromotionLevelItemList)) {
            clubPromotionLevelItem = clubPromotionLevelItemList.get(0);
        }
        return this.resultClubPromotionLevelItemZhongZhi(cacheKey, clubPromotionLevelCacheItem, Objects.nonNull(clubPromotionLevelItem) ? clubPromotionLevelItem : new ClubPromotionLevelItem());

    }

    /**
     * 查询所有（同步
     *
     * @param clazz 欲执行查询类
     * @return
     */
    public <E> List<E> getClubPromotionListZhongZhi(String dateTime, long clubId, long pid, long playGamePid, Class<E> clazz) {
        String sql = "select max(id) as maxId,sum(setCount) as setCount,sum(winner) as winner,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(promotionShareValue) as promotionShareValue,sum(roomSportsPointConsume) as actualEntryFee" +
                "  from `ClubLevelRoomLogZhongZhi" + dateTime + "` where clubId = ? and pid = ? and playGamePid = ? ";
        return (List<E>) getDefaultDao().listBeanByClass(sql, clazz, Arrays.asList(clubId, pid, playGamePid).toArray(new Object[3]));
    }

    /**
     * 结果数据
     *
     * @param cacheKey                    缓存key
     * @param clubPromotionLevelCacheItem 缓存数据
     * @param clubPromotionLevelItem      实时数据
     * @return
     */
    public ClubPromotionLevelItem resultClubPromotionLevelItemZhongZhi(String cacheKey, ClubPromotionLevelItem clubPromotionLevelCacheItem, ClubPromotionLevelItem clubPromotionLevelItem) {
        if (clubPromotionLevelItem.getMaxId() <= 0) {
            // 没有数据
            return clubPromotionLevelCacheItem;
        }
        // 时间
        clubPromotionLevelCacheItem.setTimestamp(CommTime.nowSecond());
        //最大id
        clubPromotionLevelCacheItem.setMaxId(clubPromotionLevelCacheItem.getMaxId() > 0L && clubPromotionLevelItem.getMaxId() <= 0L ? clubPromotionLevelCacheItem.getMaxId() : clubPromotionLevelItem.getMaxId());
        // 局数
        clubPromotionLevelCacheItem.setSetCount(clubPromotionLevelItem.getSetCount());
        // 赢数
        clubPromotionLevelCacheItem.setWinner(clubPromotionLevelItem.getWinner());
        // 报名费
        clubPromotionLevelCacheItem.setEntryFee(clubPromotionLevelItem.getEntryFee());
        // 消耗
        clubPromotionLevelCacheItem.setConsume(clubPromotionLevelItem.getConsume());
        // 消耗比赛分(理论报名费  房间消耗/房间人数)
        clubPromotionLevelCacheItem.setSportsPointConsume(clubPromotionLevelItem.getSportsPointConsume());
        // 推广员战绩分成
        clubPromotionLevelCacheItem.setPromotionShareValue(clubPromotionLevelItem.getPromotionShareValue());
        // 实际报名费
        clubPromotionLevelCacheItem.setActualEntryFee(clubPromotionLevelItem.getActualEntryFee());
        ContainerMgr.get().getRedis().putWithTime(cacheKey, CommTime.RemainingTime() + CommMath.randomInt(100, 500), new Gson().toJson(clubPromotionLevelCacheItem));
        return clubPromotionLevelCacheItem;
    }

}




