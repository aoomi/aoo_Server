package business.rocketmq.cdxzmj.consumer;							
							
import BaseCommon.CommLog;							
import business.global.sharegm.ShareNodeServerMgr;							
import business.cdxzmj.c2s.iclass.CCDXZMJ_CreateRoom;							
import business.rocketmq.bo.MqAbsRequestBo;							
import business.rocketmq.constant.MqTopic;							
import business.rocketmq.consumer.BaseCreateRoomConsumer;							
import cenum.PrizeType;							
import com.ddm.server.annotation.Consumer;							
import com.ddm.server.common.rocketmq.MqConsumerHandler;							
import com.google.gson.Gson;							
import core.server.cdxzmj.CDXZMJAPP;							
import jsproto.c2s.cclass.room.BaseRoomConfigure;							
							
/**							
 * @author : xushaojun							
 * create at:  2020-08-19  11:17							
 * @description: 模版麻将创建房间							
 */							
@Consumer(topic = MqTopic.BASE_CREATE_ROOM, id = CDXZMJAPP.gameTypeId)							
public class CDXZMJCreateRoomConsumer extends BaseCreateRoomConsumer implements MqConsumerHandler {							
							
							
    @Override							
    public void action(Object body) throws ClassNotFoundException {							
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;							
        //判断游戏和请求创建节点一致							
        if (mqAbsRequestBo.getGameTypeId() == CDXZMJAPP.GameType().getId() && ShareNodeServerMgr.getInstance().checkCurrentNode(mqAbsRequestBo.getShareNode().getIp(), mqAbsRequestBo.getShareNode().getPort())) {							
//            CommLog.info("创建房间[{}]", mqAbsRequestBo.getGameTypeName());							
            final CCDXZMJ_CreateRoom clientPack = new Gson().fromJson(mqAbsRequestBo.getBody(),							
                    CCDXZMJ_CreateRoom.class);							
            // 公共房间配置							
            BaseRoomConfigure<CCDXZMJ_CreateRoom> configure = new BaseRoomConfigure<>(							
                    PrizeType.RoomCard,							
                    CDXZMJAPP.GameType(),							
                    clientPack.clone());							
            super.action(body, CDXZMJAPP.GameType().getId(), configure);							
        }							
    }							
}							
