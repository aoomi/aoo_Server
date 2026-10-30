package com.aoo.bcg.common.repair;
import java.util.Objects;import java.util.Optional;import java.util.concurrent.Callable;
/** Replays the immutable outcome of an already completed repair operation. */
public final class IdempotentRepairExecutor<R>{
 public enum Claim{ACQUIRED,IN_PROGRESS,COMPLETED}
 public interface Store<R>{Claim claim(String operationId,String inputHash);Optional<R> completed(String operationId,String inputHash);void complete(String operationId,String inputHash,R result);void fail(String operationId,String inputHash,String reason);}
 private final Store<R>store;public IdempotentRepairExecutor(Store<R>store){this.store=Objects.requireNonNull(store);}
 public R execute(String operationId,String inputHash,Callable<R>operation)throws Exception{if(operationId==null||operationId.isBlank()||inputHash==null||inputHash.isBlank())throw new IllegalArgumentException("stable operation id and input hash required");Claim claim=store.claim(operationId,inputHash);if(claim==Claim.COMPLETED)return store.completed(operationId,inputHash).orElseThrow(()->new IllegalStateException("completed repair lost result"));if(claim==Claim.IN_PROGRESS)throw new IllegalStateException("repair already in progress");try{R result=operation.call();store.complete(operationId,inputHash,result);return result;}catch(Exception failure){store.fail(operationId,inputHash,failure.getClass().getSimpleName());throw failure;}}
}
