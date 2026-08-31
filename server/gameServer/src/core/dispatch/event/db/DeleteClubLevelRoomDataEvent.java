package core.dispatch.event.db;

import BaseCommon.CommLog;
import business.global.room.RoomRecordMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.other.Restrictions;
import core.db.service.clarkLog.ClubLevelRoomCountLogFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogFlowService;
import core.db.service.clarkLog.RoomPromotionPointCountLogFlowService;
import core.ioc.ContainerMgr;
import lombok.Data;

/**
 * 删除推广员过期数据
 */
@Data
public class DeleteClubLevelRoomDataEvent implements BaseExecutor {

    @Override
    public void invoke() {
        CommLog.info("DeleteClubLevelRoomDataEvent start");
        int deleteClubLevelRoom = 0;
        int deleteClubLevelRoomCount = 0;
        int deleteRoomPromotionPointCountLog = 0;
        if (RoomRecordMgr.getInstance().notContainsKey(ClubLevelRoomLogFlowService.class.getSimpleName())) {
            // 删除超过3天的数据
            long id = RoomRecordMgr.getInstance().getId(ClubLevelRoomLogFlowService.class.getSimpleName());
            if (id > 0L) {
                deleteClubLevelRoom = ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(ClubLevelRoomLogFlowService.class.getSimpleName(), deleteClubLevelRoom);
        }
        if (RoomRecordMgr.getInstance().notContainsKey(ClubLevelRoomCountLogFlowService.class.getSimpleName())) {
            // 删除超过15天的数据
            long id = RoomRecordMgr.getInstance().getId(ClubLevelRoomCountLogFlowService.class.getSimpleName());
            if (id > 0L) {
                deleteClubLevelRoomCount = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(ClubLevelRoomCountLogFlowService.class.getSimpleName(), deleteClubLevelRoomCount);
        }
        if (RoomRecordMgr.getInstance().notContainsKey(RoomPromotionPointCountLogFlowService.class.getSimpleName())) {
            // 删除超过15天的数据
            long id = RoomRecordMgr.getInstance().getId(RoomPromotionPointCountLogFlowService.class.getSimpleName());
            if (id > 0L) {
                deleteRoomPromotionPointCountLog = ContainerMgr.get().getComponent(RoomPromotionPointCountLogFlowService.class).delete(Restrictions.le("id", id).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            }
            RoomRecordMgr.getInstance().addKeySet(RoomPromotionPointCountLogFlowService.class.getSimpleName(), deleteRoomPromotionPointCountLog);
        }
        if (deleteClubLevelRoom > 0 || deleteClubLevelRoomCount > 0 || deleteRoomPromotionPointCountLog > 0) {
            CommLog.info("DeleteClubLevelRoomDataEvent end deleteClubLevelRoom:{},deleteClubLevelRoomCount:{},deleteRoomPromotionPointCountLog:{}", deleteClubLevelRoom, deleteClubLevelRoomCount, deleteRoomPromotionPointCountLog);
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
