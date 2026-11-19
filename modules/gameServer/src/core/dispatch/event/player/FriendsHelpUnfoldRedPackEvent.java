package core.dispatch.event.player;

import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerFriendsHelpUnfoldRedPack;
import cenum.DispatcherComponentEnum;
import cenum.FriendsHelpUnfoldRedPackEnum.TargetType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkGame.PlayerRedPackTaskTimeBO;
import lombok.Data;

/**
 * 帮拆红包
 *
 * @author Administrator
 */
@Data
public class FriendsHelpUnfoldRedPackEvent implements BaseExecutor {
    // 目标类型
    private TargetType targetType;
    // 推荐人账号ID
    private long accountID;

    private long playerId;

    public FriendsHelpUnfoldRedPackEvent(long playerId, TargetType targetType, long accountID) {
        this.setPlayerId(playerId);
        this.setTargetType(targetType);
        this.setAccountID(accountID);
    }

    @Override
    public void invoke() {
        Player player = PlayerMgr.getInstance().getPlayerByAccountID(getAccountID());
        if (null == player) {
            CommLogD.error("null == player error  AccountID:{}", getAccountID());
            return;
        }

        // 被推荐人本身
        Player toPlayer = PlayerMgr.getInstance().getPlayer(getPlayerId());
        if (null == toPlayer) {
            CommLogD.error("null == toPlayer error  getPlayerId:{}", getPlayerId());

            return;
        }

        // 获取玩家帮拆红包活动
        PlayerFriendsHelpUnfoldRedPack friendsHelpUnfoldRedPack = player.getFeature(PlayerFriendsHelpUnfoldRedPack.class);
        // 获取玩家的任务时间
        PlayerRedPackTaskTimeBO taskTimeBO = friendsHelpUnfoldRedPack.getRedPackTaskTimeBO();
        if (null == taskTimeBO) {
//            CommLogD.error("HelpToUnpackRedEnvelopesListener null == taskTimeBO ");
            return;
        }
        if (TargetType.OpenRedPack.equals(getTargetType())) {
            // 检查当前玩家的注册时间是否在推荐玩家的活动时间内。
            if (CommTime.checkTimeIntervale(taskTimeBO.getStartTime(), taskTimeBO.getEndTime(), (int) (toPlayer.getPlayerBO().getCreateTime() / 1000))) {
                // 帮推荐玩家拆红包。
                player.getFeature(PlayerFriendsHelpUnfoldRedPack.class).exeTask(TargetType.HelpToUnpackRedEnvelopes.ordinal(), getPlayerId());
            }
        } else if (TargetType.FriendHuPai.equals(getTargetType())) {
            // 好友胡牌
            player.getFeature(PlayerFriendsHelpUnfoldRedPack.class).exeTask(TargetType.FriendHuPai.ordinal(), getPlayerId());
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