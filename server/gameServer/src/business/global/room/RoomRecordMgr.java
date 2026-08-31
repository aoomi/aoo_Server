package business.global.room;

import BaseThread.BaseMutexManager;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomRecord;
import business.utils.TimeConditionUtils;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import com.google.common.collect.Sets;
import core.db.other.Restrictions;
import core.db.service.clarkGame.GameRoomBOService;
import core.db.service.clarkGame.PlayerGameRoomIdBOService;
import core.db.service.clarkGame.UnionDynamicBOService;
import core.db.service.clarkLog.*;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.db.DeleteClubLevelRoomDataEvent;
import core.dispatch.event.db.DeleteGameRoomRelationalDataEvent;
import core.dispatch.event.db.DeleteOtherDbDataEvent;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.QueryIdItem;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 房间记录器
 *
 * @auther zaf
 */
@Data
public class RoomRecordMgr {
    /**
     * 单列
     */
    private static RoomRecordMgr instance = new RoomRecordMgr();

    private final BaseMutexManager _lock = new BaseMutexManager();

    public static RoomRecordMgr getInstance() {
        return instance;
    }

    public void init() {

    }

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    public void add(AbsBaseRoom room) {
        new RoomRecord(room);
    }

    /**
     * 删除的房间Id值
     */
    private long deleteRoomId = 0L;

    /**
     * 要删除数据表的Id值
     */
    public Map<String, Long> deleteTableIdMap = Maps.newConcurrentMap();

    /**
     * 对比时间
     */
    private String dateTime;

    /**
     * 删除字段数量
     */
    private Set<String> keySet = Sets.newHashSet();


    /**
     * 移除删除游戏房间
     */
    public void asyncDelGameRoom() {
        // 检查时间
        this.checkDateTime();
        if (this.getId(GameRoomBOService.class.getSimpleName()) > 0L) {
            // 删除游戏房间相关过期数据
            DispatcherComponent.getInstance().publish(new DeleteGameRoomRelationalDataEvent(this.getId(GameRoomBOService.class.getSimpleName())));
        }
        // 删除推广员过期数据
        DispatcherComponent.getInstance().publish(new DeleteClubLevelRoomDataEvent());
        // 删除其他过期数据
        DispatcherComponent.getInstance().publish(new DeleteOtherDbDataEvent());
    }

    /**
     * 检查当前日期是否一致
     */
    private void checkDateTime() {
        String dateTimeTemp = CommTime.getNowTimeStringYMD();
        if (!dateTimeTemp.equals(this.dateTime)) {
            // 清空重置掉
            this.keySet.clear();
            this.dateTime = dateTimeTemp;
            this.QuerydeleteRoomId();
        }
    }

    /**
     * 删除数据库数据限制的值
     */
    public final static int DELETE_DB_DATE_LIMIT = 2500;

    /**
     * 包含这个key
     *
     * @param key key
     * @return
     */
    public boolean notContainsKey(String key) {
        return !this.keySet.contains(key);
    }

    public void addKeySet(String key, int deleteNumber) {
        if (deleteNumber < DELETE_DB_DATE_LIMIT) {
            this.keySet.add(key);
        }
    }

    public void put(String tableName, long id) {
        this.deleteTableIdMap.put(tableName, id);
    }

    /**
     * 获取Id
     *
     * @param tableName 表Id
     * @return
     */
    public long getId(String tableName) {
        return this.getDeleteTableIdMap().containsKey(tableName) ? this.getDeleteTableIdMap().get(tableName) : 0L;
    }

    /**
     * 查询要删除房间Id
     */
    private final void QuerydeleteRoomId() {
        // 游戏房间表
        QueryIdItem queryIdItem = ContainerMgr.get().getComponent(GameRoomBOService.class).findOneE(Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(15)), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(GameRoomBOService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 实时表
        queryIdItem = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).findOneE(TimeConditionUtils.DayZeroClockSLT("date_time", TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_7.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(ClubLevelRoomLogFlowService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 统计表
        queryIdItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).findOneE(TimeConditionUtils.DayZeroClockSLT("date_time", TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_15.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(ClubLevelRoomCountLogFlowService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 动态表
        queryIdItem = ContainerMgr.get().getComponent(UnionDynamicBOService.class).findOneE(TimeConditionUtils.DayZeroClockSLT(TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_MONTH.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(UnionDynamicBOService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 玩家游戏房间Id表
        queryIdItem = ContainerMgr.get().getComponent(PlayerGameRoomIdBOService.class).findOneE(TimeConditionUtils.DayZeroClockSLT(TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_LAST_THREE_DAYS.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(PlayerGameRoomIdBOService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 房间配置奖金池表
        queryIdItem = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).findOneE(TimeConditionUtils.DayZeroClockSLT("date_time", TimeConditionUtils.Record_Get_Type.RECORD_GET_DAY_7.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(RoomConfigPrizePoolLogFlowService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 玩家房间记录表
        queryIdItem = ContainerMgr.get().getComponent(PlayerRoomLogFlowService.class).findOneE(TimeConditionUtils.DayZeroClockSLT("date_time", TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_MONTH.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(PlayerRoomLogFlowService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
        // 赛事收益表
        queryIdItem = ContainerMgr.get().getComponent(UnionSportsPointProfitLogFlowService.class).findOneE(TimeConditionUtils.DayZeroClockSLT("date_time", TimeConditionUtils.Record_Get_Type.RECORD_GET_TYPE_MONTH.value()), QueryIdItem.class, QueryIdItem.getItemsName());
        this.put(UnionSportsPointProfitLogFlowService.class.getSimpleName(), Objects.nonNull(queryIdItem) && queryIdItem.getId() > 0L ? queryIdItem.getId() : 0L);
    }
}
