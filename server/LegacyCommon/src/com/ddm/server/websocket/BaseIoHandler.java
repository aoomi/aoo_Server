/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.websocket;

import BaseCommon.CommLog;
import BaseTask.SyncTask.SyncTaskManager;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.common.utils.NetUtil;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.netty.SessionConnectMgr;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public abstract class BaseIoHandler<Session extends BaseSession> implements IoHandler {

    public static final Object SESSION_ID = new Object(); // SESSION_ID
    public static final AtomicLong _IDFactory = new AtomicLong(1);
    protected final Map<Long, Session> connections = Maps.newConcurrentHashMap();
    protected IMessageDispatcher<Session> messageDispatcher;

    public Session getSession(long sessionId) {
        Session client = connections.get(sessionId);
        return client;
    }

    public abstract Session createSession(IoSession session, long sessionID);

    public void setMessageDispatcher(IMessageDispatcher<Session> messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
        SessionConnectMgr.getInstance().setMessageDispatcher(messageDispatcher);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
//        if (!(message instanceof String)) {
//            return;
//        }
//        long sessionId = (long) session.getAttribute(SESSION_ID);
//        final BaseSession client = connections.get(sessionId);
//        if (client == null) {
//            return;
//        }
//        SyncTaskManager.task(() -> client.onSent((String) message));
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        long sessionId = (long) session.getAttribute(SESSION_ID);
        if (messageDispatcher == null) {
            CommLogD.error("协议派发器未初始化");
            return;
        }
        Session client = connections.get(sessionId);
        if (client == null) {
            return;
        }

        if (message instanceof String) {
            BaseSession baseSession = client;
            if (!isAllowedWebSocketOrigin((String) message)) {
                CommLog.error("messageReceived rejected websocket Origin from client:{}", session.getRemoteAddress());
                session.write("HTTP/1.1 403 Forbidden\r\nConnection: close\r\nContent-Length: 0\r\n\r\n");
                session.closeOnFlush();
                return;
            }
            String oldClientIP = (String) session.getAttribute("KEY_SESSION_CLIENT_IP");
            String newClientIP = Optional.ofNullable(StringUtil.getIps(message.toString())).orElseGet(() -> NetUtil.LOCALHOST);
            baseSession.getSession().setAttribute("KEY_SESSION_CLIENT_IP", newClientIP);
            baseSession.resetRemoteIP();
            if (NetUtil.LOCALHOST.equals(newClientIP)) {
                CommLogD.debug("websocket handshake has no proxy client IP, using socket IP:{}", oldClientIP);
            }
            baseSession.getSession().write(this.getSecWebSocketAccept((String) message));
        } else if (message instanceof ByteBuffer) {
            messageDispatcher.handleRawMessage(client, (ByteBuffer) message);
        }
    }

    /** Browsers need an exact official Origin; native clients may omit the header. */
    static boolean isAllowedWebSocketOrigin(String request) {
        Matcher originHeader = Pattern.compile("^Origin:\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                .matcher(request == null ? "" : request);
        if (!originHeader.find()) return true;
        String configured = System.getProperty("AllowedWebSocketOrigins",
                System.getenv().getOrDefault("ALLOWED_WEBSOCKET_ORIGINS", ""));
        Set<String> allowed = Arrays.stream(configured.split(","))
                .map(BaseIoHandler::normalizeOrigin).filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return allowed.contains(normalizeOrigin(originHeader.group(1)));
    }

    private static String normalizeOrigin(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    public String getSecWebSocketAccept(String msg) throws UnsupportedEncodingException {
        String secKey = getSecWebSocketKey(msg);

        secKey += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(secKey.getBytes("iso-8859-1"), 0, secKey.length());
            byte[] sha1Hash = md.digest();
            secKey = new String(org.apache.mina.util.Base64.encodeBase64(sha1Hash), java.nio.charset.StandardCharsets.US_ASCII);
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }

        return "HTTP/1.1 101 Switching Protocols\r\n" // 头
                + "Upgrade: websocket\r\n" // upgrade
                + "Connection: Upgrade\r\n" // connect
                + "Sec-WebSocket-Accept: " + secKey + "\r\n\r\n";
    }

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

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        StringBuilder s = new StringBuilder();
        s.append(Calendar.getInstance().getTime().toString());
        s.append(":\n session status:\n");
        IoServiceStatistics stat = session.getService().getStatistics();
        s.append("Read Bytes:").append(stat.getReadBytesThroughput()).append("B/S\n");
        s.append("Write Bytes:").append(stat.getWrittenBytesThroughput()).append("B/S\n");
        s.append("Read Message:").append(stat.getReadMessagesThroughput()).append("pkt/S\n");
        s.append("Write Message:").append(stat.getWrittenMessagesThroughput()).append("pkt/S\n");
        CommLogD.info(s.toString());
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        if (session.getRemoteAddress() == null) {
            CommLogD.error("session {} created, but no remote address!", session.getRemoteAddress());
            return;
        }
        String clientIP = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();
        session.setAttribute("KEY_SESSION_CLIENT_IP", clientIP);
        long sessionId = _IDFactory.incrementAndGet();
        session.setAttribute(SESSION_ID, sessionId);
        final Session client = createSession(session, sessionId);
        connections.put(sessionId, client);

//        if (client != null) {
//            SyncTaskManager.task(() -> {
//                if (client != null) {
//                    client.onCreated();
//                }
//            });
//        }
//        CommLogD.debug("create session:{}, id:{}!!!", client.getClass().getName(), sessionId);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        CommLogD.debug("session {} opened at {}", SESSION_ID, CommTime.nowSecond());
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        long sessionId = (long) session.getAttribute(SESSION_ID);
        final BaseSession client = connections.remove(sessionId);
        if (client == null) {
            CommLogD.error("close session:{}, id:{}!!!", session.getClass().getName(), sessionId);
            return;
        }
        CommLogD.debug("close client:{}, id:{}!!!", client.getClass().getName(), sessionId);
        SyncTaskManager.task(() -> client.onClosed());
    }

    @Override
    public void exceptionCaught(IoSession session, final Throwable cause) throws Exception {
        session.close(true);
        CommLogD.info("client exceptionCaughted closed sessionid:{}", session.getAttribute(SESSION_ID));
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {
        session.close(true);
        CommLogD.info("client inputClosed sessionid:{}", session.getAttribute(SESSION_ID));
    }
}
