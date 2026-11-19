package com.aoo.bcg.lobby;
import java.time.Duration;import java.util.Objects;import java.util.concurrent.*;
public class LobbyRoomCreateRecoveryService implements AutoCloseable{
 private final LobbyRoomCreateSaga saga;private final int batch;private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("hall-room-saga-recovery").factory());
 public LobbyRoomCreateRecoveryService(LobbyRoomCreateSaga saga,int batch){this.saga=Objects.requireNonNull(saga);this.batch=batch;}
 public LobbyRoomCreateRecoveryService start(Duration interval){long delay=Math.max(1,interval.toSeconds());worker.scheduleWithFixedDelay(this::recover,delay,delay,TimeUnit.SECONDS);return this;}
 private void recover(){try{var report=saga.recoverPending(batch);if(report.pending()>0)System.err.println("room saga recovery pending="+report.pending()+" failures="+report.failures());}catch(RuntimeException error){System.err.println("room saga recovery scan failed: "+error.getMessage());}}
 public void close(){worker.shutdownNow();}
}
