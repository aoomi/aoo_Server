package com.aoo.bcg.gateway;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/** 统一构造客户端可严格校验的 WebSocket V2 服务端信封。 */
final class GatewayEnvelopeFactory {
    private GatewayEnvelopeFactory() {}

    static Map<String,Object> push(String msgId,String requestId,long sequence,String traceId,
                                   String action,Object payload,Clock clock) {
        if (blank(msgId)||blank(requestId)||blank(traceId)||blank(action)||sequence<=0) {
            throw new IllegalArgumentException("invalid V2 push envelope");
        }
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("action",action);
        body.put("payload",payload);
        Map<String,Object> envelope=new LinkedHashMap<>();
        envelope.put("protocolVersion","2.0");
        envelope.put("msgId",msgId);
        envelope.put("kind","push");
        envelope.put("requestId",requestId);
        envelope.put("seq",sequence);
        envelope.put("timestamp",Math.max(1L,clock.millis()));
        envelope.put("traceId",traceId);
        envelope.put("body",body);
        return envelope;
    }

    private static boolean blank(String value){return value==null||value.isBlank();}
}
