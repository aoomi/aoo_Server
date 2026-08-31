package business.rocketmq.consumer;

import business.global.club.ClubMgr;
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
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.SUnion_ClubChange;

import java.util.Objects;

/**
 * @author : xushaojun
 * create at:  2021-04-01  14:00
 * @description: 赛事成员消息通知
 */
@Consumer(topic = MqTopic.UNION_CLUB_ALL_MEMBER_NOTIFY)
public class UnionNotify2ClubAllMemberConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubMemberNotifyBo mqClubRoomResultBo = (MqClubMemberNotifyBo) body;
        CommLogD.info("赛事成员消息通知[{}]", JsonUtil.toJson(mqClubRoomResultBo));
        SUnion_ClubChange sUnionClubChange = (SUnion_ClubChange) mqClubRoomResultBo.getBaseSendMsgT();
        ShareClubMemberMgr.getInstance().getAllOneClubMember(mqClubRoomResultBo.getClubID())
                .values()
                .stream()
                .filter(k -> mqClubRoomResultBo.getClubID() == k.getClubID() && k.getStatus(Club_define.Club_Player_Status.PLAYER_JIARU.value()))
                .forEach(k -> {
                    Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(k.getClubMemberBO().getPlayerID());
                    if (Objects.nonNull(player)) {
                        if (sUnionClubChange.getUnionId() <= 0L) {
                            sUnionClubChange.setSportsPoint(0D);
                            sUnionClubChange.setUnionPostType(UnionDefine.UNION_POST_TYPE.UNION_GENERAL.value());
                        } else {
//                            sUnionClubChange.setSportsPoint(k.getClubMemberBO().getSportsPoint());
                            sUnionClubChange.setSportsPoint(ShareClubMemberMgr.getInstance().getClubMember(k.getId()).getClubMemberBO().getSportsPoint());
                            if (mqClubRoomResultBo.getPid() == player.getPlayerBO().getId()) {
                                sUnionClubChange.setUnionPostType(UnionDefine.UNION_POST_TYPE.UNION_CREATE.value());
                            } else {
                                sUnionClubChange.setUnionPostType(UnionDefine.UNION_POST_TYPE.UNION_GENERAL.value());
                            }
                        }
                        player.pushProto(sUnionClubChange);
                    }

                });
    }
}
