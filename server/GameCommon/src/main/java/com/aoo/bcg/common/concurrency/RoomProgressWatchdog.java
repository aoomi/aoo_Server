package com.aoo.bcg.common.concurrency;
import java.time.*;import java.util.*;import java.util.concurrent.ConcurrentHashMap;import java.util.function.Consumer;
/** Detects an authoritative operation seat whose event sequence stops advancing. */
public final class RoomProgressWatchdog{
 public record State(long roomId,long roundNo,Integer operationSeat,long eventSequence,Instant observedAt,String phase){}
 public record StuckRoom(State current,State lastProgress,Duration stalledFor){}
 private final Duration budget;private final Consumer<StuckRoom>sink;private final Map<Long,State>progress=new ConcurrentHashMap<>();private final Set<Long>alerted=ConcurrentHashMap.newKeySet();
 public RoomProgressWatchdog(Duration budget,Consumer<StuckRoom>sink){if(budget==null||budget.isZero()||budget.isNegative())throw new IllegalArgumentException("positive stall budget required");this.budget=budget;this.sink=Objects.requireNonNull(sink);}
 public void observe(State state){Objects.requireNonNull(state);State old=progress.get(state.roomId());if(state.operationSeat()==null){progress.remove(state.roomId());alerted.remove(state.roomId());return;}if(old==null||old.roundNo()!=state.roundNo()||old.eventSequence()!=state.eventSequence()){progress.put(state.roomId(),state);alerted.remove(state.roomId());return;}Duration stalled=Duration.between(old.observedAt(),state.observedAt());if(stalled.compareTo(budget)>0&&alerted.add(state.roomId()))sink.accept(new StuckRoom(state,old,stalled));}
 public void roomClosed(long roomId){progress.remove(roomId);alerted.remove(roomId);}
}
