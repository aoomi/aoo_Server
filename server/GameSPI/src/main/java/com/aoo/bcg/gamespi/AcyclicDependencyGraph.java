package com.aoo.bcg.gamespi;
import java.util.*;
/** Shared cycle/missing-edge guard for components, regional inheritance, templates and serializers. */
public final class AcyclicDependencyGraph<K>{
 public record Node<K>(K key,Set<K> dependencies){public Node{Objects.requireNonNull(key);dependencies=Set.copyOf(dependencies==null?Set.of():dependencies);}}
 private final Map<K,Node<K>>nodes;
 public AcyclicDependencyGraph(Collection<Node<K>> values){Map<K,Node<K>>copy=new LinkedHashMap<>();for(var node:values)if(copy.putIfAbsent(node.key(),node)!=null)throw new IllegalArgumentException("duplicate dependency node: "+node.key());for(var node:copy.values())for(K dependency:node.dependencies())if(!copy.containsKey(dependency))throw new IllegalArgumentException("missing dependency: "+dependency);nodes=Map.copyOf(copy);requireAcyclic();}
 public List<K> topologicalOrder(){Map<K,Integer>degree=new HashMap<>();Map<K,List<K>>dependents=new HashMap<>();nodes.forEach((key,node)->{degree.put(key,node.dependencies().size());node.dependencies().forEach(dependency->dependents.computeIfAbsent(dependency,ignored->new ArrayList<>()).add(key));});ArrayDeque<K>ready=new ArrayDeque<>();degree.forEach((key,value)->{if(value==0)ready.add(key);});List<K>order=new ArrayList<>();while(!ready.isEmpty()){K key=ready.remove();order.add(key);for(K dependent:dependents.getOrDefault(key,List.of()))if(degree.compute(dependent,(ignored,value)->value-1)==0)ready.add(dependent);}if(order.size()!=nodes.size())throw new IllegalStateException("dependency cycle detected");return List.copyOf(order);}
 private void requireAcyclic(){topologicalOrder();}
}
