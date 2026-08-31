package com.aoo.bcg.common.readiness;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Closed-by-default readiness gate. Startup dependencies are verified before traffic is accepted;
 * any later failed verification closes the gate again.
 */
public final class ServiceReadinessGate {
    public enum State { NOT_READY, READY, FAILED }

    private final List<ReadinessCheck> checks;
    private final AtomicReference<State> state = new AtomicReference<>(State.NOT_READY);
    private volatile List<String> failures = List.of("startup checks have not run");

    public ServiceReadinessGate(List<ReadinessCheck> checks) {
        if (checks == null || checks.isEmpty()) throw new IllegalArgumentException("readiness checks are empty");
        this.checks = List.copyOf(checks);
    }

    public synchronized void verifyAndOpen() {
        state.set(State.NOT_READY);
        List<String> detected = new ArrayList<>();
        for (ReadinessCheck check : checks) {
            try {
                check.action().verify();
            } catch (Exception error) {
                String message = error.getMessage();
                detected.add(check.name() + ": " + (message == null || message.isBlank()
                        ? error.getClass().getSimpleName() : message));
            }
        }
        failures = List.copyOf(detected);
        if (!detected.isEmpty()) {
            state.set(State.FAILED);
            throw new IllegalStateException("service readiness failed: " + String.join("; ", detected));
        }
        failures = List.of();
        state.set(State.READY);
    }

    public void requireAcceptingTraffic() {
        if (state.get() != State.READY) {
            throw new ServiceUnavailableException("service is not ready: " + String.join("; ", failures));
        }
    }

    public State state() { return state.get(); }
    public List<String> failures() { return failures; }

    public static ReadinessCheck check(String name, ReadinessCheck.CheckedAction action) {
        return new ReadinessCheck(name, Objects.requireNonNull(action, "action"));
    }
}
