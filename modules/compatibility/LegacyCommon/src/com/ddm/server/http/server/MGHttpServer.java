package com.ddm.server.http.server;

import BaseCommon.CommLog;
import com.ddm.server.common.utils.DefaultThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

public class MGHttpServer {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static MGHttpServer instance = new MGHttpServer();
    }


    // 私有化构造方法
    private MGHttpServer() {
    }

    // 获取单例
    public static MGHttpServer getInstance() {
        return MGHttpServer.SingletonHolder.instance;
    }

    // http服务
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public void createServer(int port, HttpDispather handler, String path) throws IOException {
        CommLog.info("[MGHttpServer.init] load http begin...]");
        this.bossGroup = new NioEventLoopGroup(4, new DefaultThreadFactory(this.getClass()));
        this.workerGroup = new NioEventLoopGroup(4, new DefaultThreadFactory(MGHttpServer.class));
        try {
            this.serverChannel = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(new HttpServerCodec());
                            channel.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                            channel.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(io.netty.channel.ChannelHandlerContext context, FullHttpRequest request) {
                                    handler.handle(context, request);
                                }
                            });
                        }
                    })
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .bind(port).syncUninterruptibly().channel();
        } catch (RuntimeException e) {
            stop();
            throw new IOException("Failed to start HTTP server on port " + port, e);
        }
        CommLog.info("[MGHttpServer.init] load http success, port: {}", port);
    }


    public void stop() {
        if (serverChannel != null) serverChannel.close().syncUninterruptibly();
        if (workerGroup != null) workerGroup.shutdownGracefully().syncUninterruptibly();
        if (bossGroup != null) bossGroup.shutdownGracefully().syncUninterruptibly();
        serverChannel = null;
        workerGroup = null;
        bossGroup = null;
    }


}
