package com.ddm.server.websocket.server;

import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.BaseSession;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.def.TerminalType;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.handler.response.ResponseHandler;
import com.ddm.server.websocket.message.MessageWapper;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.mina.core.session.IoSession;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashMap;
import com.ddm.server.protocol.v2.ProtocolEnvelope;
import com.ddm.server.protocol.v2.ProtocolMessageNames;
import com.ddm.server.protocol.v2.ProtocolV2Bridge;
import com.google.gson.JsonParser;


/**
 * server连接代理对象
 *
 * @author blade
 */
public abstract class ServerSession extends BaseSession {

    private volatile boolean protocolV2;
    private volatile boolean protocolTicketValidated;
    private volatile long protocolAuthenticatedAccountId;
    private long lastProtocolV2Sequence;
    private final Map<String, Boolean> protocolV2RequestIds = new LinkedHashMap<String, Boolean>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 2048;
        }
    };

    public void setProtocolV2(boolean protocolV2) {
        this.protocolV2 = protocolV2;
    }

    public boolean isProtocolV2() {
        return protocolV2;
    }

    public boolean isProtocolTicketValidated() { return protocolTicketValidated; }

    public synchronized void bindProtocolAccount(long accountId) {
        if (accountId <= 0L) throw new IllegalArgumentException("Protocol account must be positive");
        if (protocolTicketValidated && protocolAuthenticatedAccountId != accountId) {
            throw new IllegalArgumentException("Protocol account cannot change on an established connection");
        }
        protocolAuthenticatedAccountId = accountId;
        protocolTicketValidated = true;
    }

    public long getProtocolAuthenticatedAccountId() {
        if (!protocolTicketValidated || protocolAuthenticatedAccountId <= 0L) {
            throw new IllegalStateException("Protocol connection is not authenticated");
        }
        return protocolAuthenticatedAccountId;
    }

    public synchronized void acceptProtocolV2Request(String requestId, long sequence, long timestamp) {
        long drift = Math.abs(System.currentTimeMillis() - timestamp);
        if (drift > 30_000L) throw new IllegalArgumentException("Protocol timestamp expired");
        if (sequence <= lastProtocolV2Sequence) throw new IllegalArgumentException("Protocol sequence replayed");
        if (protocolV2RequestIds.containsKey(requestId)) throw new IllegalArgumentException("Protocol request replayed");
        protocolV2RequestIds.put(requestId, Boolean.TRUE);
        lastProtocolV2Sequence = sequence;
    }

    private Map<String, ServerSocketRequestMgr> requestMgrs = new HashMap<>();

    private int remoteServerID = -1;
    private TerminalType remoteServerType;

    private int localServerid;
    private TerminalType localServerType;

    //mina的方法
    public ServerSession(TerminalType localServerType, int localServerid, TerminalType remoteServerType, IoSession session, long sessionID) {
        super(session, sessionID);
        this.localServerType = localServerType;
        this.localServerid = localServerid;
        this.remoteServerType = remoteServerType;
    }

    //netty的方法
    public ServerSession(TerminalType localServerType, int localServerid, TerminalType remoteServerType, Channel session, long sessionID) {
        super(session, sessionID);
        this.localServerType = localServerType;
        this.localServerid = localServerid;
        this.remoteServerType = remoteServerType;
    }

    public void setRemoteServerID(int serverID) {
        this.remoteServerID = serverID;
    }

    public int getRemoteServerID() {
        return this.remoteServerID;
    }

    public TerminalType getRemoteServerType() {
        return this.remoteServerType;
    }

    public int getLocalServerid() {
        return this.localServerid;
    }

    private ServerSocketRequestMgr getRequestMgr(String opcode) {
        ServerSocketRequestMgr requestMgr = this.requestMgrs.get(opcode);
        if (requestMgr == null) {
            synchronized (this.requestMgrs) {
                requestMgr = this.requestMgrs.get(opcode);
                if (requestMgr == null) {
                    requestMgr = new ServerSocketRequestMgr(this, opcode);
                    this.requestMgrs.put(opcode, requestMgr);
                }
            }
        }
        return requestMgr;
    }

    public void request(TerminalType desctype, long descid, String opcode, Object protocol, ResponseHandler handler, String subjectTopic) {
        ServerSocketRequest request = this.getRequestMgr(opcode).genRequest(handler);
        if (request != null) {
            this.sendRequest(MessageType.Request, desctype, descid, opcode, request.getSequence(), protocol, subjectTopic);
        } else {
            CommLogD.error("[ServerSession]消息请求队列已满，发送失败!");
        }
    }

    public void request(String opcode, Object protocol, ResponseHandler handler, String subjectTopic) {
        this.request(this.remoteServerType, this.remoteServerID, opcode, protocol, handler, subjectTopic);
    }

    public void notifyMessage(TerminalType serverType, long serverid, String opcode, Object protocol, String subjectTopic) {
        // System.out.println("Go To SererSesion
        // MessageType.Request"+serverType);
        // System.out.println("Go To SererSesion serverid"+serverid);
        // System.out.println("Go To SererSesion opcode"+opcode);
        // System.out.println("Go To SererSesion protocol"+protocol.toString());

        this.sendRequest(MessageType.Request, serverType, serverid, opcode, (short) -1, protocol, subjectTopic);
    }

    public void notifyMessage(String opcode, Object protocol, String subjectTopic) {

        this.notifyMessage(this.remoteServerType, this.remoteServerID, opcode, protocol, subjectTopic);
    }

    public void notifyMessage(String opcode, Object protocol) {

        this.notifyMessage(this.remoteServerType, this.remoteServerID, opcode, protocol, null);
    }

    public ServerSocketRequest popRequest(String opcode, short sequence) {
        return this.getRequestMgr(opcode).popRequest(sequence);
    }

    public void checkTimeoutRequest() {
        for (ServerSocketRequestMgr requestMgr : new ArrayList<>(this.requestMgrs.values())) {
            requestMgr.checkTimeoutRequest();
        }
    }

    public void sendRequest(MessageType messageType, TerminalType desctype, long descId, String opcode, short sequence, Object proto, String subjectTopic) {
        MessageHeader header = new MessageHeader();
        header.messageType = messageType;
        header.srcType = this.localServerType.value();
        header.srcId = this.localServerid;
        header.descType = (byte) desctype.ordinal();
        header.descId = descId;
        header.event = opcode;
        header.sequence = sequence;
        this.sendPacket(header, proto, subjectTopic);
    }

    public void sendResponse(MessageHeader srcHeader, String body, String subjectTopic) {
        this.sendPacket(srcHeader.genResponseHeader(), body, subjectTopic);
    }

    public void sendResponse(MessageHeader srcHeader, String body) {
        this.sendPacket(srcHeader.genResponseHeader(), body, null);
    }

    public void sendResponseObj(MessageHeader srcHeader, Object body, String subjectTopic) {
        this.sendPacket(srcHeader.genResponseHeader(), body, subjectTopic);
    }

    public void sendResponseObj(MessageHeader srcHeader, Object body) {
        this.sendPacket(srcHeader.genResponseHeader(), body, null);
    }

    public void sendError(MessageHeader srcHeader, ErrorCode errorCode, String message, String subjectTopic) {
        this.sendError(srcHeader.genResponseHeader(errorCode.value()), errorCode.value(), message, subjectTopic);
    }

    public void sendError(MessageHeader srcHeader, ErrorCode errorCode, String message) {
        this.sendError(srcHeader.genResponseHeader(errorCode.value()), errorCode.value(), message, null);
    }

    public void sendError(MessageHeader srcHeader, short errorCode, String message, String subjectTopic) {
        this.sendPacket(srcHeader.genResponseHeader(errorCode), message, subjectTopic);
    }

    public void sendError(MessageHeader srcHeader, short errorCode, String message) {
        this.sendPacket(srcHeader.genResponseHeader(errorCode), message, null);
    }

    private void sendPacket(MessageHeader header, Object body, String subjectTopic) {
        this.sendPacket(header, new Gson().toJson(body), subjectTopic);
    }

    /**
     * 最大的数据包长度
     */
    private final static int MAX_DATA_LENGTH = 25535;

    private void sendPacket(MessageHeader header, String body, String subjectTopic) {
        if (protocolV2 && !ProtocolV2Bridge.DISPATCH_EVENT.equals(header.event)) {
            ProtocolEnvelope push = new ProtocolEnvelope();
            push.msgId = ProtocolMessageNames.canonicalPush(header.event);
            push.kind = "push";
            push.requestId = UUID.randomUUID().toString();
            push.seq = System.currentTimeMillis();
            push.timestamp = System.currentTimeMillis();
            push.traceId = push.requestId;
            try { push.body = JsonParser.parseString(body); }
            catch (RuntimeException ignored) { push.body = body; }
            header.event = "protocol.v2.push";
            body = new Gson().toJson(push);
        }
        CommLogD.info("response service data sessionID:{}, bytes:{}, interface:{}", getSessionId(), body.length(), header.event);
        StringBuilder playBack = new StringBuilder(body.toString());
        if (Objects.nonNull(playBack)) {
            // 获取最大长度
            int maxLength = playBack.length();
            // 可拆得包数量
            int number = (maxLength / MAX_DATA_LENGTH);
            // 共下发多少包
            int playBackNum = maxLength % MAX_DATA_LENGTH == 0 ? number : number + 1;
            this.subPlayBack(0, 0, maxLength, playBack, playBackNum, header, subjectTopic);
        }
    }


    /**
     * 截取 回放数据分段
     *
     * @param idx
     * @param start
     * @param maxLength
     * @param playBack
     */
    public void subPlayBack(int idx, int start, int maxLength, StringBuilder playBack, int playBackNum, MessageHeader header, String subjectTopic) {
        int end = 0;
        end = start + MAX_DATA_LENGTH;
        if (end < maxLength) {
            String msg = playBack.substring(start, end);
            this.sendPacket(header, msg, playBackNum, idx, subjectTopic);
        } else {
            String msg = playBack.substring(start, maxLength);
            this.sendPacket(header, msg, playBackNum, idx, subjectTopic);
            return;
        }
        idx++;
        this.subPlayBack(idx, end, maxLength, playBack, playBackNum, header, subjectTopic);

    }

    private void sendPacket(MessageHeader header, String body, int size, int id, String subjectTopic) {
        if (Objects.nonNull(channel)) {
            ByteBuf buf = encodeMessageWapper(MessageWapper.make(header, body), size, id);
            this.sendToServer(buf, subjectTopic);
            return;
        }
        try {
            byte[] event = header.event.getBytes("utf-8");
            byte[] bodybytes = body.getBytes("utf-8");
            if (bodybytes.length > 65535) {
                CommLogD.error("发送协议过长无法发送. srctype:{}, srcid:{}, event:{}, sequance:{}, bytes:{}", //
                        header.srcType, header.srcId, header.event, header.sequence, bodybytes.length //
                );
                return;
            }

//			CommLogD.error("发送协议. srctype:{}, srcid:{}, event:{}, sequance:{}, body:{}",
//					 header.srcType, header.srcId, header.event, header.sequence, body );

            int length = 1 + 1 + 8 + 1 + 8 + 2 + event.length + 2 + 2 + 2 + bodybytes.length + 2 + 2;
            ByteBuffer buff = ByteBuffer.allocate(length);
            buff.put((byte) header.messageType.ordinal());// 长度 1
            buff.put(header.srcType);// 长度 1
            buff.putLong(header.srcId);// 长度 8
            buff.put(header.descType);// 长度 1
            buff.putLong(header.descId);// 长度 8
            // 共几包
            buff.putShort((short) size);
            // 当前第几包
            buff.putShort((short) id);

            buff.putShort((short) event.length);
            // CommLogD.error("put event.length:{} {}", event.length, event);
            buff.put(event);// event.length
            buff.putShort(header.sequence);// 长度 2
            buff.putShort(header.errcode);// 长度 2
            buff.putShort((short) bodybytes.length);
            // CommLogD.error("put bodybytes.length:{} {}", bodybytes.length,
            // bodybytes);
            //CommLogD.info("[S->C]=========== srctype:{}, srcid:{}, event:{}, sequance:{}, body:{}", header.srcType, header.srcId, header.event, header.sequence, body);// jason
            buff.put(bodybytes);// bodybytes.length
            buff.flip();
            if (session != null) {
                this.session.write(buff);
            } else {
                this.channel.writeAndFlush(buff.array());
            }
        } catch (Exception e) {
            CommLogD.error("发送协议发送错误. srctype:{}, srcid:{}, event:{}, sequance:{}, bytes:{}", //
                    header.srcType, header.srcId, header.event, header.sequence, body.length(), //
                    e);
        }
    }


    /**
     * 消息编码 .
     *
     * @param messageWapper .
     * @return .
     */
    public static ByteBuf encodeMessageWapper(MessageWapper messageWapper, int size, int id) {
        ByteBuf byteBuf = null;
        try {
            byte[] event = messageWapper.getHeader().event.getBytes("utf-8");
            byte[] msgName = messageWapper.getMsg().getBytes("utf-8");
            // 消息长度
            int length = 1 + 1 + 8 + 1 + 8 + 2 + event.length + 2 + 2 + 2 + msgName.length + 2 + 2;
            byteBuf = Unpooled.buffer(length);
//            // 长度 4
//            byteBuf.capacity(length);
            // 长度 1
            byteBuf.writeByte((byte) messageWapper.getHeader().messageType.ordinal());
            // 长度 1
            byteBuf.writeByte(messageWapper.getHeader().srcType);
            // 长度 8
            byteBuf.writeLong(messageWapper.getHeader().srcId);
            // 长度 1
            byteBuf.writeByte(messageWapper.getHeader().descType);
            // 长度 8
            byteBuf.writeLong(messageWapper.getHeader().descId);
            // 总包数
            byteBuf.writeShort((short) size);
            // 第几个包
            byteBuf.writeShort((short) id);
            // event 长度
            byteBuf.writeShort((short) event.length);
            byteBuf.writeBytes(event);
            // 长度 2
            byteBuf.writeShort(messageWapper.getHeader().sequence);
            // 长度 2
            byteBuf.writeShort(messageWapper.getHeader().errcode);
            // msg 长度
            byteBuf.writeShort((short) msgName.length);
            byteBuf.writeBytes(msgName);
            CommLogD.info("send protobuf msg [{}]", messageWapper.getMsg().getClass().getSimpleName());
        } catch (Exception exception) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", exception);
            CommLogD.error(exception.getMessage(), exception);
            byteBuf.resetWriterIndex();
        }
        return byteBuf;
    }

}
