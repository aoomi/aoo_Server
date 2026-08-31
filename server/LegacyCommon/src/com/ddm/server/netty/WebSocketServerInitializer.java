package com.ddm.server.netty;

import com.ddm.server.websocket.codecfactory.MessageDecoder;
import com.ddm.server.websocket.codecfactory.MessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * netty的管道器
 */
public class WebSocketServerInitializer extends ChannelInitializer<NioSocketChannel> {

    private ServerHandler serverHandler;

    public void setHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 编解码 http 请求
        pipeline.addLast(new HttpServerCodec());
        // Close connections that send neither protocol heartbeat nor game traffic for 90 seconds.
        pipeline.addLast(new IdleStateHandler(90, 0, 0, TimeUnit.SECONDS));
        // 写文件内容
        pipeline.addLast(new ChunkedWriteHandler());
        // 聚合解码 HttpRequest/HttpContent/LastHttpContent 到 FullHttpRequest，保证接收的 Http 请求的完整性，防止类似mina出现多条同样请求
        pipeline.addLast(new HttpObjectAggregator(8192));
        // Browsers must originate from an explicitly configured official H5 domain.
        pipeline.addLast(new WebSocketOriginHandler());
        // ServerHandler owns the handshake and WebSocket frame lifecycle. Do
        // not install a second WebSocketServerProtocolHandler here: after the
        // manual upgrade it would still try to cast binary frames to
        // HttpObject and close every game connection.
        //编解码器
        pipeline.addLast(new MessageDecoder(1 << 20, 2, 2));
        pipeline.addLast(new MessageEncoder());
        //消息处理器
        pipeline.addLast(serverHandler);
    }

}
