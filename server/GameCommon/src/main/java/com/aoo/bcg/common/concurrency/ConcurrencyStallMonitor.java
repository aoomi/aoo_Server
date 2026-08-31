package com.aoo.bcg.common.concurrency;
import java.lang.management.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import java.util.function.*;
/** Production deadlock/stall detector emitting a bounded thread dump and a JFR trigger hook. */
public final class ConcurrencyStallMonitor implements AutoCloseable{
 public record Probe(String name,int queueDepth,Instant lastProgress,Map<String,Duration>heldLocks){}
 public record Alert(String kind,String subject,String threadDump,Instant observedAt){}
 private final ThreadMXBean threads;private final Supplier<Collection<Probe>>probes;private final Duration longHold,stagnation;private final Consumer<Alert>alertSink;private final Runnable jfrTrigger;private final ScheduledExecutorService scheduler;
 public ConcurrencyStallMonitor(Supplier<Collection<Probe>>probes,Duration longHold,Duration stagnation,Consumer<Alert>alertSink,Runnable jfrTrigger){this(ManagementFactory.getThreadMXBean(),probes,longHold,stagnation,alertSink,jfrTrigger);}
 ConcurrencyStallMonitor(ThreadMXBean threads,Supplier<Collection<Probe>>probes,Duration longHold,Duration stagnation,Consumer<Alert>alertSink,Runnable jfrTrigger){this.threads=Objects.requireNonNull(threads);this.probes=Objects.requireNonNull(probes);this.longHold=positive(longHold);this.stagnation=positive(stagnation);this.alertSink=Objects.requireNonNull(alertSink);this.jfrTrigger=Objects.requireNonNull(jfrTrigger);this.scheduler=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("aoo-stall-monitor").factory());}
 private static Duration positive(Duration d){if(d==null||d.isZero()||d.isNegative())throw new IllegalArgumentException("positive duration required");return d;}
 public void start(Duration interval){long ms=positive(interval).toMillis();scheduler.scheduleWithFixedDelay(this::safeSample,ms,ms,TimeUnit.MILLISECONDS);}
 public void sample(Instant now){long[]deadlocked=threads.findDeadlockedThreads();if(deadlocked!=null&&deadlocked.length>0)emit("thread-deadlock",Arrays.toString(deadlocked),now);for(Probe p:probes.get()){p.heldLocks().forEach((lock,age)->{if(age.compareTo(longHold)>0)emit("long-lock",p.name()+":"+lock+":"+age,now);});if(p.queueDepth()>0&&Duration.between(p.lastProgress(),now).compareTo(stagnation)>0)emit("queue-stagnation",p.name()+":depth="+p.queueDepth(),now);}}
 private void safeSample(){try{sample(Instant.now());}catch(Throwable failure){System.getLogger(ConcurrencyStallMonitor.class.getName()).log(System.Logger.Level.ERROR,"concurrency stall sample failed",failure);}}
 private void emit(String kind,String subject,Instant now){alertSink.accept(new Alert(kind,subject,dump(),now));jfrTrigger.run();}
 private String dump(){var out=new StringBuilder();for(ThreadInfo i:threads.dumpAllThreads(true,true)){if(out.length()>64*1024)break;out.append(i).append('\n');}return out.toString();}
 @Override public void close(){scheduler.shutdownNow();}
}
