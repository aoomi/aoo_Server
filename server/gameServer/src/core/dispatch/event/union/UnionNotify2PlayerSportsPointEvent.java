package core.dispatch.event.union;

import business.rocketmq.bo.MqClubMemberNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 赛事通知玩家竞技点更新
 */
@Data
public class UnionNotify2PlayerSportsPointEvent implements BaseExecutor {
    /**
     * 通知信息
     */
    private BaseSendMsg baseSendMsg;
    /**
     * 玩家信息
     */
    private SharePlayer sharePlayer = null;
    private long playerId;
    private long clubId;

    public UnionNotify2PlayerSportsPointEvent(SharePlayer sharePlayer, long clubId, BaseSendMsg baseSendMsg) {
        this.setSharePlayer(sharePlayer);
        if (sharePlayer != null) {
            this.setPlayerId(sharePlayer.getPlayerBO().getId());
        }
        this.setBaseSendMsg(baseSendMsg);
        this.setClubId(clubId);
    }

    public UnionNotify2PlayerSportsPointEvent(SharePlayer sharePlayer, BaseSendMsg baseSendMsg) {
        this.setSharePlayer(sharePlayer);
        this.setBaseSendMsg(baseSendMsg);
    }

    public UnionNotify2PlayerSportsPointEvent(long playerId, long clubId, BaseSendMsg baseSendMsg) {
        this.setPlayerId(playerId);
        this.setBaseSendMsg(baseSendMsg);
        this.setClubId(clubId);
    }


    @Override
    public void invoke() {
//        Player player = null == getPlayer() ? PlayerMgr.getInstance().getOnlinePlayerByPid(getPlayerId()) : getPlayer();
//        if(Config.isShare()){
        MqClubMemberNotifyBo mqClubMemberNotifyBo = new MqClubMemberNotifyBo();
        mqClubMemberNotifyBo.setPid(getPlayerId());
        mqClubMemberNotifyBo.setNotExistRoom(true);
        mqClubMemberNotifyBo.setBaseSendMsgClassType(getBaseSendMsg().getClass().getName());
        mqClubMemberNotifyBo.setBaseSendMsg(getBaseSendMsg());
        mqClubMemberNotifyBo.setClubID(this.getClubId());
        MqProducerMgr.get().send(MqTopic.CLUB_ALL_BY_CLUB_NOTIFY, mqClubMemberNotifyBo);
//        } else {
//            if (null != player && player.notExistRoom()) {
//                player.pushProto(getBaseSendMsg());
//            }
//        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.CLUB_UNION.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.CLUB_UNION.bufferSize();
    }
}
