package com.aoo.bcg.common.repair;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Optimistic compare-version gate shared by every online data repair. */
public final class VersionedRepairGuard {
    public record Request(String repairId, String entityType, String entityId, long expectedVersion) {
        public Request {
            if (repairId == null || repairId.isBlank() || entityType == null || entityType.isBlank()
                || entityId == null || entityId.isBlank() || expectedVersion < 0) throw new IllegalArgumentException("invalid versioned repair request");
        }
    }
    public record Result(long beforeVersion, long afterVersion) { }

    public Result execute(Request request, LongSupplier lockedVersionReader, RepairMutation mutation) throws Exception {
        Objects.requireNonNull(request); Objects.requireNonNull(lockedVersionReader); Objects.requireNonNull(mutation);
        long actual = lockedVersionReader.getAsLong();
        if (actual != request.expectedVersion()) throw new StaleRepairException(request.expectedVersion(), actual);
        long after = mutation.apply(actual);
        if (after <= actual) throw new IllegalStateException("repair must advance entity version");
        return new Result(actual, after);
    }

    @FunctionalInterface public interface RepairMutation { long apply(long lockedVersion) throws Exception; }
    public static final class StaleRepairException extends RuntimeException {
        private final long expected; private final long actual;
        public StaleRepairException(long expected, long actual) { super("repair version conflict"); this.expected=expected; this.actual=actual; }
        public long expected() { return expected; } public long actual() { return actual; }
    }
}
