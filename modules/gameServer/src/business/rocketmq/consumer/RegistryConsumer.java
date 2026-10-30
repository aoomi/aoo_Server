package business.rocketmq.consumer;


import BaseCommon.CommLog;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.mq.factory.InnerMessageCodecFactory;
import com.ddm.server.websocket.message.ServerToServerHead;
import core.dispatcher.HandlerServerDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;


/**
 * 注册中心通知
 */
@Data
@Consumer(topic = Config.REGISTRY_NOTIFY)
public class RegistryConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) throws ClassNotFoundException {
        byte[] req = (byte[]) body;
        if (req.length <= 0) {
            CommLog.error("RegistryConsumer body length <= 0");
            return;
        }
        ByteBuf byteBuf = Unpooled.wrappedBuffer(req);
        ServerToServerHead registryToServerHead = InnerMessageCodecFactory.getInstance().readRegistryToServerHead(byteBuf);
        if (Config.ServerID() != registryToServerHead.getServerId()) {
            HandlerServerDispatcher.getInstance().registryHandleMessage(registryToServerHead.getEvent(), registryToServerHead.getMessage());
        }
    }
}