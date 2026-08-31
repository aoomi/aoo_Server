package business.rocketmq.consumer;

import business.global.shareclub.ShareClubMemberMgr;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqClubMemberNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;
import jsproto.c2s.cclass.club.Club_define;

import java.util.Objects;

/**
 * @author : xushaojun
 * create at:  2020-08-20  14:00
 * @description: 亲友圈房间成员消息通知
 */
@Consumer(topic = MqTopic.CLUB_ALL_BY_CLUB_MINISTER_NOTIFY)
public class ClubAllByClubMinsterNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubMemberNotifyBo mqClubRoomResultBo = (MqClubMemberNotifyBo) body;
        CommLogD.info("亲友圈管理员消息通知[{}]", JsonUtil.toJson(mqClubRoomResultBo));
        if (mqClubRoomResultBo.getClubID() == null) return;
        ShareClubMemberMgr.getInstance().getAllOneClubMember(mqClubRoomResultBo.getClubID()).values().stream()
                // 筛选亲友圈ID和成员状态
                .filter(k -> mqClubRoomResultBo.getClubID() == k.getClubID() && k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()) && k.isMinister())
                .map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                    if (Objects.nonNull(k)) {
                        k.pushProto(mqClubRoomResultBo.getBaseSendMsgT());
                    }
                });
    }
}
