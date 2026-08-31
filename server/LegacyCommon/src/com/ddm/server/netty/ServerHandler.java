package com.ddm.server.netty;

import BaseTask.SyncTask.SyncTaskManager;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.websocket.BaseSession;
import com.ddm.server.websocket.IMessageDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息处理类
 */
@Sharable
public abstract class ServerHandler<Session extends BaseSession> extends SimpleChannelInboundHandler<Object> {
    // 每个连接独立保存握手器；本 Handler 是 @Sharable，不能共享可变握手状态。
    private static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR =
            AttributeKey.valueOf("webSocketHandshaker");
    //webSocket地址
    private static final String WEB_SOCKET_URL = "ws://localhost:9999/websocket";
    //channel的sessionID
    private static final AtomicLong _IDFactory = new AtomicLong(1);
    //连接map<sessionID,session>
    protected final Map<Long, Session> connections = Maps.newConcurrentHashMap();
    //消息处理器
    protected IMessageDispatcher<Session> messageDispatcher;
    //chanel的sessionKey
    protected AttributeKey<Long> sessionAttr = AttributeKey.valueOf("sessionID");

    /**
     * 通过sessionID获取session
     *
     * @param sessionId
     * @return
     */
    public Session getSession(long sessionId) {
        Session client = connections.get(sessionId);
        return client;
    }

    /**
     * 创建session
     *
     * @param session
     * @param sessionID
     * @return
     */
    public abstract Session createSession(Channel session, long sessionID);

    /**
     * 设置消息处理器
     *
     * @param messageDispatcher
     */
    public void setMessageDispatcher(IMessageDispatcher<Session> messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    /**
     * webSocket创建连接
     *
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() == null) {
            CommLogD.error("session {} created, but no remote address!", ctx.channel().localAddress());
            return;
        }
        long sessionId = _IDFactory.incrementAndGet();
        ctx.attr(sessionAttr).setIfAbsent(sessionId);
        final Session client = createSession(ctx.channel(), sessionId);
        connections.put(sessionId, client);
        //System.out.println("客户端与服务端连接开启--------"+"现在人数:"+connections.size());
        ctx.fireChannelActive();
    }

    /**
     * 客户端失去连接
     *
     * @param ctx
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
        long sessionId = ctx.attr(sessionAttr).get();
        final BaseSession client = connections.remove(sessionId);
        if (client == null) {
            CommLogD.error("client==null" + sessionId);
            return;
        }
        //System.out.println("客户端与服务端连接关闭--------"+"现在人数:"+connections.size());
        SyncTaskManager.task(() -> client.onClosed());
    }

    /**
     * 异常关闭
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        //System.out.println("异常消息关闭--------"+ctx.channel().attr(sessionAttr)+"现在人数:"+connections.size());
        CommLogD.info(ctx.attr(sessionAttr).get() + ":exceptionCaught:" + cause.getMessage());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent idle && idle.state() == IdleState.READER_IDLE) {
            CommLogD.info("Closing idle WebSocket session:{}", ctx.attr(sessionAttr).get());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, event);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) {
        if (messageDispatcher == null) {
            CommLogD.error("协议派发器未初始化");
            return;
        }
        long sessionId = channelHandlerContext.channel().attr(sessionAttr).get();
        Session client = connections.get(sessionId);
        if (client == null) {
            return;
        }

        if (message instanceof FullHttpRequest) { //握手连接
            handHttpRequest(channelHandlerContext, (FullHttpRequest) message);
        } else if (message instanceof WebSocketFrame) {
            WebSocketFrame frame = (WebSocketFrame) message;
            // Protocol-control and unsupported text frames must never enter the
            // binary game packet decoder.
            if (!handlerWebSocketFrame(channelHandlerContext, frame)) return;
            ByteBuf buf = frame.content();
            if (buf.readableBytes() > (1 << 20)) {
                channelHandlerContext.close();
                return;
            }
            messageDispatcher.handleRawMessage(client, buf);
        }
    }

    /**
     * 处理二进制数据
     *
     * @param ctx
     * @param frame
     */
    private boolean handlerWebSocketFrame(ChannelHandlerContext ctx,
                                       WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {// 关闭链路的指令
            WebSocketServerHandshaker handShaker = ctx.channel().attr(HANDSHAKER_ATTR).get();
            if (handShaker != null) {
                handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            } else {
                ctx.close();
            }
            //System.out.println("接收到关闭链路的指令");
            return false;
        }
        if (frame instanceof PingWebSocketFrame) {  // ping消息
            ctx.channel().write(
                    new PongWebSocketFrame(frame.content().retain()));
            //System.out.println("接收到ping消息");
            return false;
        }
        if (frame instanceof BinaryWebSocketFrame) { // 二进制数据接收
//            ByteBuf buf = frame.content();
//            StringBuilder sb= new StringBuilder();
//            for (int i = 0; i < buf.capacity(); i++){
//                byte b = buf.getByte(i);
//                sb.append(b);
//                sb.append(",");
//            }
//            System.out.println("接收到二进制消息"+sb.toString());
            return true;
        }
        ctx.close();
        return false;
    }

    /**
     * http握手连接
     *
     * @param ctx
     * @param req
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.getDecoderResult().isSuccess()
                || !("websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                WEB_SOCKET_URL, null, false);
        WebSocketServerHandshaker handShaker = wsFactory.newHandshaker(req);
        if (handShaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            ctx.channel().attr(HANDSHAKER_ATTR).set(handShaker);
            //握手
            handShaker.handshake(ctx.channel(), req);
            try {
                //握手完毕后发送ResPonce
                ctx.channel().writeAndFlush(this.getSecWebSocketAccept(req.toString()));
            } catch (Exception e) {
                CommLogD.error("WebSocket handshake failed", e);
            }
        }
    }

    /**
     * 服务端响应请求
     *
     * @param ctx
     * @param req
     * @param res
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req,
                                  DefaultFullHttpResponse res) {
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        //服务端向客户端发送数据
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 获取socket握手返回
     *
     * @param msg
     * @return
     * @throws UnsupportedEncodingException
     */
    public String getSecWebSocketAccept(String msg) {
        String secKey = getSecWebSocketKey(msg);

        secKey += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(secKey.getBytes("iso-8859-1"), 0, secKey.length());
            byte[] sha1Hash = md.digest();
            secKey = Base64.getEncoder().encodeToString(sha1Hash);
        } catch (Exception e) {
            CommLogD.error("Unable to calculate WebSocket accept key", e);
        }

        return "HTTP/1.1 101 Switching Protocols\r\n" // 头
                + "Upgrade: websocket\r\n" // upgrade
                + "Connection: Upgrade\r\n" // connect
                + "Sec-WebSocket-Accept: " + secKey + "\r\n\r\n";
    }

    /**
     * 获取webSocket的key
     *
     * @param req
     * @return
     */
    private String getSecWebSocketKey(String req) {
        Pattern p = Pattern.compile("^(Sec-WebSocket-Key:).+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(req);
        if (m.find()) {
            String foundstring = m.group();
            return foundstring.split(":")[1].trim();
        } else {
            return null;
        }
    }
}
