package com.ddm.server.http.server;

import com.ddm.server.common.CommLogD;
import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.StandardCharsets;


public class HttpRequest {

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private String requestBody = "";

    public HttpRequest(FullHttpRequest request) {
        this.initRequestBody(request);
    }

    private void initRequestBody(FullHttpRequest request) {
        try {
            int size = request.content().readableBytes();
            if (size > MAX_BODY_BYTES) {
                throw new IllegalArgumentException("HTTP request body exceeds 64KB");
            }
            requestBody = request.content().toString(StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            CommLogD.error("[HttpRequest]解析http协议body发生错误: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid HTTP request body", e);
        }
    }

    public String getRequestBody() {
        return requestBody;
    }
}
