package com.aoo.bcg.gateway;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** 统一判断权威快照是否仍处于“第一局从未开始”的等待态。未知结构必须 fail-closed。 */
public final class WaitingRoomExpirationPolicy {
    public static final Duration TIMEOUT=Duration.ofSeconds(300);
    public static final Duration STARTED_ROOM_INACTIVITY_TIMEOUT=Duration.ofHours(12);
    private WaitingRoomExpirationPolicy(){}
    public static boolean expired(Instant lastActivityAt,Instant now,Map<String,Object> state){
        if(now.isBefore(lastActivityAt.plus(TIMEOUT)))return false;
        if(number(state,"roundNo")>0||number(state,"setID")>0||number(state,"setId")>0)return false;
        Object started=state.get("started");if(Boolean.TRUE.equals(started))return false;
        String phase=text(state.get("phase"));
        if(!phase.isEmpty()&&!phase.equals("WAITING")&&!phase.equals("CREATED")&&!phase.equals("OPEN"))return false;
        if(Boolean.FALSE.equals(started)||phase.equals("WAITING")||phase.equals("CREATED")||phase.equals("OPEN"))return true;
        // Fail closed means "do not expire", not "abort the caller".  This policy is
        // also consulted synchronously by join/reconnect, so throwing here turns a
        // harmless legacy or partially restored snapshot into a Hall HTTP 500 and
        // locks every player out of an otherwise live room.  Durable route markers
        // remain authoritative for started rooms; unknown snapshots are retained
        // until a later authoritative save supplies an explicit lifecycle marker.
        return false;
    }
    public static boolean hasExplicitLifecycleMarker(Map<String,Object> state){
        return state.containsKey("roundNo")||state.containsKey("setID")||state.containsKey("setId")
                ||state.containsKey("started")||state.containsKey("phase");
    }
    public static boolean startedRoomInactive(Instant firstRoundStartedAt,Instant lastBusinessActivityAt,Instant now){
        if(firstRoundStartedAt==null||lastBusinessActivityAt==null)return false;
        return !now.isBefore(lastBusinessActivityAt.plus(STARTED_ROOM_INACTIVITY_TIMEOUT));
    }
    private static long number(Map<String,Object>s,String key){Object v=s.get(key);return v instanceof Number n?n.longValue():0L;}
    private static String text(Object value){return value==null?"":String.valueOf(value).strip().toUpperCase(java.util.Locale.ROOT);}
}
