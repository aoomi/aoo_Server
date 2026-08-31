package business.rocketmq.consumer;

import business.global.union.UnionMgr;
import business.rocketmq.bo.MqUnionMemberNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.union.UnionNotify2AllByClubEvent;

/**
 * @author : xushaojun
 * create at:  2020-08-21  14:00
 * @description: 赛事房间成员消息通知
 */
@Consumer(topic = MqTopic.UNION_ALL_BY_UNION_NOTIFY)
public class UnionAllByClubNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUnionMemberNotifyBo mqUnionMemberNotifyBo = (MqUnionMemberNotifyBo) body;
        CommLogD.info("赛事房间成员消息通知[{}]", JsonUtil.toJson(mqUnionMemberNotifyBo));
        DispatcherComponent.getInstance().publish(new UnionNotify2AllByClubEvent(UnionMgr.getInstance().getUnionMemberMgr().getUnionToClubIdList(mqUnionMemberNotifyBo.getUnionId()), mqUnionMemberNotifyBo.getUnionGameCfgId(), mqUnionMemberNotifyBo.getBaseSendMsgT()));
    }
}
