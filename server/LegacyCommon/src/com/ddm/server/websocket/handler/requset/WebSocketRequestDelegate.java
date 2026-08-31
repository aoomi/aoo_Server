package com.ddm.server.websocket.handler.requset;

import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;


public class WebSocketRequestDelegate extends WebSocketRequest {

    public long pid = 0;

    public WebSocketRequestDelegate() {
        super(null, null);
    }


    @Override
    public void response(Object protocol) {
        if (protocol == null) {
            protocol = "{}";
        }
        CommLogD.info("pid:{}, send response:{}", this.pid, new Gson().toJson(protocol));
    }

    // 空包
    @Override
    public void response() {
//        CommLogD.info("pid:{}, send response:{}", "{}");
    }

    @Override
    public void error(short errorcode, String format, Object... params) {
        String message = format;
        try {
            message = String.format(format, params);
        } catch (Exception e) {
            CommLogD.error("[WebSocketRequest]格式化错误字符串:{},时错误", e);
        }
        CommLogD.info("pid:{}, send error:{}", this.pid, errorcode);
    }

    @Override
    public void error(ErrorCode errorcode, String format, Object... params) {
        String message = format;
        try {
            message = String.format(format, params);
        } catch (Exception e) {
            CommLogD.error("[WebSocketRequest]格式化错误字符串:{},时错误", e);
        }
        CommLogD.info("pid:{}, send err:{}", this.pid, errorcode);
    }
}
