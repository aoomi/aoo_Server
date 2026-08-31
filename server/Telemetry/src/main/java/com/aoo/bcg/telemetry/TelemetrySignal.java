package com.aoo.bcg.telemetry;
import java.time.Instant;import java.util.Map;
public record TelemetrySignal(String eventId,Kind kind,Instant occurredAt,String sessionId,Long roomId,String deviceId,String ipAddress,Double latitude,Double longitude,Map<String,Object> attributes){public enum Kind{PERFORMANCE,CRASH,NETWORK,DEVICE,LOCATION,OPERATION}}
