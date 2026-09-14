package com.aoo.bcg.common.recovery;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Explicit product rule for which authoritative timers freeze during a controlled pause. */
public enum RoomTimerPausePolicy {
    MAINTENANCE(Set.of("operation","dissolveVote","settlementDisplay","continueDecision","autoReady","nextRound","interRound")),
    TOURNAMENT_PAUSE(Set.of("operation","settlementDisplay","continueDecision","autoReady","nextRound","interRound")),
    NO_FREEZE(Set.of());
    private final Set<String> frozenKinds;
    RoomTimerPausePolicy(Set<String> frozenKinds){this.frozenKinds=Set.copyOf(frozenKinds);}
    public RoomDeadlineSnapshot resume(RoomDeadlineSnapshot before,Instant pausedAt,Instant resumedAt){
        if(resumedAt.isBefore(pausedAt))throw new IllegalArgumentException("resume before pause");
        long shift=Duration.between(pausedAt,resumedAt).toMillis();Map<String,Long> result=new LinkedHashMap<>();
        before.deadlineEpochMillis().forEach((kind,value)->result.put(kind,value>0&&frozenKinds.contains(kind)?Math.addExact(value,shift):value));
        return new RoomDeadlineSnapshot(result);
    }
    public boolean freezes(String kind){return frozenKinds.contains(kind);}
}
