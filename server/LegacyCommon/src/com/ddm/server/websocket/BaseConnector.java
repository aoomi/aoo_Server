/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.websocket;

import BaseTask.SyncTask.SyncTaskManager;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.codecfactory.WebDecoder;
import com.ddm.server.websocket.codecfactory.WebEncoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public abstract class BaseConnector<Session extends BaseSession> {

    protected volatile String ip;
    protected volatile int port;

    protected NioSocketConnector connector;
    protected BaseIoHandler<Session> handler;
    protected ProtocolCodecFilter codecFilter;
    protected volatile Session _session;

    public BaseConnector(BaseIoHandler<Session> ioHandler) {
        handler = ioHandler;
        this.codecFilter = new ProtocolCodecFilter(new WebEncoder(), new WebDecoder());
    }

    public Session getSocketSession() {
        return _session;
    }

    /**
     * @param sa
     * @return
     * @throws Exception
     */
    private boolean connect(final SocketAddress sa, long timeout) {
        try {
            connector = new NioSocketConnector();
            config(connector);

            DefaultIoFilterChainBuilder chain = connector.getFilterChain();
            chain.addLast("logger", new LoggingFilter());
            chain.addLast("codec", codecFilter);

            connector.setHandler(handler);

            ConnectFuture f = connector.connect(sa);
            // Bind 阻塞
            f.awaitUninterruptibly(timeout);
            // 成功
            if (f.isConnected()) {
                Long sessionId = (Long) f.getSession().getAttribute(BaseIoHandler.SESSION_ID);
                _session = handler.getSession(sessionId);
                CommLogD.info("Connector {} Connnect to {} Success! ", getClass().getName(), sa.toString());
            } else {
                CommLogD.error("Connector {} Connnect to {} Failed! exception:{}\n", getClass().getName(), sa.toString(), f.getException());
                onConnectFailed();
            }
            return f.isConnected();
        } catch (Exception e) {
            CommLogD.error("Connector {} Connnect to {} Failed! \n{}", getClass().getName(), sa.toString(), e);
        }
        return false;
    }

    public BaseIoHandler<Session> getSocketHandler() {
        return handler;
    }

    protected void config(NioSocketConnector connector) {
        SocketSessionConfig sessioncfg = connector.getSessionConfig();
        sessioncfg.setReceiveBufferSize(8192);
        sessioncfg.setSendBufferSize(8192 * 8);
        sessioncfg.setKeepAlive(true);
        sessioncfg.setTcpNoDelay(false);

        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
    }

    public boolean isConnected() {
        return _session != null && _session.isConnected();
    }

    public void disconnect() {
        try {
            if (connector != null) {
                connector.dispose();
            }
            _session = null;
        } catch (Throwable ex) {
            CommLogD.error(null, ex);
        }
    }

    /**
     * @return
     */
    public synchronized boolean reconnect(String ip, int port) {
        if (isConnected()) {
            return true;
        }
        this.ip = ip;
        this.port = port;

        disconnect();
        // 连接服务端
        return connect(new InetSocketAddress(ip, port), 25000);
    }

    protected void onConnectFailed() {
        SyncTaskManager.task(() -> {
            reconnect(ip, port);
        }, 3 * 1000);
    }
}
