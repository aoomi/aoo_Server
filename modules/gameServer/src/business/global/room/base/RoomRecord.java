package business.global.room.base;

import cenum.PrizeType;
import core.logger.flow.FlowLogger;
import com.ddm.server.common.redis.RedisUtil;

/**
 * 房间记录
 *
 * @author Huaxing
 */
public class RoomRecord {
    // 房间信息
    public AbsBaseRoom room;

    public RoomRecord(AbsBaseRoom room) {
        this.room = room;
        this.initSetRecord();
        this.clear();
    }

    /**
     * 清空
     */
    private void clear() {
        this.room = null;
    }

    private void initSetRecord() {
        // 检查如果是金币场则不记录
        if (PrizeType.Gold.equals(this.room.getBaseRoomConfigure().getPrizeType())) {
            return;
        }
        // 添加房卡消耗日志
        this.addPlayerRoomLog();
        //红包活动
        this.room.checkHaveHongBao();
    }

    /**
     * 添加房卡消耗日志
     */
    public void addPlayerRoomLog() {
        String businessId = "aoo:record:room:" + this.room.getRoomID() + ":set:" + this.room.getCurSetID();
        RedisUtil.AtomicResult guard=RedisUtil.setNxExResult(businessId, "RECORDED", 90 * 24 * 60 * 60);
        if (guard==RedisUtil.AtomicResult.UNAVAILABLE||guard==RedisUtil.AtomicResult.UNKNOWN_APPLIED) throw new IllegalStateException("room record idempotency outcome unknown");
        if (guard==RedisUtil.AtomicResult.ALREADY_EXISTS) {
            return;
        }
        FlowLogger.playerRoomLog(String.valueOf(this.room.getGameRoomBO().getDateTime()), this.room.getOwnerID(), this.room.getRoomID(), this.room.getCurSetID(),
                this.room.getGameRoomBO().getPlayerList(), this.room.getCount(), this.room.getBaseRoomConfigure().getBaseCreateRoom().getClubId(),
                this.room.getRoomKey(), this.room.getConsumeValue(), this.room.getGameRoomBO().getCreateTime(),
                this.room.getValueType().value(), this.room.getBaseRoomConfigure().getGameType().getId(), 0, this.room.getBaseRoomConfigure().getBaseCreateRoom().getUnionId());
    }
}
