package com.aoo.bcg.gamespi.fsm;
import java.util.*;
/** Exhaustively enumerates short declared-event sequences and reports reachable non-terminal deadlocks. */
public final class BoundedStateModelChecker<S extends Enum<S>,E extends Enum<E>>{
 public record Counterexample<S,E>(S state,List<E> eventSequence,String violation){}
 public List<Counterexample<S,E>> check(S initial,Collection<StateTransition<S,E>>edges,Set<S>terminal,int maxDepth){if(maxDepth<0||maxDepth>100)throw new IllegalArgumentException("invalid model depth");record Node<S,E>(S state,List<E>path){}ArrayDeque<Node<S,E>>queue=new ArrayDeque<>();queue.add(new Node<>(initial,List.of()));List<Counterexample<S,E>>issues=new ArrayList<>();Set<String>seen=new HashSet<>();while(!queue.isEmpty()){var node=queue.remove();if(!seen.add(node.state()+":"+node.path().size()))continue;var outgoing=edges.stream().filter(edge->edge.source()==node.state()).toList();if(outgoing.isEmpty()&&!terminal.contains(node.state()))issues.add(new Counterexample<>(node.state(),node.path(),"NON_TERMINAL_DEADLOCK"));if(node.path().size()>=maxDepth)continue;for(var edge:outgoing){List<E>path=new ArrayList<>(node.path());path.add(edge.event());queue.add(new Node<>(edge.target(),List.copyOf(path)));}}return List.copyOf(issues);}
}
