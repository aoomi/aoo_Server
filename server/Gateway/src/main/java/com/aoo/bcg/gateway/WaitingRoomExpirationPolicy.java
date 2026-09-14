package com.aoo.bcg.gateway;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** 统一判断权威快照是否仍处于“第一局从未开始”的等待态。未知结构必须 fail-closed。 */
public final class WaitingRoomExpirationPolicy {
    public static final Duration TIMEOUT=Duration.ofSeconds(300);
    private WaitingRoomExpirationPolicy(){}
    public static boolean expired(Instant createdAt,Instant now,Map<String,Object> state){
        if(now.isBefore(createdAt.plus(TIMEOUT)))return false;
        if(number(state,"roundNo")>0||number(state,"setID")>0||number(state,"setId")>0)return false;
        Object started=state.get("started");if(Boolean.TRUE.equals(started))return false;
        String phase=text(state.get("phase"));
        if(!phase.isEmpty()&&!phase.equals("WAITING")&&!phase.equals("CREATED")&&!phase.equals("OPEN"))return false;
        if(Boolean.FALSE.equals(started)||phase.equals("WAITING")||phase.equals("CREATED")||phase.equals("OPEN"))return true;
        // 首局是否曾开始由 aoo_room_authority_route.first_round_started_at 永久裁决；
        // 玩法快照没有等待态字段时，不能让公共层超时任务永久悬挂。
        return true;
    }
    private static long number(Map<String,Object>s,String key){Object v=s.get(key);return v instanceof Number n?n.longValue():0L;}
    private static String text(Object value){return value==null?"":String.valueOf(value).strip().toUpperCase(java.util.Locale.ROOT);}
}
