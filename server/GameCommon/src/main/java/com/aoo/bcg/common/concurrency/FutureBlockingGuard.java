package com.aoo.bcg.common.concurrency;
import java.util.concurrent.*;
/** Marks executor workers and rejects synchronous waits that can exhaust their own pool. */
public final class FutureBlockingGuard {
 private static final ThreadLocal<String> WORKER=new ThreadLocal<>();private FutureBlockingGuard(){}
 public static Runnable worker(String pool,Runnable task){return()->{String old=WORKER.get();WORKER.set(pool);try{task.run();}finally{if(old==null)WORKER.remove();else WORKER.set(old);}};}
 public static <T>T await(Future<T>future,long timeout,TimeUnit unit)throws InterruptedException,ExecutionException,TimeoutException{String pool=WORKER.get();if(pool!=null)throw new IllegalStateException("synchronous Future wait forbidden inside pool "+pool);return future.get(timeout,unit);}
}
