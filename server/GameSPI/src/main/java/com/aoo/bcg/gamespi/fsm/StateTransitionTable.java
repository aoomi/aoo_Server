package com.aoo.bcg.gamespi.fsm;
import java.util.*;import java.util.function.Predicate;
/** Declarative transition table: every edge names its guard and side-effect contract. */
public final class StateTransitionTable<S extends Enum<S>,E extends Enum<E>>{
 public record TransitionResult<S>(boolean accepted,S state,String code){public static <S>TransitionResult<S>accepted(S state){return new TransitionResult<>(true,state,"OK");}public static <S>TransitionResult<S>rejected(S state,String code){return new TransitionResult<>(false,state,code);}}
 public record Key<S,E>(S state,E event){}
 private final Map<Key<S,E>,StateTransition<S,E>>edges;
 private final Set<S>terminalStates;
 public StateTransitionTable(Collection<StateTransition<S,E>> transitions){this(transitions,Set.of());}
 public StateTransitionTable(Collection<StateTransition<S,E>> transitions,Set<S> terminalStates){this.terminalStates=Set.copyOf(terminalStates);Map<Key<S,E>,StateTransition<S,E>>copy=new LinkedHashMap<>();for(var edge:transitions){if(this.terminalStates.contains(edge.source()))throw new IllegalArgumentException("terminal state cannot have outgoing edge");if(copy.putIfAbsent(new Key<>(edge.source(),edge.event()),edge)!=null)throw new IllegalArgumentException("duplicate transition");}edges=Map.copyOf(copy);}
 public StateTransition<S,E> require(S state,E event){var edge=edges.get(new Key<>(state,event));if(edge==null)throw new IllegalStateException("undeclared transition: "+state+" + "+event);return edge;}
 public S transition(S state,E event,Predicate<String> guard,java.util.function.Consumer<String> sideEffect){var edge=require(state,event);if(!guard.test(edge.guardName()))throw new IllegalStateException("transition guard rejected: "+edge.guardName());sideEffect.accept(edge.sideEffectName());return edge.target();}
 public TransitionResult<S> tryTransition(S state,E event,Predicate<String> guard,java.util.function.Consumer<String> sideEffect){if(terminalStates.contains(state))return TransitionResult.rejected(state,"TERMINAL_STATE");var edge=edges.get(new Key<>(state,event));if(edge==null)return TransitionResult.rejected(state,"UNDECLARED_EVENT");if(!guard.test(edge.guardName()))return TransitionResult.rejected(state,"GUARD_REJECTED");sideEffect.accept(edge.sideEffectName());return TransitionResult.accepted(edge.target());}
 public Collection<StateTransition<S,E>> edges(){return edges.values();}
}
