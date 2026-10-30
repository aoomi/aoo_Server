package com.aoo.bcg.common.concurrency;
import com.aoo.bcg.common.retry.*;import java.time.Duration;import java.util.*;import java.util.concurrent.Callable;import java.util.concurrent.ThreadLocalRandom;import java.util.function.*;
/** Finite randomized retry boundary for lock and transaction contention only. */
public final class ContentionRetryExecutor{
 private final ControlledRetry retry;
 public ContentionRetryExecutor(){var policy=RetryPolicyCatalog.forDomain(RetryPolicyCatalog.Domain.DATABASE).policy();this.retry=new ControlledRetry(policy,duration->Thread.sleep(duration), (base,ratio)->{long spread=(long)(base*ratio);return spread==0?base:Math.max(1,base+ThreadLocalRandom.current().nextLong(-spread,spread+1));});}
 public<T>T execute(String idempotencyKey,Callable<T>operation,Predicate<Throwable>contention)throws Exception{return retry.execute(idempotencyKey,Objects.requireNonNull(operation),Objects.requireNonNull(contention));}
 public<T>T executeDiagnosed(String idempotencyKey,List<String>statements,List<String>lockObjects,List<String>businessKeys,Callable<T>operation,Consumer<DatabaseDeadlockDiagnostic>diagnosticSink)throws Exception{return execute(idempotencyKey,()->{try{return operation.call();}catch(Throwable failure){if(standardContention(failure))diagnosticSink.accept(DatabaseDeadlockDiagnostic.capture(idempotencyKey,statements,lockObjects,businessKeys,failure));if(failure instanceof Exception e)throw e;throw new RuntimeException(failure);}},ContentionRetryExecutor::standardContention);}
 public static boolean standardContention(Throwable failure){for(Throwable current=failure;current!=null;current=current.getCause()){if(current instanceof java.sql.SQLTransactionRollbackException)return true;String message=String.valueOf(current.getMessage()).toLowerCase();if(message.contains("deadlock")||message.contains("lock timeout")||message.contains("concurrent modification"))return true;}return false;}
}
