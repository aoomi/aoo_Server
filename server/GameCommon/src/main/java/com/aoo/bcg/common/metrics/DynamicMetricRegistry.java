package com.aoo.bcg.common.metrics;

import java.util.Map;import java.util.Set;import java.util.TreeMap;import java.util.concurrent.ConcurrentHashMap;import java.util.concurrent.atomic.AtomicBoolean;import java.util.concurrent.atomic.AtomicInteger;

/** Lifecycle and cardinality gate for dynamically registered room/play metrics. */
public final class DynamicMetricRegistry{
 private static final Set<String> FORBIDDEN_TAGS=Set.of("roomId","accountId","userId","requestId","sessionId");
 private final int maximumSeries;private final ConcurrentHashMap<Key,AtomicInteger> references=new ConcurrentHashMap<>();
 private record Key(String name,Map<String,String> tags){}
 public DynamicMetricRegistry(int maximumSeries){if(maximumSeries<1)throw new IllegalArgumentException("maximum series must be positive");this.maximumSeries=maximumSeries;}
 public synchronized Handle register(String name,Map<String,String> tags){if(name==null||!name.matches("[a-z][a-z0-9_.-]+"))throw new IllegalArgumentException("invalid metric name");if(tags.keySet().stream().anyMatch(FORBIDDEN_TAGS::contains))throw new IllegalArgumentException("high-cardinality metric tag forbidden");Map<String,String> canonical=Map.copyOf(new TreeMap<>(tags));Key key=new Key(name,canonical);AtomicInteger existing=references.get(key);if(existing==null&&references.size()>=maximumSeries)throw new IllegalStateException("metric series cardinality limit reached");references.computeIfAbsent(key,ignored->new AtomicInteger()).incrementAndGet();return new Handle(key);}
 public int seriesCount(){return references.size();}
 public final class Handle implements AutoCloseable{private final Key key;private final AtomicBoolean closed=new AtomicBoolean();private Handle(Key key){this.key=key;}@Override public void close(){if(!closed.compareAndSet(false,true))return;synchronized(DynamicMetricRegistry.this){references.computeIfPresent(key,(ignored,count)->count.decrementAndGet()==0?null:count);}}}
}
