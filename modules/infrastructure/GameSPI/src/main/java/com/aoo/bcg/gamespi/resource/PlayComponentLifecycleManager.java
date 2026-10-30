package com.aoo.bcg.gamespi.resource;

import java.util.ArrayList;import java.util.HashSet;import java.util.List;import java.util.Set;import java.util.concurrent.atomic.AtomicBoolean;

/** Initializes in dependency order and always destroys in reverse order, including partial failures. */
public final class PlayComponentLifecycleManager implements AutoCloseable{
 private final List<ManagedPlayComponent> components;private final List<ManagedPlayComponent> initialized=new ArrayList<>();private final AtomicBoolean closed=new AtomicBoolean();
 public PlayComponentLifecycleManager(List<ManagedPlayComponent> components){this.components=List.copyOf(components);Set<String> ids=new HashSet<>();for(var component:this.components)if(!ids.add(component.componentId()))throw new IllegalArgumentException("duplicate play component: "+component.componentId());}
 public synchronized void initializeAndStart(){if(!initialized.isEmpty()||closed.get())throw new IllegalStateException("play component lifecycle already used");try{for(var component:components){component.initialize();initialized.add(component);}for(var component:components)component.start();}catch(Throwable failure){try{close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}}
 public synchronized void resetBetweenRounds(){if(closed.get()||initialized.size()!=components.size())throw new IllegalStateException("play components are not active");for(var component:components)component.resetBetweenRounds();}
 @Override public synchronized void close(){if(!closed.compareAndSet(false,true))return;var failures=new ArrayList<Throwable>();for(int index=initialized.size()-1;index>=0;index--)try{initialized.get(index).destroy();}catch(Throwable failure){failures.add(failure);}initialized.clear();if(!failures.isEmpty()){var failure=new IllegalStateException("play component destruction failed");failures.forEach(failure::addSuppressed);throw failure;}}
}
