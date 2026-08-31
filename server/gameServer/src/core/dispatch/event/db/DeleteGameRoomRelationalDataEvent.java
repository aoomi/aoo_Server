package core.dispatch.event.db;

import BaseCommon.CommLog;
import business.global.room.RoomRecordMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.other.Restrictions;
import core.db.service.clarkGame.GameRoomBOService;
import core.db.service.clarkGame.GameSetBOService;
import core.db.service.clarkGame.PlayerRoomAloneBOService;
import core.ioc.ContainerMgr;
import lombok.Data;

/**
 * 删除游戏房间相关过期数据
 */
@Data
public class DeleteGameRoomRelationalDataEvent implements BaseExecutor {
    /**
     * 删除的房间Id值
     */
    private long deleteRoomId;

    public DeleteGameRoomRelationalDataEvent(long deleteRoomId) {
        this.deleteRoomId = deleteRoomId;
    }

    @Override
    public void invoke() {
        // 查询15天前最大的roomId
        // 删除小于15天前最大的roomId的数据一次2000条
        int deleteGameRoom = 0;
        int deleteGameSet = 0;
        int deletePlayerRoomAlone = 0;
        if (this.deleteRoomId <= 0L) {
            return;
        }
        CommLog.info("DeleteGameRoomRelationalDataEvent start");
        if (RoomRecordMgr.getInstance().notContainsKey(GameRoomBOService.class.getSimpleName())) {
            deleteGameRoom = ContainerMgr.get().getComponent(GameRoomBOService.class).delete(Restrictions.le("id", this.getDeleteRoomId()).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            RoomRecordMgr.getInstance().addKeySet(GameRoomBOService.class.getSimpleName(), deleteGameRoom);
        }
        if (RoomRecordMgr.getInstance().notContainsKey(GameSetBOService.class.getSimpleName())) {
            deleteGameSet = ContainerMgr.get().getComponent(GameSetBOService.class).delete(Restrictions.le("roomID", this.getDeleteRoomId()).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            RoomRecordMgr.getInstance().addKeySet(GameSetBOService.class.getSimpleName(), deleteGameSet);
        }
        if (RoomRecordMgr.getInstance().notContainsKey(PlayerRoomAloneBOService.class.getSimpleName())) {
            deletePlayerRoomAlone = ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class).delete(Restrictions.le("roomID", this.getDeleteRoomId()).setLimit(RoomRecordMgr.DELETE_DB_DATE_LIMIT));
            RoomRecordMgr.getInstance().addKeySet(PlayerRoomAloneBOService.class.getSimpleName(), deletePlayerRoomAlone);
        }
        if (deleteGameRoom > 0 || deleteGameSet > 0 || deletePlayerRoomAlone > 0) {
            CommLog.info("DeleteGameRoomRelationalDataEvent end deleteGameRoom:{},deleteGameSet:{},deletePlayerRoomAlone:{}", deleteGameRoom, deleteGameSet, deletePlayerRoomAlone);
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
