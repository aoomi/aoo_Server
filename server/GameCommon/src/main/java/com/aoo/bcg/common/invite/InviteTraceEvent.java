package com.aoo.bcg.common.invite;
import java.time.Instant;
/** Minimal invite funnel telemetry. It is deliberately absent from every admission context. */
public record InviteTraceEvent(String traceId,String inviteId,long roomId,Stage stage,String channel,String resultCode,Instant occurredAt){public enum Stage{CREATED,OPENED,VERIFIED,JOIN_ATTEMPTED,JOINED,FAILED}public InviteTraceEvent{if(traceId==null||traceId.isBlank()||inviteId==null||inviteId.isBlank()||roomId<=0||stage==null||channel==null||channel.isBlank()||resultCode==null||occurredAt==null)throw new IllegalArgumentException("invalid invite trace");if(channel.length()>32||resultCode.length()>64)throw new IllegalArgumentException("invite trace field too long");}}
