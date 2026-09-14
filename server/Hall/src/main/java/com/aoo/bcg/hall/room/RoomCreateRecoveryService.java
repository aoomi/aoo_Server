package com.aoo.bcg.hall.room;
import java.time.Duration;import java.util.Objects;import java.util.concurrent.*;
public final class RoomCreateRecoveryService implements AutoCloseable{
 private final RoomCreateSaga saga;private final int batch;private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("hall-room-saga-recovery").factory());
 public RoomCreateRecoveryService(RoomCreateSaga saga,int batch){this.saga=Objects.requireNonNull(saga);this.batch=batch;}
 public RoomCreateRecoveryService start(Duration interval){long delay=Math.max(1,interval.toSeconds());worker.scheduleWithFixedDelay(this::recover,delay,delay,TimeUnit.SECONDS);return this;}
 private void recover(){try{var report=saga.recoverPending(batch);if(report.pending()>0)System.err.println("room saga recovery pending="+report.pending()+" failures="+report.failures());}catch(RuntimeException error){System.err.println("room saga recovery scan failed: "+error.getMessage());}}
 public void close(){worker.shutdownNow();}
}
