package business.rocketmq.consumer;

import business.global.club.ClubMgr;
import business.rocketmq.bo.MqClubMemberUpdateNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

/**
 * @author : xushaojun
 * create at:  2020-9-2  14:00
 * @description: 亲友圈成员数据更新
 */
@Consumer(topic = MqTopic.CLUB_UPDATE_MEMBER_BO_NOTIFY)
public class UpdateClubMemberBONotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubMemberUpdateNotifyBo bo = (MqClubMemberUpdateNotifyBo) body;
        //不是同一个节点推送更新，这个一定要注释掉不然本地更新不了
//        if (!bo.getNodeName().equals(Config.nodeName())) {
        ClubMgr.getInstance().getClubMemberMgr().onUpdateMemberStatusShare(bo.getClubMemberBoId());
        CommLogD.info("clubmember数据更新[{}]", JsonUtil.toJson(bo));
//        }

    }
}
