package com.ddm.server.protocol.v2;

public final class ProtocolEnvelope {
    public String protocolVersion = "2.0";
    public String msgId;
    public String kind;
    public String requestId;
    public long seq;
    public long timestamp;
    public String traceId;
    public String roomId;
    public Integer roundNo;
    public String playVersion;
    public Integer code;
    public String message;
    public String wsTicket;
    public Object body;
}
