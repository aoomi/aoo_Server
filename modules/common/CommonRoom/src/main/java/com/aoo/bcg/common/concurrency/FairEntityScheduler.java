package com.aoo.bcg.common.concurrency;
import java.time.*;import java.util.*;import java.util.concurrent.*;import java.util.function.Consumer;
/** Round-robin scheduler: one hot room/user cannot occupy consecutive dispatch slots while peers wait. */
public final class FairEntityScheduler<K,T>{
 public record Entry<K,T>(K key,T value,Instant enqueuedAt){}
 private final Map<K,ArrayDeque<Entry<K,T>>>queues=new LinkedHashMap<>();private final ArrayDeque<K>ready=new ArrayDeque<>();private final int perEntity,total;private final Duration latencyBudget;private int size;
 public FairEntityScheduler(int perEntity,int total,Duration latencyBudget){if(perEntity<1||total<1||perEntity>total||latencyBudget==null||latencyBudget.isNegative()||latencyBudget.isZero())throw new IllegalArgumentException("valid budgets required");this.perEntity=perEntity;this.total=total;this.latencyBudget=latencyBudget;}
 public synchronized void offer(K key,T value,Instant now){Objects.requireNonNull(key);Objects.requireNonNull(now);var q=queues.computeIfAbsent(key,k->new ArrayDeque<>());if(q.size()>=perEntity||size>=total)throw new RejectedExecutionException("fair queue capacity exceeded for "+key);if(q.isEmpty())ready.addLast(key);q.addLast(new Entry<>(key,value,now));size++;}
 public synchronized Optional<Entry<K,T>>poll(Instant now,Consumer<Entry<K,T>>lateSink){Objects.requireNonNull(now);Objects.requireNonNull(lateSink);K key=ready.pollFirst();if(key==null)return Optional.empty();var q=queues.get(key);var entry=q.removeFirst();size--;if(q.isEmpty())queues.remove(key);else ready.addLast(key);if(Duration.between(entry.enqueuedAt(),now).compareTo(latencyBudget)>0)lateSink.accept(entry);return Optional.of(entry);}
 public synchronized int size(){return size;}
}
