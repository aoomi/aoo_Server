package business.rocketmq.consumer;

import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqDataResult;
import business.rocketmq.bo.MqResponseBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

/**
 * @author : xushaojun
 * create at:  2020-09-04  16:51
 * @description: 进入子游戏返回大厅消息
 */
@Consumer(topic = MqTopic.ALL_HALL_BACK)
public class AllHallBackConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqRoomResultBo mqRoomResultBo = (MqRoomResultBo) body;
        Player player = PlayerMgr.getInstance().getPlayer(mqRoomResultBo.getMqAbsRequestBo().getPid());
        if (player != null) {
            CommLogD.info("子游戏返回[{}][{}]", mqRoomResultBo.getMqAbsRequestBo().getGameTypeName(), JsonUtil.toJson(mqRoomResultBo));
            MqResponseBo mqResponseBo = new MqResponseBo();
            mqResponseBo.setOpcode(mqRoomResultBo.getMqAbsRequestBo().getOpcode());
            mqResponseBo.setResult(new MqDataResult(mqRoomResultBo.getsData_result().getCode().value(), mqRoomResultBo.getsData_result().getData(), mqRoomResultBo.getsData_result().getMsg(), mqRoomResultBo.getsData_result().getCustom()));
            player.pushProto(mqResponseBo);
        }
    }
}
