package com.aoo.bcg.gamespi.fsm;
import java.util.ArrayList;import java.util.List;
/** Cross-dimension invariants for room, round, player, operation-window and dissolve state. */
public final class CompositeStateValidator{
 public List<String> violations(CompositeGameState state){List<String>errors=new ArrayList<>();
  if(state.round()==RoundLifecycleState.OPERATING&&state.room()!=RoomLifecycleState.PLAYING)errors.add("OPERATING_REQUIRES_PLAYING_ROOM");
  if(state.operationWindow()==OperationWindowLifecycleState.OPEN&&state.round()!=RoundLifecycleState.OPERATING)errors.add("OPEN_WINDOW_REQUIRES_OPERATING_ROUND");
  if(state.dissolve()==DissolveLifecycleState.VOTING&&(state.room()==RoomLifecycleState.FINISHED||state.room()==RoomLifecycleState.DISSOLVED))errors.add("TERMINAL_ROOM_CANNOT_VOTE");
  if(state.room()==RoomLifecycleState.DISSOLVED&&(state.round()!=RoundLifecycleState.NONE&&state.round()!=RoundLifecycleState.ABORTED))errors.add("DISSOLVED_ROOM_REQUIRES_ABORTED_ROUND");
  if(state.room()==RoomLifecycleState.DISSOLVED&&state.operationWindow()!=OperationWindowLifecycleState.CLOSED)errors.add("DISSOLVED_ROOM_REQUIRES_CLOSED_WINDOW");
  if(state.players().containsValue(PlayerLifecycleState.PLAYING)&&state.room()!=RoomLifecycleState.PLAYING)errors.add("PLAYING_PLAYER_REQUIRES_PLAYING_ROOM");
  return List.copyOf(errors);}
 public void requireValid(CompositeGameState state){List<String>errors=violations(state);if(!errors.isEmpty())throw new IllegalStateException(String.join(",",errors));}
}
