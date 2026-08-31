package com.aoo.bcg.gamespi.fsm;
import java.util.Map;import java.util.Objects;
/** Common orthogonal lifecycle dimensions shared by every game family. */
public record CompositeGameState(RoomLifecycleState room,RoundLifecycleState round,
 Map<Integer,PlayerLifecycleState> players,OperationWindowLifecycleState operationWindow,DissolveLifecycleState dissolve){
 public CompositeGameState{Objects.requireNonNull(room);Objects.requireNonNull(round);Objects.requireNonNull(operationWindow);Objects.requireNonNull(dissolve);players=Map.copyOf(players==null?Map.of():players);if(players.keySet().stream().anyMatch(seat->seat<0))throw new IllegalArgumentException("invalid seat");}
 public static CompositeGameState created(){return new CompositeGameState(RoomLifecycleState.CREATED,RoundLifecycleState.NONE,Map.of(),OperationWindowLifecycleState.CLOSED,DissolveLifecycleState.NONE);}
}
