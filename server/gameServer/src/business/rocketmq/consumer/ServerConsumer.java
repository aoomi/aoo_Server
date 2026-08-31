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
 * 服务间通知
 */
@Data
@Consumer(topic = Config.SERVER_NOTIFY)
public class ServerConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) throws ClassNotFoundException {
        byte[] req = (byte[]) body;
        if (req.length <= 0) {
            CommLog.error("RegistryConsumer body length <= 0");
            return;
        }
        ByteBuf byteBuf = Unpooled.wrappedBuffer(req);
        ServerToServerHead serverToServerHead = InnerMessageCodecFactory.getInstance().readRegistryToServerHead(byteBuf);
        if (Config.ServerID() != serverToServerHead.getServerId()) {
            HandlerServerDispatcher.getInstance().registryHandleMessage(serverToServerHead.getEvent(), serverToServerHead.getMessage());
        }
    }
}