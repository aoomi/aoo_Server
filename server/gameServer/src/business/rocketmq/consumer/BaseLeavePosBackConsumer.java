package business.rocketmq.consumer;

import business.global.sharegm.ShareNodeServerMgr;
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
 * create at:  2020-08-12  16:51
 * @description: 进入房间返回
 */
@Consumer(topic = MqTopic.BASE_LEAVE_POS_BACK)
public class BaseLeavePosBackConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqRoomResultBo mqRoomResultBo = (MqRoomResultBo) body;
        Player player = PlayerMgr.getInstance().getPlayer(mqRoomResultBo.getMqAbsRequestBo().getPid());
        if (player != null && ShareNodeServerMgr.getInstance().checkCurrentNode(mqRoomResultBo.getMqAbsRequestBo().getShareNodeFrom().getIp(), mqRoomResultBo.getMqAbsRequestBo().getShareNodeFrom().getPort())) {
            CommLogD.info("进入房间返回[{}][{}]", mqRoomResultBo.getMqAbsRequestBo().getGameTypeName(), JsonUtil.toJson(mqRoomResultBo));
            MqResponseBo mqResponseBo = new MqResponseBo();
            mqResponseBo.setOpcode(mqRoomResultBo.getMqAbsRequestBo().getOpcode());
            mqResponseBo.setResult(new MqDataResult(mqRoomResultBo.getsData_result().getCode().value(), mqRoomResultBo.getsData_result().getData(), mqRoomResultBo.getsData_result().getMsg(), mqRoomResultBo.getsData_result().getCustom()));
            player.pushProto(mqResponseBo);
            CommLogD.info("进入房间返回结束[{}],请求标识[{}]", mqRoomResultBo.getMqAbsRequestBo().getGameTypeName(), mqRoomResultBo.getMqAbsRequestBo().getRequestId());
        }
    }
}
