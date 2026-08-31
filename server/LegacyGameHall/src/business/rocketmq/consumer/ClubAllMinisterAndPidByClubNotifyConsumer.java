package business.rocketmq.consumer;

import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqClubMemberNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;
import jsproto.c2s.cclass.club.Club_define;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author : xushaojun
 * create at:  2021-04-01  14:00
 * @description: 亲友圈房间成员消息通知
 */
@Consumer(topic = MqTopic.CLUB_ALL_MINISTER_AND_PID_BY_CLUB_NOTIFY)
public class ClubAllMinisterAndPidByClubNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubMemberNotifyBo mqClubRoomResultBo = (MqClubMemberNotifyBo) body;
        CommLogD.info("亲友圈管理员和玩家消息通知[{}]", JsonUtil.toJson(mqClubRoomResultBo));
        ShareClubMemberMgr.getInstance().getAllOneClubMember(mqClubRoomResultBo.getClubID()).values().stream()
                .filter(k -> mqClubRoomResultBo.getClubID() == k.getClubID()
                        && ((k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                        || (mqClubRoomResultBo.getPid() != null && k.getClubMemberBO().getPlayerID() == mqClubRoomResultBo.getPid()))
                        || (!CollectionUtils.isEmpty(mqClubRoomResultBo.getPidList()) && mqClubRoomResultBo.getPidList().contains(k.getClubMemberBO().getPlayerID())))
                .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                    if (null != k) {
                        k.pushProto(mqClubRoomResultBo.getBaseSendMsgT());
                    }
                });
    }
}
