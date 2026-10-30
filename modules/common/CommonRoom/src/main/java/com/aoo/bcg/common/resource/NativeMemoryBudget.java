package com.aoo.bcg.common.resource;

import java.util.EnumMap;import java.util.Map;import java.util.concurrent.atomic.AtomicBoolean;

/** Unified admission budget for memory outside the managed Java heap. */
public final class NativeMemoryBudget{
 public enum Category{DIRECT_BUFFER,COMPRESSION,TLS,NATIVE_SDK}
 public record Usage(long currentBytes,long highWatermarkBytes,long limitBytes){}
 private final EnumMap<Category,Long> limits=new EnumMap<>(Category.class),current=new EnumMap<>(Category.class),high=new EnumMap<>(Category.class);private final long totalLimit;private long total;
 public NativeMemoryBudget(Map<Category,Long> limits,long totalLimit){if(limits.keySet().size()!=Category.values().length||totalLimit<=0||limits.values().stream().anyMatch(value->value==null||value<=0))throw new IllegalArgumentException("complete positive native memory limits required");this.limits.putAll(limits);this.totalLimit=totalLimit;for(var category:Category.values()){current.put(category,0L);high.put(category,0L);}}
 public synchronized Reservation reserve(Category category,long bytes){if(bytes<=0)throw new IllegalArgumentException("native reservation must be positive");long next=Math.addExact(current.get(category),bytes),nextTotal=Math.addExact(total,bytes);if(next>limits.get(category)||nextTotal>totalLimit)throw new IllegalStateException("native memory budget exceeded: "+category);current.put(category,next);high.put(category,Math.max(high.get(category),next));total=nextTotal;return new Reservation(category,bytes);}
 public synchronized Map<Category,Usage> snapshot(){var values=new EnumMap<Category,Usage>(Category.class);for(var category:Category.values())values.put(category,new Usage(current.get(category),high.get(category),limits.get(category)));return Map.copyOf(values);}
 public synchronized long totalBytes(){return total;}
 public final class Reservation implements AutoCloseable{private final Category category;private final long bytes;private final AtomicBoolean closed=new AtomicBoolean();private Reservation(Category category,long bytes){this.category=category;this.bytes=bytes;}@Override public void close(){if(!closed.compareAndSet(false,true))return;synchronized(NativeMemoryBudget.this){current.put(category,Math.subtractExact(current.get(category),bytes));total=Math.subtractExact(total,bytes);}}}
}
