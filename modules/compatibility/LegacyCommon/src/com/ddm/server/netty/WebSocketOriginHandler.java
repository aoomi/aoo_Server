package com.ddm.server.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Exact browser Origin allow-list. Native clients without Origin remain supported. */
public final class WebSocketOriginHandler extends ChannelInboundHandlerAdapter {
    private final Set<String> allowedOrigins;

    public WebSocketOriginHandler() {
        String configured = System.getProperty("AllowedWebSocketOrigins",
                System.getenv().getOrDefault("ALLOWED_WEBSOCKET_ORIGINS", ""));
        allowedOrigins = Arrays.stream(configured.split(","))
                .map(WebSocketOriginHandler::normalize)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest request) {
            String origin = request.headers().get(HttpHeaderNames.ORIGIN);
            if (origin != null && !allowedOrigins.contains(normalize(origin))) {
                context.writeAndFlush(new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN, Unpooled.EMPTY_BUFFER))
                        .addListener(future -> context.close());
                request.release();
                return;
            }
        }
        context.fireChannelRead(message);
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
