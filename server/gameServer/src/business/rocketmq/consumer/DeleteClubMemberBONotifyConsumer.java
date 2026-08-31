package business.rocketmq.consumer;

import business.global.club.ClubMgr;
import business.rocketmq.bo.MqClubMemberUpdateNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2021-3-29  14:00
 * @description: 亲友圈成员数据删除
 */
@Consumer(topic = MqTopic.CLUB_DELETE_MEMBER_BO_NOTIFY)
public class DeleteClubMemberBONotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubMemberUpdateNotifyBo bo = (MqClubMemberUpdateNotifyBo) body;
        //不是同一个节点推送更新
        if (!bo.getNodeName().equals(Config.nodeName())) {
            ClubMgr.getInstance().getClubMemberMgr().onDeleteMemberLocal(bo.getClubMemberBoId());
            CommLogD.info("删除clubmemberId[{}]", bo.getClubMemberBoId());
        }

    }
}
