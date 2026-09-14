package com.aoo.bcg.gateway;

import java.util.ArrayList;import java.util.EnumMap;import java.util.EnumSet;import java.util.Map;import java.util.concurrent.atomic.AtomicBoolean;

/** Owns every resource tied to one authenticated transport connection. */
public final class ConnectionResourceScope implements AutoCloseable{
 public enum Kind{CHANNEL,SESSION,BUFFER,SUBSCRIPTION,HEARTBEAT_TASK}
 private final String connectionId;private final EnumMap<Kind,AutoCloseable> resources=new EnumMap<>(Kind.class);private final AtomicBoolean sealed=new AtomicBoolean(),closed=new AtomicBoolean();
 public ConnectionResourceScope(String connectionId){if(connectionId==null||connectionId.isBlank())throw new IllegalArgumentException("connectionId required");this.connectionId=connectionId;}
 public synchronized void own(Kind kind,AutoCloseable resource){if(sealed.get()||closed.get())throw new IllegalStateException("connection scope sealed");if(resources.putIfAbsent(kind,resource)!=null)throw new IllegalArgumentException("duplicate connection resource: "+kind);}
 public synchronized void activate(){EnumSet<Kind> missing=EnumSet.allOf(Kind.class);missing.removeAll(resources.keySet());if(!missing.isEmpty())throw new IllegalStateException("connection resource ownership incomplete: "+missing);sealed.set(true);}
 @Override public synchronized void close(){if(!closed.compareAndSet(false,true))return;var failures=new ArrayList<Throwable>();Kind[] order={Kind.HEARTBEAT_TASK,Kind.SUBSCRIPTION,Kind.BUFFER,Kind.SESSION,Kind.CHANNEL};for(Kind kind:order){AutoCloseable resource=resources.remove(kind);if(resource!=null)try{resource.close();}catch(Throwable failure){failures.add(failure);}}if(!failures.isEmpty()){var failure=new IllegalStateException("connection resource release failed: "+connectionId);failures.forEach(failure::addSuppressed);throw failure;}}
 public synchronized Map<Kind,AutoCloseable> snapshot(){return Map.copyOf(resources);}
}
