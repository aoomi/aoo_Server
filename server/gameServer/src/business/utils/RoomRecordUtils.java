package business.utils;

import cenum.room.RoomState;
import com.ddm.server.common.utils.CommTime;

public class RoomRecordUtils {

    /**
     * 获取房间状态
     *
     * @param gameEndTime 游戏结束时间
     * @param roomState
     * @return
     */
    public static int getRoomState(int gameEndTime, int roomState) {
        if (RoomState.End.value() == roomState) {
            // 已经是结束状态
            return roomState;
        }
        if (gameEndTime <= 0) {
            // 游戏都没开始
            return RoomState.End.value();
        }
        return CommTime.hourTimeDifference(gameEndTime * 1000L, CommTime.nowMS()) >= 2 ? RoomState.End.value() : roomState;
    }
}
