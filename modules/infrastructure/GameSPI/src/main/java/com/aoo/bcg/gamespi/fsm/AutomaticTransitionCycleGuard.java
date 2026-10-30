package com.aoo.bcg.gamespi.fsm;
import java.time.Duration;import java.util.*;
/** Rejects automatic state cycles whose total scheduling delay is zero. */
public final class AutomaticTransitionCycleGuard{
 public record Edge<S>(S source,S target,Duration minimumDelay,String action){public Edge{Objects.requireNonNull(source);Objects.requireNonNull(target);Objects.requireNonNull(minimumDelay);if(minimumDelay.isNegative()||action==null||action.isBlank())throw new IllegalArgumentException("invalid automatic transition");}}
 private AutomaticTransitionCycleGuard(){}
 public static <S>void requireNoZeroDelayCycle(Collection<Edge<S>> edges){Map<S,List<Edge<S>>>graph=new HashMap<>();for(var edge:edges)graph.computeIfAbsent(edge.source(),ignored->new ArrayList<>()).add(edge);for(S start:graph.keySet())visit(start,start,graph,new HashSet<>(),Duration.ZERO);}
 private static <S>void visit(S start,S state,Map<S,List<Edge<S>>>graph,Set<S>path,Duration delay){if(!path.add(state))return;for(var edge:graph.getOrDefault(state,List.of())){Duration next=delay.plus(edge.minimumDelay());if(edge.target().equals(start)&&next.isZero())throw new IllegalStateException("zero-delay automatic transition cycle at "+start);if(next.isZero())visit(start,edge.target(),graph,path,next);}path.remove(state);}
}
