package com.ddm.server.http.server;

import com.ddm.server.common.CommLogD;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private final ChannelHandlerContext context;
    private final String requestUri;

    public HttpResponse(ChannelHandlerContext context, String requestUri) {
        this.context = context;
        this.requestUri = requestUri;
    }

    public void response(String result) {
        response(200, result);
    }

    public void response(int code, String result) {
        try {
            byte[] content = result.getBytes(StandardCharsets.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(code), Unpooled.wrappedBuffer(content));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json;charset=UTF-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, content.length);
            context.writeAndFlush(response).addListener(future -> context.close());
        } catch (Exception e) {
            CommLogD.error("回写Http数据response时发生错误", e);
        }
    }


    public void error(int code, String format, Object... param) {
        String msg = String.format(format, param);
        String rep = String.format("{\"Code\":%d,\"Msg\":\"%s\"}", code, this.encodeString(msg));
        this.response(rep);
        CommLogD.error("{}请求处理失败,错误码:{},msg:{}", requestUri, code, msg);
    }

    private String encodeString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default: {
                    sb.append(ch);
                    break;
                }
            }
        }
        return sb.toString();
    }
}
