package com.aoo.bcg.common.recovery;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable snapshot schema for every independently recoverable room timer. */
public record RoomDeadlineSnapshot(Map<String, Long> deadlineEpochMillis) {
    public static final List<String> REQUIRED = java.util.Arrays.stream(RoomTimerKind.values()).map(RoomTimerKind::key).toList();
    public RoomDeadlineSnapshot {
        Map<String,Long> copy=new LinkedHashMap<>();
        for(String kind:REQUIRED) copy.put(kind,Math.max(0L,deadlineEpochMillis==null?0L:deadlineEpochMillis.getOrDefault(kind,0L)));
        deadlineEpochMillis=Map.copyOf(copy);
    }
    public static RoomDeadlineSnapshot from(AuthoritativeGameSession session) {
        long operation=session.operationDeadline().open()?session.operationDeadline().deadline().toEpochMilli():0L;
        return new RoomDeadlineSnapshot(Map.of("operation",operation));
    }
    public Map<String,Object> toMap(){return Map.of("deadlineEpochMillis",deadlineEpochMillis);}
    public static RoomDeadlineSnapshot fromState(Map<String,Object> state){
        Object raw=state.get("deadlines");if(!(raw instanceof Map<?,?> outer))return new RoomDeadlineSnapshot(Map.of());
        Object values=outer.get("deadlineEpochMillis");if(!(values instanceof Map<?,?> map))return new RoomDeadlineSnapshot(Map.of());
        Map<String,Long> parsed=new LinkedHashMap<>();map.forEach((k,v)->{if(v instanceof Number n)parsed.put(String.valueOf(k),n.longValue());});return new RoomDeadlineSnapshot(parsed);
    }
    public List<String> expiredAt(Instant now){long epoch=now.toEpochMilli();return REQUIRED.stream().filter(kind->{long value=deadlineEpochMillis.get(kind);return value>0&&value<=epoch;}).toList();}
}
