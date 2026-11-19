/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.websocket;

import com.ddm.server.common.CommLogD;
import com.ddm.server.netty.ServerHandler;
import com.ddm.server.netty.WebSocketServerInitializer;
import com.ddm.server.websocket.codecfactory.WebDecoder;
import com.ddm.server.websocket.codecfactory.WebEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.net.InetSocketAddress;


/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public abstract class BaseAcceptor<Session extends BaseSession> {

    private String ip;
    private int port;

    protected NioSocketAcceptor acceptor;
    //mina的handler
    protected BaseIoHandler<Session> handler = null;
    //netty的handler
    protected ServerHandler<Session> serverHandler = null;
    //mina协议器
    protected ProtocolCodecFilter codecFilter = null;
    //netty管道管理器
    protected WebSocketServerInitializer handlers = new WebSocketServerInitializer();
    //boss注册和解除线程管理
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    //work消息接收和发送管理
    private final EventLoopGroup workGroup = new NioEventLoopGroup();
    //启动netty的promise
    private ChannelFuture channelFuture;

    /**
     * 构造方法
     */
    public BaseAcceptor() {
    }

    /**
     * 初始化socket
     *
     * @param type
     * @param handler
     */
    public void initSocket(int type, Object handler) {
        if (type == 1) {//mina
            this.handler = (BaseIoHandler<Session>) handler;
            this.codecFilter = new ProtocolCodecFilter(new WebEncoder(), new WebDecoder());
        } else {//netty
            this.serverHandler = (ServerHandler<Session>) handler;
        }
    }

    /**
     * 启动mina
     *
     * @param ip
     * @param port
     * @return
     */
    public boolean startSocket(String ip, int port) {
        this.ip = ip;
        this.port = port;
        try {
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;
            this.acceptor = new NioSocketAcceptor(threadCount);
            this.config();
            DefaultIoFilterChainBuilder chain = this.acceptor.getFilterChain();
            chain.addLast("logger", new LoggingFilter());
            chain.addLast("codec", this.codecFilter);
            this.acceptor.setHandler(this.handler);
            this.acceptor.bind(new InetSocketAddress(ip, port));
            CommLogD.info("Service {} listening on ip {}, port {}", this.getClass().getName(), ip, port);
            return true;
        } catch (Throwable ex) {
            CommLogD.error("Service {} on ip {}, port {}, faield!!!\n{}", this.getClass().getName(), ip, port, ex);
            System.exit(-1);
            return false;
        }
    }

    /**
     * 启动netty管道
     *
     * @param ip
     * @param port
     * @return
     */
    public boolean startChannel(String ip, int port) {
        this.ip = ip;
        this.port = port;
        try {
            handlers.setHandler(serverHandler);
            ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup, workGroup)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.SO_RCVBUF, 8192)
                    .childOption(ChannelOption.SO_SNDBUF, 8192 * 4)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(handlers);
            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());
            System.getLogger(BaseAcceptor.class.getName()).log(System.Logger.Level.INFO,
                    "Starting WebSocket server on port {0}", port);
            channelFuture = b.bind(port).sync();
        } catch (Exception e) {
            System.getLogger(BaseAcceptor.class.getName()).log(System.Logger.Level.ERROR,
                    "Unable to start WebSocket server", e);
            return false;
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdown();
                }
            });
        }
        return channelFuture != null && channelFuture.isSuccess();
    }

    /**
     * netty关闭
     */
    public void shutdown() {
        if (channelFuture != null) {
            channelFuture.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully();
            CommLogD.error("Service {} on ip {}, port {}, closed!", this.getClass().getName(), this.ip, this.port);
        }
    }

    /**
     * 关闭mina
     */
    public void close() {
        if (acceptor != null) {
            this.acceptor.dispose();
            CommLogD.error("Service {} on ip {}, port {}, closed!", this.getClass().getName(), this.ip, this.port);
        }
    }

    public boolean isOpened() {
        return this.acceptor.isActive() && !this.acceptor.isDisposing();
    }

    /**
     * mina的配置
     */
    private void config() {
        SocketSessionConfig sessioncfg = this.acceptor.getSessionConfig();
        sessioncfg.setReceiveBufferSize(8192);
        sessioncfg.setSendBufferSize(8192 * 4);
        sessioncfg.setKeepAlive(true);
        sessioncfg.setTcpNoDelay(false);

        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());

        this.acceptor.setBacklog(1024);
        this.acceptor.setReuseAddress(true);
    }

    /**
     * 监听指定端口所有IP地址的通信
     *
     * @param port
     * @return
     */
    public boolean startSocket(Integer port) {
        if (handler != null) {
            return this.startSocket("0.0.0.0", port);
        } else {
            return this.startChannel("0.0.0.0", port);
        }
    }
}
