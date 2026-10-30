package com.aoo.bcg.gateway;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Converts only known, deterministic authority-state failures to typed business
 * errors before they cross the internal HTTP boundary. Unknown failures remain
 * runtime failures and retain the existing retryable 5xx behavior.
 */
final class DeterministicAuthorityErrorBoundary implements GatewayRuntimeProvider.RoomAuthority {
    private final GatewayRuntimeProvider.RoomAuthority delegate;

    DeterministicAuthorityErrorBoundary(GatewayRuntimeProvider.RoomAuthority delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override public Map<String, Object> create(Map<String, Object> command) {
        return call(() -> delegate.create(command));
    }

    @Override public Map<String, Object> recover(Map<String, Object> command) {
        return call(() -> delegate.recover(command));
    }

    @Override public Map<String, Object> destroy(Map<String, Object> command) {
        return call(() -> delegate.destroy(command));
    }

    @Override public Map<String, Object> join(Map<String, Object> command) {
        return call(() -> delegate.join(command));
    }

    @Override public Map<String, Object> leave(Map<String, Object> command) {
        return call(() -> delegate.leave(command));
    }

    @Override public void remove(long roomId, long fencingToken) {
        try {
            delegate.remove(roomId, fencingToken);
        } catch (RuntimeException failure) {
            throw deterministicOrOriginal(failure);
        }
    }

    @Override public void close() {
        delegate.close();
    }

    private <T> T call(Callable<T> operation) {
        try {
            return operation.call();
        } catch (Exception failure) {
            throw deterministicOrOriginal(failure);
        }
    }

    static RuntimeException deterministicOrOriginal(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) root = root.getCause();
        String code = deterministicCode(root);
        if (code != null) return new RoomAuthorityBusinessError(409, code, code);
        if (failure instanceof RuntimeException runtime) return runtime;
        return new IllegalStateException("room authority unavailable", failure);
    }

    static String deterministicCode(Throwable failure) {
        if (failure == null || failure.getMessage() == null) return null;
        String code = failure.getMessage().strip();
        if ("ROOM_ENDED".equals(code)
                || "ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED".equals(code)
                || "ROOM_RUNTIME_IDENTITY_DESTROYED".equals(code)
                || code.startsWith("ROOM_ORCHESTRATOR_LEAVE_")
                || code.matches("ROOM_[A-Z0-9_]+_IDENTITY_CONFLICT")) return code;
        return null;
    }
}
