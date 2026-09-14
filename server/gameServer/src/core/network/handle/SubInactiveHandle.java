package core.network.handle;

import BaseCommon.CommLog;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.ddm.server.mq.factory.InnerMessageCodecFactory;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import com.ddm.server.netty.SessionConnectMgr;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.message.MessageToServerHead;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;

/**
 * 处理消费的消息
 */
@Data
public class SubInactiveHandle implements BaseExecutor {
    /**
     * 消息包
     */
    private byte[] body;

    public SubInactiveHandle(byte[] body) {
        this.body = body;
    }

    @Override
    public void invoke() {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(this.getBody());
        MessageToServerHead messageHead = InnerMessageCodecFactory.getInstance().readMessageToServerHead(byteBuf);
        CommLog.info(messageHead.toString());
        if (MessageType.Notify.value() == messageHead.getMessageId()) {
            MqConsumerTopicFactory.getInstance().tryConnectNotify(messageHead.getTopic());
            MqConsumerTopicFactory.getInstance().addNotifySet(messageHead.getTopic());
        } else if (MessageType.Request.value() == messageHead.getMessageId()) {
            // 向其他服务器请求，必定回包
            this.invokeRequest(messageHead, byteBuf);
        } else {
            CommLog.error("[SubInactiveHandle] invoke error message:{}", messageHead.toString());
        }
    }


    /**
     * 向其他服务器请求，必定回包
     *
     * @param messageHead 消息头部
     * @param byteBuf     消息内容
     */
    private void invokeRequest(MessageToServerHead messageHead, ByteBuf byteBuf) {
        SessionConnectMgr.getInstance().getMessageDispatcher().handleRawMessageMQ(messageHead, byteBuf);
    }

    @Override
    public int threadId() {
        return 0;
    }
}
