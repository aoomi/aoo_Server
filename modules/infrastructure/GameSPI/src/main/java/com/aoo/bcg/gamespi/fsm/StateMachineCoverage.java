package com.aoo.bcg.gamespi.fsm;
import java.util.*;
/** Release gate for state, legal-edge and explicitly critical illegal-edge coverage. */
public final class StateMachineCoverage<S extends Enum<S>,E extends Enum<E>>{
 public record Edge<S,E>(S state,E event){}public record Report(double stateCoverage,double legalEdgeCoverage,double criticalIllegalEdgeCoverage){}
 private final Set<S>states;private final Set<Edge<S,E>>legal,criticalIllegal;private final Set<S>visitedStates=new HashSet<>();private final Set<Edge<S,E>>visitedLegal=new HashSet<>(),visitedIllegal=new HashSet<>();
 public StateMachineCoverage(Set<S>states,Set<Edge<S,E>>legal,Set<Edge<S,E>>criticalIllegal){this.states=Set.copyOf(states);this.legal=Set.copyOf(legal);this.criticalIllegal=Set.copyOf(criticalIllegal);if(!Collections.disjoint(this.legal,this.criticalIllegal))throw new IllegalArgumentException("edge cannot be both legal and illegal");}
 public void markState(S state){visitedStates.add(state);}public void markLegal(S state,E event){Edge<S,E>edge=new Edge<>(state,event);if(!legal.contains(edge))throw new IllegalArgumentException("not a declared legal edge");visitedLegal.add(edge);visitedStates.add(state);}public void markIllegal(S state,E event){Edge<S,E>edge=new Edge<>(state,event);if(!criticalIllegal.contains(edge))throw new IllegalArgumentException("not a critical illegal edge");visitedIllegal.add(edge);visitedStates.add(state);}
 public Report report(){return new Report(ratio(visitedStates,states),ratio(visitedLegal,legal),ratio(visitedIllegal,criticalIllegal));}
 public void requireReleaseThreshold(double state,double legalEdge,double criticalIllegalEdge){Report report=report();if(report.stateCoverage()<state||report.legalEdgeCoverage()<legalEdge||report.criticalIllegalEdgeCoverage()<criticalIllegalEdge)throw new IllegalStateException("state-machine coverage below release threshold: "+report);}
 private static double ratio(Set<?>visited,Set<?>all){return all.isEmpty()?1d:(double)visited.size()/all.size();}
}
