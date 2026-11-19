package core.dispatch.event.player;

import BaseCommon.CommLog;
import business.global.room.RoomMgr;
import business.player.Player;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.logger.flow.FlowLogger;
import lombok.Data;

/**
 * 登出
 *
 * @author Administrator
 */
@Data
public class LogoutEvent implements BaseExecutor {
    private Player player;

    public LogoutEvent(long playerId, Player player) {
        this.player = player;
    }

    @Override
    public void invoke() {
        if (null == getPlayer()) {
            CommLog.error("[LogoutListener]([LogoutEvent]) null == logoutEvent || null == logoutEvent.getPlayer() ");
            return;
        }
        Player player = this.getPlayer();
        // 更新掉线状态
        RoomMgr.getInstance().lostConnect(player, true);
        int lastLogout = CommTime.nowSecond();
        if (CommTime.minTimeDifference(player.getPlayerBO().getLastLogin(), lastLogout) >= 5) {
            FlowLogger.playerLoginLog(player.getPid(), player.getPlayerBO().getLastLogin(), lastLogout, player.getGameList(), player.getIp(),
                    String.valueOf(player.getLocationInfo().getLatitude()), String.valueOf(player.getLocationInfo().getLongitude()),
                    player.getLocationInfo().getAddress());
        }

    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PLAYER.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PLAYER.bufferSize();
    }
}