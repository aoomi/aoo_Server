package core.dispatch.event.player;

import BaseCommon.CommLog;
import business.global.room.RoomMgr;
import business.player.Player;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

import java.util.Objects;

/**
 * 登录
 *
 * @author Administrator
 */
@Data
public class LoginEvent implements BaseExecutor {
    /**
     * 玩家信息
     */
    private Player player;
    /**
     * 是否切到游戏
     */
    private boolean isGame;


    public LoginEvent(boolean isGame, Player player) {
        this.setPlayer(player);
        this.setGame(isGame);
    }

    @Override
    public void invoke() {
        if (Objects.isNull(getPlayer())) {
            CommLog.error("[LoginListener]([LoginEvent]) null == loginEvent || null == loginEvent.getPlayer() ");
            return;
        }
        if (isGame()) {
            this.gameInvoke();
        } else {
            this.normalInvoke();
        }
    }

    private void normalInvoke() {
        Player player = getPlayer();
        // 如果登录时间不是今天，并且不是游客，则插入记录。
        player.playerDataLog();
        // 检查活跃积分
        player.getExp().checkVipExp();
        // 更新最近一次登陆的时间
        player.setTodayFirstLogin(CommTime.isSameDayWithInTimeZone(player.getPlayerBO().getLastLogin(), CommTime.nowSecond()));
        // 设置最近登录时间
        player.getPlayerBO().setLastLogin(CommTime.nowSecond());
        // 保存最近登录时间
        player.getPlayerBO().saveLastLogin();
        // 更新掉线状态
        RoomMgr.getInstance().lostConnect(player, false);
    }

    private void gameInvoke() {
        CommLog.info("LoginEvent gameInvoke Pid:{}", this.getPlayer().getPid());
        // 更新掉线状态
        RoomMgr.getInstance().lostConnect(getPlayer(), false);
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