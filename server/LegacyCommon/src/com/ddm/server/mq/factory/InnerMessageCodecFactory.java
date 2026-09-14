package com.ddm.server.mq.factory;

import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.message.MessageToServerHead;
import com.ddm.server.websocket.message.ServerToServerHead;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;


public class InnerMessageCodecFactory {
    private static InnerMessageCodecFactory instance = new InnerMessageCodecFactory();

    /**
     * accountId （long） pid (long) sessoinId(long) messageId (short) serverId (short) serverType (short) valid(byte)
     */
    private static final short Fix_Len = 8 + 8 + 8 + 2 + 2 + 2 + 1;

    public static InnerMessageCodecFactory getInstance() {
        return instance;
    }


    /**
     * @param messageHead
     * @param body
     * @return
     * @Desc 编码从网关到业务服务的消息, serverType不需要传送，在Message的实例中是可以找到的。
     */
    public ByteBuf gateToGameServerEncode(MessageToServerHead messageHead, ByteBuf body) {
        int bodySize = 0;
        if (body != null) {
            bodySize = body.readableBytes();
        }
        ByteBuf byteBuf = this.writeMessageHeader(messageHead, bodySize);
        if (body != null) {
            byteBuf.writeBytes(body);
        }
        return byteBuf;
    }

    private ByteBuf writeMessageHeader(MessageToServerHead messageHead, int bodySize) {
        int initialCapacity = Fix_Len + (StringUtils.isEmpty(messageHead.getTopic()) ? 0 : messageHead.getTopic().length()) + (StringUtils.isEmpty(messageHead.getIp()) ? 0 : messageHead.getIp().length()) + bodySize;
        ByteBuf byteBuf = Unpooled.buffer(initialCapacity);
        byteBuf.writeLong(messageHead.getAccountId());
        byteBuf.writeLong(messageHead.getPid());
        byteBuf.writeLong(messageHead.getSessoinId());
        byteBuf.writeShort(messageHead.getMessageId());
        byteBuf.writeShort(messageHead.getServerId());
        byteBuf.writeShort(messageHead.getServerType());
        byteBuf.writeByte(messageHead.getValid());
        if (StringUtils.isNotEmpty(messageHead.getTopic())) {
            byteBuf.writeShort(messageHead.getTopic().length());
            byteBuf.writeBytes(messageHead.getTopic().getBytes(CharsetUtil.UTF_8));
        } else {
            byteBuf.writeShort(0);
        }
        if (StringUtils.isNotEmpty(messageHead.getIp())) {
            byteBuf.writeShort(messageHead.getIp().length());
            byteBuf.writeBytes(messageHead.getIp().getBytes(CharsetUtil.UTF_8));
        } else {
            byteBuf.writeShort(0);
        }
        return byteBuf;
    }

    public MessageToServerHead readMessageToServerHead(ByteBuf buf) {
        MessageToServerHead gameMessageHead = new MessageToServerHead();
        gameMessageHead.setAccountId(buf.readLong());
        gameMessageHead.setPid(buf.readLong());
        gameMessageHead.setSessoinId(buf.readLong());
        gameMessageHead.setMessageId(buf.readShort());
        gameMessageHead.setServerId(buf.readShort());
        gameMessageHead.setServerType(buf.readShort());
        gameMessageHead.setValid(buf.readByte());
        int lenTopic = buf.readShort();
        if (lenTopic > 0) {
            byte[] topicBytes = new byte[lenTopic];
            buf.readBytes(topicBytes);
            gameMessageHead.setTopic(new String(topicBytes, CharsetUtil.UTF_8));
        }
        int lenIp = buf.readShort();
        if (lenIp > 0) {
            byte[] ipBytes = new byte[lenIp];
            buf.readBytes(ipBytes);
            gameMessageHead.setIp(new String(ipBytes, CharsetUtil.UTF_8));
        }
        return gameMessageHead;
    }


    /**
     * messageType （byte） sequence (short)
     */
    private static final short Registry_Fix_Len = 2 + 2;

    /***
     *
     * @param event 消息头
     * @param message 消息内容
     * @return
     */
    public ByteBuf registryToServerEncode(MessageType messageType, String event, String message) {
        int lengthEvent = StringUtils.isEmpty(event) ? 0 : event.length();
        int lengthMessage = StringUtils.isEmpty(message) ? 0 : message.length();
        int initialCapacity = Registry_Fix_Len + lengthEvent + lengthMessage;
        ByteBuf byteBuf = Unpooled.buffer(initialCapacity);
        byteBuf.writeShort(messageType.value());
        byteBuf.writeShort(Config.ServerID());
        if (lengthEvent > 0) {
            byteBuf.writeShort(lengthEvent);
            byteBuf.writeBytes(event.getBytes(CharsetUtil.UTF_8));
        }
        if (lengthMessage > 0) {
            byteBuf.writeShort(lengthMessage);
            byteBuf.writeBytes(message.getBytes(CharsetUtil.UTF_8));
        }
        return byteBuf;
    }


    public ServerToServerHead readRegistryToServerHead(ByteBuf buf) {
        ServerToServerHead registryToServerHead = new ServerToServerHead();
        registryToServerHead.setMessageId(buf.readShort());
        registryToServerHead.setServerId(buf.readShort());
        int lenEvent = buf.readShort();
        if (lenEvent > 0) {
            byte[] eventBytes = new byte[lenEvent];
            buf.readBytes(eventBytes);
            registryToServerHead.setEvent(new String(eventBytes, CharsetUtil.UTF_8));
        }
        int lenMessage = buf.readShort();
        if (lenMessage > 0) {
            byte[] messageBytes = new byte[lenMessage];
            buf.readBytes(messageBytes);
            registryToServerHead.setMessage(new String(messageBytes, CharsetUtil.UTF_8));
        }
        return registryToServerHead;
    }

}
