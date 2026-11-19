package core.dispatcher;


import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.mq.factory.InnerMessageCodecFactory;
import com.ddm.server.websocket.def.MessageType;
import io.netty.buffer.ByteBuf;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 消费的的订阅分类
 */
@Data
public class RegNetPack {

    private static class SingletonHolder {
        static final RegNetPack instance = new RegNetPack();
    }

    private RegNetPack() {
    }

    public static RegNetPack getInstance() {
        return SingletonHolder.instance;
    }


    /**
     * 发送请求包
     *
     * @param topic
     * @param event
     * @param baseSendMsg
     */
    public void regNetPack(String topic, String event, BaseSendMsg baseSendMsg) {
        ByteBuf packByteBuf = InnerMessageCodecFactory.getInstance().registryToServerEncode(MessageType.Request, event.toLowerCase(), GsonUtils.toJsonString(baseSendMsg));
        byte[] packByte = new byte[packByteBuf.readableBytes()];
        packByteBuf.readBytes(packByte);
        MqProducerMgr.get().sendGateway(topic, packByte);
    }


}



