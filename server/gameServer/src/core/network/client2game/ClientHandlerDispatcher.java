package core.network.client2game;

import BaseCommon.CommLog;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.netty.SessionConnectMgr;
import com.ddm.server.websocket.BaseIoHandler;
import com.ddm.server.websocket.BaseSession;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.def.TerminalType;
import com.ddm.server.websocket.handler.MessageDispatcher;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.handler.requset.NotifyDispatcher;
import com.ddm.server.websocket.handler.requset.RequestDispatcher;
import com.ddm.server.websocket.handler.response.ResponseDispatcher;
import com.ddm.server.websocket.message.MessageToServerHead;
import com.ddm.server.websocket.server.ServerMessageDispatcher;
import core.network.client2game.handler.BaseHandler;
import core.server.ServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

import java.nio.ByteBuffer;
import java.util.Objects;

public class ClientHandlerDispatcher extends ServerMessageDispatcher<ClientSession> {

    @Override
    public void init() {
        RequestDispatcher<ClientSession> dRequest = new RequestDispatcher<ClientSession>(TerminalType.GameServer,
                ServerConfig.ServerID()) {
            @Override
            public void forward(ClientSession session, MessageHeader header, String data) {
                CommLogD.error("[ClientHandlerDispatcher]暂时无跨服功能,消息无法转发");
            }
        };
        this.putDispatcher(MessageType.Request, dRequest);

        NotifyDispatcher<ClientSession> dNotify = new NotifyDispatcher<ClientSession>(TerminalType.GameServer,
                ServerConfig.ServerID(), dRequest) {
            @Override
            public void forward(ClientSession session, MessageHeader header, String data) {
                CommLogD.error("[ClientHandlerDispatcher]暂时无跨服功能,消息无法转发");
            }
        };
        this.putDispatcher(MessageType.Notify, dNotify);

        ResponseDispatcher<ClientSession> dResponse = new ResponseDispatcher<ClientSession>(TerminalType.GameServer,
                ServerConfig.ServerID()) {
            @Override
            public void forward(ClientSession session, MessageHeader header, String data) {
                CommLogD.error("[ClientHandlerDispatcher]暂时无跨服功能,消息无法转发");
            }
        };
        this.putDispatcher(MessageType.Response, dResponse);

        this.registerRequestHandlers(BaseHandler.class);
    }

    /**
     * mina消息处理
     *
     * @param session
     * @param stream
     * @return
     */
    @Override
    public int handleRawMessage(final ClientSession session, ByteBuffer stream) {
        try {
            MessageHeader header = new MessageHeader();
            header.messageType = MessageType.values()[stream.get()];// 客户端提取
            // header.srcType = (byte) TerminalType.Client.ordinal();
            // header.srcId = session.getPlayer() == null ? 0 :
            // session.getPlayer().getPid();
            // header.descType = (byte) TerminalType.GameServer.ordinal();
            // header.descId = ServerConfig.ServerID();
            header.sequence = stream.getShort();// 客户端提取

            short length = stream.getShort();
            byte[] event = new byte[length];
            stream.get(event);
            header.event = new String(event, "UTF-8");// 客户端提取

            MessageDispatcher<ClientSession> dispatcher = _messageDispatcher.get(header.messageType);
            if (dispatcher == null) {
                CommLogD.error("[WSBaseSocketListener] 协议类型错误 类型:{}", header.messageType);
                return -1;
            }
            if (!session.checkSequenceException(header.sequence)) {
                CommLog.error("[handleRawMessage] accountId:{},event:{},sequence:{}", session.getAccountID(), header.event, header.sequence);
                return -3;
            }
            byte[] msg = new byte[stream.getShort()];
            stream.get(msg);
//			CommLogD.error("[C->S]---------------messageType:{},sequence:{}    strlen:{},event:{},    strlen:{},msg:{}",
//					header.messageType, header.sequence, length, header.event, msg.length, new String(msg, "UTF-8"));// jason
            return dispatcher.dispatch(session, header, new String(msg, "UTF-8"));
        } catch (Exception e) {
            CommLogD.error("[WSBaseSocketListener] 协议处理协议信息处理时错误:", e);
        }
        return -2;
    }

    /**
     * netty处理消息
     *
     * @param session
     * @param stream
     * @return
     */
    @Override
    public int handleRawMessage(ClientSession session, ByteBuf stream) {
        try {
            MessageHeader header = new MessageHeader();
            header.messageType = MessageType.values()[stream.readByte()];// 客户端提取
            header.sequence = stream.readShort();// 客户端提取
            short length = stream.readShort();
            byte[] event = new byte[length];
            stream.readBytes(event);
            header.event = new String(event, "UTF-8");// 客户端提取
            MessageDispatcher<ClientSession> dispatcher = _messageDispatcher.get(header.messageType);
            if (dispatcher == null) {
                CommLogD.error("[WSBaseSocketListener] 协议类型错误 类型:{}", header.messageType);
                return -1;
            }
            if (!session.checkSequenceException(header.sequence)) {
                CommLogD.error("[handleRawMessage] accountId:{},event:{},sequence:{}", session.getAccountID(), header.event, header.sequence);
                return -3;
            }
            byte[] msg = new byte[stream.readShort()];
            stream.readBytes(msg);
            return dispatcher.dispatch(session, header, new String(msg, "UTF-8"));
        } catch (Exception e) {
            CommLogD.error("[WSBaseSocketListener] 协议处理协议信息处理时错误:", e);
        }
        return -2;
    }


    /**
     * netty处理消息
     *
     * @param messageHead
     * @param stream
     * @return
     */
    @Override
    public int handleRawMessageMQ(MessageToServerHead messageHead, ByteBuf stream) {
        try {
            MessageHeader header = new MessageHeader();
            header.messageType = MessageType.values()[stream.readByte()];// 客户端提取
            header.sequence = stream.readShort();// 客户端提取
            short length = stream.readShort();
            byte[] event = new byte[length];
            stream.readBytes(event);
            header.event = new String(event, "UTF-8");// 客户端提取
            MessageDispatcher<ClientSession> dispatcher = _messageDispatcher.get(header.messageType);
            if (dispatcher == null) {
                CommLogD.error("[WSBaseSocketListener] 协议类型错误 类型:{}", header.messageType);
                return -1;
            }
            ClientSession clientSession = invokeRequest(messageHead, header.event);
            if (Objects.isNull(clientSession)) {
                CommLogD.error("[WSBaseSocketListener] messageHead 类型:{}", messageHead.toString());
                return -1;
            }
            if (!clientSession.checkSequenceException(header.sequence)) {
                CommLogD.error("[handleRawMessageMQ] accountId:{},event:{},sequence:{}", messageHead.getAccountId(), header.event, header.sequence);
                return -3;
            }
            byte[] msg = new byte[stream.readShort()];
            stream.readBytes(msg);
            return dispatcher.dispatch(clientSession, header, new String(msg, "UTF-8"));
        } catch (Exception e) {
            CommLogD.error("[WSBaseSocketListener] 协议处理协议信息处理时错误:", e);
        }
        return -2;
    }

    /**
     * 创建新客户端连接
     *
     * @param messageHead
     * @return
     */
    private ClientSession newClientSession(MessageToServerHead messageHead) {
        BaseSession session = SessionConnectMgr.getInstance().getAccountIdToSession(messageHead.getAccountId());
        if (Objects.isNull(session)) {
            session = SessionConnectMgr.getInstance().newSession(new ClientSession(new EmbeddedChannel(), BaseIoHandler._IDFactory.incrementAndGet()), messageHead.getAccountId());
        }
        session.resetSession(messageHead);
        return (ClientSession) session;
    }

    /**
     * 向其他服务器请求，必定回包
     *
     * @param messageHead 消息头部
     *                    \
     */
    private ClientSession invokeRequest(MessageToServerHead messageHead, String headerEvent) {
        // 没有验证过的消息
        // 如果没有走base.都是问题请求需要拦截
        if (headerEvent.startsWith("base.")) {
            return newClientSession(messageHead);
        }
        Player player = messageHead.getPid() <= 0L ? PlayerMgr.getInstance().getPlayerByAccountID(messageHead.getAccountId()) : PlayerMgr.getInstance().getPlayer(messageHead.getPid());
        if (messageHead.getValid() == 1) {
            if (Objects.isNull(player)) {
                CommLog.error("invokeRequest player null messageHead:{}", messageHead.toString());
                return null;
            } else {
                // 验证过的用户
                if (!player.isOnline()) {
                    ClientSession clientSession = this.newClientSession(messageHead);
                    clientSession.setValid(true);
                    PlayerMgr.getInstance().connectPlayer(clientSession, player);
                } else if (ErrorCode.Not.name().equals(player.getSession().getCurPubTopic())) {
                    player.getSession().resetSession(messageHead);
                }
                return player.getSession();
            }
        }
        return null;
    }
}
