package com.aoo.bcg.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Validates GPS/device claims as risk signals only; it never turns them into identity authority. */
public final class UntrustedRiskSignalPolicy {
    public record Signals(Double latitude, Double longitude, Double accuracyMeters,
                          Instant observedAt, String deviceAttestationId,
                          boolean emulatorReported, boolean rootedReported) { }
    public record Evaluation(boolean usable, int riskScore, Set<String> reasons) {
        public Evaluation { reasons = Set.copyOf(reasons); }
    }

    private final Duration maximumAge;
    private final Duration maximumFutureSkew;
    private final double maximumAccuracyMeters;

    public UntrustedRiskSignalPolicy(Duration maximumAge, Duration maximumFutureSkew,
                                     double maximumAccuracyMeters) {
        if (maximumAge == null || maximumFutureSkew == null || maximumAge.isNegative()
                || maximumAge.isZero() || maximumFutureSkew.isNegative()
                || !Double.isFinite(maximumAccuracyMeters) || maximumAccuracyMeters <= 0)
            throw new IllegalArgumentException("invalid risk signal policy");
        this.maximumAge = maximumAge;
        this.maximumFutureSkew = maximumFutureSkew;
        this.maximumAccuracyMeters = maximumAccuracyMeters;
    }

    public Evaluation evaluate(Signals signals, Instant serverReceivedAt) {
        if (signals == null || serverReceivedAt == null) throw new IllegalArgumentException("risk signals and server time are required");
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        boolean locationComplete = signals.latitude() != null && signals.longitude() != null
                && signals.accuracyMeters() != null && signals.observedAt() != null;
        if (!locationComplete) reasons.add("LOCATION_INCOMPLETE");
        else {
            if (!finiteInRange(signals.latitude(), -90, 90)
                    || !finiteInRange(signals.longitude(), -180, 180)) reasons.add("LOCATION_RANGE_INVALID");
            if (!Double.isFinite(signals.accuracyMeters()) || signals.accuracyMeters() < 0
                    || signals.accuracyMeters() > maximumAccuracyMeters) reasons.add("LOCATION_ACCURACY_UNUSABLE");
            if (signals.observedAt().isBefore(serverReceivedAt.minus(maximumAge))) reasons.add("LOCATION_STALE");
            if (signals.observedAt().isAfter(serverReceivedAt.plus(maximumFutureSkew))) reasons.add("LOCATION_FROM_FUTURE");
        }
        if (signals.deviceAttestationId() == null || !signals.deviceAttestationId().matches("[A-Za-z0-9_-]{16,128}"))
            reasons.add("DEVICE_ATTESTATION_INVALID");
        if (signals.emulatorReported()) reasons.add("EMULATOR_REPORTED");
        if (signals.rootedReported()) reasons.add("ROOTED_REPORTED");
        int score = reasons.stream().mapToInt(reason -> switch (reason) {
            case "EMULATOR_REPORTED", "ROOTED_REPORTED" -> 30;
            case "LOCATION_RANGE_INVALID", "LOCATION_FROM_FUTURE", "DEVICE_ATTESTATION_INVALID" -> 20;
            default -> 10;
        }).sum();
        return new Evaluation(reasons.isEmpty(), Math.min(score, 100), reasons);
    }

    private static boolean finiteInRange(double value, double minimum, double maximum) {
        return Double.isFinite(value) && value >= minimum && value <= maximum;
    }
}
