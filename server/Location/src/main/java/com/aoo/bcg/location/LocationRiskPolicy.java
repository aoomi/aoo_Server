package com.aoo.bcg.location;

import java.time.Duration;

public record LocationRiskPolicy(double proximityMeters, double maxAccuracyMeters, Duration maxLocationAge,
                                 int proximityScore, int missingScore, int inaccurateScore, Duration idempotencyTtl) {
    public LocationRiskPolicy {
        if (proximityMeters <= 0 || maxAccuracyMeters <= 0 || proximityScore < 0 || missingScore < 0 || inaccurateScore < 0) LocationModels.invalid("invalid risk policy");
        if (maxLocationAge == null || maxLocationAge.isNegative() || maxLocationAge.isZero()) LocationModels.invalid("maxLocationAge must be positive");
        if (idempotencyTtl == null || idempotencyTtl.isNegative() || idempotencyTtl.isZero()) LocationModels.invalid("idempotencyTtl must be positive");
    }
}
