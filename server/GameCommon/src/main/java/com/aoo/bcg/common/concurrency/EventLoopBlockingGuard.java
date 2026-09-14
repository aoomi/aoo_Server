package com.aoo.bcg.common.concurrency;
import java.util.concurrent.Callable;
/** Runtime assertion used by JDBC/file/synchronous-wait adapters before entering a blocking operation. */
public final class EventLoopBlockingGuard {
 private static final ThreadLocal<Boolean> EVENT_LOOP=ThreadLocal.withInitial(()->false);private EventLoopBlockingGuard(){}
 public static Runnable eventLoop(Runnable action){return()->{boolean old=EVENT_LOOP.get();EVENT_LOOP.set(true);try{action.run();}finally{EVENT_LOOP.set(old);}};}
 public static void assertBlockingAllowed(){String name=Thread.currentThread().getClass().getName();if(EVENT_LOOP.get()||name.contains("FastThreadLocalThread")||Thread.currentThread().getName().toLowerCase().contains("eventloop"))throw new IllegalStateException("blocking call on event loop");}
 public static <T>T blocking(Callable<T> action)throws Exception{assertBlockingAllowed();return action.call();}
}
