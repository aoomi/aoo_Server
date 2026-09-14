package com.aoo.bcg.common.idempotency;
import java.time.Duration;
import java.util.Optional;
public interface IdempotencyStore<R> {
    Optional<IdempotencyResult<R>> findResult(IdempotencyKey key);
    default Optional<R> find(IdempotencyKey key){return findResult(key).map(IdempotencyResult::data);}
    boolean acquire(IdempotencyKey key, Duration retention);
    void saveResult(IdempotencyKey key,IdempotencyResult<R> result);
    void save(IdempotencyKey key,R result,Duration retention);
    void release(IdempotencyKey key);
    /** Keeps the reservation queryable when mutation may have committed but acknowledgement was lost. */
    default void markUnknown(IdempotencyKey key) { }
}
