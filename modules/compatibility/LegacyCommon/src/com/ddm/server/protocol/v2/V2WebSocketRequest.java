package com.ddm.server.protocol.v2;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;

/** Wraps legacy handler responses without exposing V1 to business callers. */
public final class V2WebSocketRequest extends WebSocketRequest {
    private final WebSocketRequest delegate;
    private final ProtocolEnvelope inbound;
    private final long accountId;

    public V2WebSocketRequest(WebSocketRequest delegate, ProtocolEnvelope inbound, long accountId) {
        super(delegate.getSession(), delegate.getHeader());
        this.delegate = delegate;
        this.inbound = inbound;
        this.accountId = accountId;
    }

    @Override
    public void response(Object protocol) {
        ProtocolEnvelope response = success(protocol);
        complete(response);
        delegate.response(response);
    }

    @Override
    public void response(Object protocol, String subjectTopic) {
        ProtocolEnvelope response = success(protocol);
        complete(response);
        delegate.response(response, subjectTopic);
    }

    @Override
    public void response() {
        ProtocolEnvelope response = success(new Object());
        complete(response);
        delegate.response(response);
    }

    @Override
    public void error(short errorCode, String format, Object... params) {
        ProtocolEnvelope response = failure((int) errorCode, format, params);
        complete(response);
        delegate.response(response);
    }

    @Override
    public void error(ErrorCode errorCode, String format, Object... params) {
        ProtocolEnvelope response = failure((int) errorCode.value(), format, params);
        complete(response);
        delegate.response(response);
    }

    private ProtocolEnvelope success(Object body) {
        ProtocolEnvelope value = base();
        value.code = 0;
        value.message = "success";
        value.body = body == null ? new Object() : body;
        return value;
    }

    private void complete(ProtocolEnvelope response) {
        RedisProtocolIdempotency.complete(accountId, inbound.msgId, inbound.roomId,
                inbound.roundNo, inbound.requestId, response);
    }

    private ProtocolEnvelope failure(int code, String format, Object... params) {
        ProtocolEnvelope value = base();
        value.code = code;
        try { value.message = String.format(format, params); }
        catch (RuntimeException ignored) { value.message = format; }
        value.body = new Object();
        return value;
    }

    private ProtocolEnvelope base() {
        ProtocolEnvelope value = new ProtocolEnvelope();
        value.msgId = inbound.msgId;
        value.kind = "resp";
        value.requestId = inbound.requestId;
        value.seq = inbound.seq;
        value.timestamp = System.currentTimeMillis();
        value.traceId = inbound.traceId;
        value.roomId = inbound.roomId;
        value.roundNo = inbound.roundNo;
        value.playVersion = inbound.playVersion;
        return value;
    }
}
