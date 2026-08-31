package business.rocketmq.consumer;

import business.rocketmq.bo.MqClubMemberBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.localcache.LocalClubMemberEvent;

/**
 * @author : xushaojun
 * create at:  2021-08-09  16:51
 * @description: 删除亲友圈成员
 */
@Consumer(topic = MqTopic.LOCAL_CLUB_MEMBER_REMOVE)
public class LocalClubMemberRemoveConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) {
        MqClubMemberBo bo = (MqClubMemberBo) body;
//        CommLogD.info("删除亲友圈成员{}", bo.getClubMember().getClubMemberBO().getId());
        DispatcherComponent.getInstance().publish(new LocalClubMemberEvent(bo.getClubMember(), false));
    }
}
