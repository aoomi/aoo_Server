package com.aoo.bcg.gamespi.fsm;
import com.aoo.bcg.gamespi.time.OperationDeadline;import java.util.*;
/** Detects states that cannot make progress before they enter production. */
public final class LivenessValidator{
 public List<String> violations(CompositeGameState state,OperationDeadline deadline,Collection<StateTransition<RoomLifecycleState,?>> roomEdges,Set<RoomLifecycleState> terminal){List<String>errors=new ArrayList<>();
  if(state.round()==RoundLifecycleState.OPERATING){if(state.operationWindow()!=OperationWindowLifecycleState.OPEN)errors.add("OPERATING_WITHOUT_WINDOW");if(!deadline.open())errors.add("OPEN_OPERATION_WITHOUT_DEADLINE");if(deadline.open()&&!state.players().containsKey(deadline.seatId()))errors.add("OPERATION_SEAT_MISSING");}
  if(!canReachTerminal(state.room(),roomEdges,terminal))errors.add("NO_TERMINAL_PATH");return List.copyOf(errors);}
 private boolean canReachTerminal(RoomLifecycleState start,Collection<StateTransition<RoomLifecycleState,?>>edges,Set<RoomLifecycleState>terminal){Set<RoomLifecycleState>seen=EnumSet.noneOf(RoomLifecycleState.class);ArrayDeque<RoomLifecycleState>queue=new ArrayDeque<>();queue.add(start);while(!queue.isEmpty()){var current=queue.remove();if(!seen.add(current))continue;if(terminal.contains(current))return true;edges.stream().filter(edge->edge.source()==current).map(StateTransition::target).forEach(queue::add);}return false;}
}
