package com.aoo.bcg.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic deployment diff: environment-specific values are explicit, all other drift fails. */
public final class EnvironmentDifferenceReport {
    public record Difference(RuntimeConfigKey key, String baseline, String candidate, boolean allowed, String reason) {}
    private final List<Difference> differences;

    private EnvironmentDifferenceReport(List<Difference> differences) { this.differences = List.copyOf(differences); }
    public List<Difference> differences() { return differences; }
    public boolean accepted() { return differences.stream().allMatch(Difference::allowed); }
    public void requireAccepted() {
        if (!accepted()) throw new IllegalStateException("unexpected environment configuration drift: "
            + differences.stream().filter(value -> !value.allowed()).map(value -> value.key().canonicalName()).toList());
    }

    public static EnvironmentDifferenceReport compare(DeploymentEnvironment baselineEnvironment,
                                                       Map<RuntimeConfigKey,String> baseline,
                                                       DeploymentEnvironment candidateEnvironment,
                                                       Map<RuntimeConfigKey,String> candidate) {
        List<Difference> values = new ArrayList<>();
        for (RuntimeConfigKey key : RuntimeConfigKey.values()) {
            String left = baseline.get(key), right = candidate.get(key);
            if (Objects.equals(left, right)) continue;
            String reason = allowedReason(key, baselineEnvironment, candidateEnvironment);
            values.add(new Difference(key, display(key,left), display(key,right), reason != null,
                reason == null ? "not in environment allowlist" : reason));
        }
        return new EnvironmentDifferenceReport(values);
    }

    private static String allowedReason(RuntimeConfigKey key, DeploymentEnvironment left, DeploymentEnvironment right) {
        if (key == RuntimeConfigKey.ENVIRONMENT) return "deployment environment identity";
        if (key.kind() == RuntimeConfigKey.Kind.ADDRESS) return "environment endpoint";
        if (key.kind() == RuntimeConfigKey.Kind.SECRET) return "environment-scoped secret reference";
        if (key.developmentOnly() && (left == DeploymentEnvironment.DEVELOPMENT || right == DeploymentEnvironment.DEVELOPMENT))
            return "development-only setting";
        return null;
    }

    private static String display(RuntimeConfigKey key, String value) {
        return key.kind() == RuntimeConfigKey.Kind.SECRET && value != null ? "[SECRET_REFERENCE]" : value;
    }
}
