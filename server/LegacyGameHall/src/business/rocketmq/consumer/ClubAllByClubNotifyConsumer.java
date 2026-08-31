package business.rocketmq.consumer;

import business.global.shareclub.ShareClubMemberMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqClubMemberNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;
import jsproto.c2s.cclass.club.Club_define;

/**
 * @author : xushaojun
 * create at:  2020-08-20  14:00
 * @description: 亲友圈房间成员消息通知
 */
@Consumer(topic = MqTopic.CLUB_ALL_BY_CLUB_NOTIFY)
public class ClubAllByClubNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubMemberNotifyBo mqClubRoomResultBo = (MqClubMemberNotifyBo) body;
//        CommLog.info("亲友圈房间成员消息通知[{}]", JsonUtil.toJson(mqClubRoomResultBo));
//        if (ClubMgr.getInstance().getClubMemberMgr().checkClubMember(mqClubRoomResultBo.getClubID(), mqClubRoomResultBo.getPid(), mqClubRoomResultBo.getNotExistRoom(), mqClubRoomResultBo.getSignEnumClubID())) {
        CommLogD.info("亲友圈房间成员消息通知[{}]", JsonUtil.toJson(mqClubRoomResultBo));
        if (mqClubRoomResultBo.getClubID() != null) {
            ShareClubMemberMgr.getInstance().getAllOneClubMember(mqClubRoomResultBo.getClubID()).values().stream().filter(k -> mqClubRoomResultBo.getClubID() == k.getClubID() && k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value())).map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
                if (null != k) {
                    k.pushProto(mqClubRoomResultBo.getBaseSendMsgT());
                }
            });
//                ShareClubMemberMgr.getInstance().getAllOneClubMember(mqClubRoomResultBo.getClubID()).values().stream().filter(k -> mqClubRoomResultBo.getClubID() == k.getClubID() && k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value())).map(k -> PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID())).forEach(k -> {
//                    if (null != k && k.notExistRoom()) {
//                        k.pushProto(mqClubRoomResultBo.getBaseSendMsgT());
//                    }
//                });
        } else {
            //大厅接口玩家在线就是不在房间了
            Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(mqClubRoomResultBo.getPid());
            if (null != player) {
                player.pushProto(mqClubRoomResultBo.getBaseSendMsgT());
            }
        }
//        }
    }
}
