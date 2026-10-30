package com.aoo.bcg.gamespi.resource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit room ownership ledger; release is reverse-order, complete and idempotent. */
public final class RoomResourceRegistry implements AutoCloseable {
    public enum Kind { PLAYERS, EVENT_STREAM, SNAPSHOT, SCHEDULED_TASKS, LOCKS, LISTENERS, CACHES, METRICS }
    public record OwnedResource(Kind kind,String owner,String identity,AutoCloseable release) {
        public OwnedResource { if(kind==null||owner==null||owner.isBlank()||identity==null||identity.isBlank()||release==null)throw new IllegalArgumentException("invalid room resource ownership"); }
    }
    private final long roomId;private final List<OwnedResource> resources=new ArrayList<>();private final AtomicBoolean active=new AtomicBoolean();private final AtomicBoolean closed=new AtomicBoolean();
    public RoomResourceRegistry(long roomId){if(roomId<=0)throw new IllegalArgumentException("roomId must be positive");this.roomId=roomId;}
    public synchronized void register(OwnedResource resource){if(active.get()||closed.get())throw new IllegalStateException("room resource registry is sealed");if(resources.stream().anyMatch(value->value.kind()==resource.kind()&&value.identity().equals(resource.identity())))throw new IllegalArgumentException("duplicate room resource");resources.add(resource);}
    public synchronized void activate(){EnumSet<Kind> present=EnumSet.noneOf(Kind.class);resources.forEach(value->present.add(value.kind()));EnumSet<Kind> missing=EnumSet.allOf(Kind.class);missing.removeAll(present);if(!missing.isEmpty())throw new IllegalStateException("room resource ownership incomplete: "+missing);active.set(true);}
    public synchronized List<OwnedResource> snapshot(){return List.copyOf(resources);}
    @Override public synchronized void close(){if(!closed.compareAndSet(false,true))return;List<Throwable> failures=new ArrayList<>();for(int index=resources.size()-1;index>=0;index--)try{resources.get(index).release().close();}catch(Throwable failure){failures.add(failure);}resources.clear();if(!failures.isEmpty()){var failure=new IllegalStateException("room resource release failed: "+roomId);failures.forEach(failure::addSuppressed);throw failure;}}
}
