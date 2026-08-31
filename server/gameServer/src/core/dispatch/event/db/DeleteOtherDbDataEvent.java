package core.dispatch.event.db;

import BaseCommon.CommLog;
import business.global.room.RoomRecordMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerGameRoomIdBOService;
import core.db.service.clarkGame.UnionDynamicBOService;
import core.db.service.clarkLog.PlayerRoomLogFlowService;
import core.db.service.clarkLog.RoomConfigPrizePoolLogFlowService;
import core.db.service.clarkLog.UnionSportsPointProfitLogFlowService;
import core.ioc.ContainerMgr;
import lombok.Data;

/**
 * 删除其他过期数据
 */
@Data
public class DeleteOtherDbDataEvent implements BaseExecutor {

    @Override
    public void invoke() {
        int deleteUnionDynamic = 0;
        int deletePlayerGameRoomId = 0;
        int deleteRoomConfigPrizePoolLogId = 0;
        int deletePlayerRoomLogId = 0;
        int deleteUnionSportsPointProfitLogId = 0;
        CommLog.info("DeleteOtherDbDataEvent start");
        if (RoomRecordMgr.getInstance().notContainsKey(UnionDynamicBOService.class.getSimpleName())) {
            // 删除超过一个月的赛事、亲友圈动态数据
            long id = RoomRecordMgr.getInstance().getId(UnionDynamicBOService.class.getSimpleName());
            if (id > 0L) {
                deleteUnionDynamic = ContainerMgr.get().getComponent(UnionDynamicBOService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(UnionDynamicBOService.class.getSimpleName(), deleteUnionDynamic);
        }

        if (RoomRecordMgr.getInstance().notContainsKey(PlayerGameRoomIdBOService.class.getSimpleName())) {
            // 删除超过3天的已查看数据
            long id = RoomRecordMgr.getInstance().getId(PlayerGameRoomIdBOService.class.getSimpleName());
            if (id > 0L) {
                deletePlayerGameRoomId = ContainerMgr.get().getComponent(PlayerGameRoomIdBOService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(PlayerGameRoomIdBOService.class.getSimpleName(), deletePlayerGameRoomId);
        }

        if (RoomRecordMgr.getInstance().notContainsKey(RoomConfigPrizePoolLogFlowService.class.getSimpleName())) {
            long id = RoomRecordMgr.getInstance().getId(RoomConfigPrizePoolLogFlowService.class.getSimpleName());
            if (id > 0L) {
                deleteRoomConfigPrizePoolLogId = ContainerMgr.get().getComponent(RoomConfigPrizePoolLogFlowService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(RoomConfigPrizePoolLogFlowService.class.getSimpleName(), deleteRoomConfigPrizePoolLogId);
        }

        if (RoomRecordMgr.getInstance().notContainsKey(PlayerRoomLogFlowService.class.getSimpleName())) {
            long id = RoomRecordMgr.getInstance().getId(PlayerRoomLogFlowService.class.getSimpleName());
            if (id > 0L) {
                deletePlayerRoomLogId = ContainerMgr.get().getComponent(PlayerRoomLogFlowService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(PlayerRoomLogFlowService.class.getSimpleName(), deletePlayerRoomLogId);
        }

        if (RoomRecordMgr.getInstance().notContainsKey(UnionSportsPointProfitLogFlowService.class.getSimpleName())) {
            long id = RoomRecordMgr.getInstance().getId(UnionSportsPointProfitLogFlowService.class.getSimpleName());
            if (id > 0L) {
                deleteUnionSportsPointProfitLogId = ContainerMgr.get().getComponent(UnionSportsPointProfitLogFlowService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(UnionSportsPointProfitLogFlowService.class.getSimpleName(), deleteUnionSportsPointProfitLogId);
        }
        if (deletePlayerGameRoomId > 0 || deleteUnionDynamic > 0 || deleteRoomConfigPrizePoolLogId > 0 || deletePlayerRoomLogId > 0 || deleteUnionSportsPointProfitLogId > 0) {
            CommLog.info("DeleteOtherDbDataEvent end deleteUnionDynamic:{},deletePlayerGameRoomId:{},deleteRoomConfigPrizePoolLogId:{},deletePlayerRoomLogId:{},deleteUnionSportsPointProfitLogId:{}", deleteUnionDynamic, deletePlayerGameRoomId, deleteRoomConfigPrizePoolLogId, deletePlayerRoomLogId, deleteUnionSportsPointProfitLogId);
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.DB_DELETE.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.DB_DELETE.bufferSize();
    }
}
