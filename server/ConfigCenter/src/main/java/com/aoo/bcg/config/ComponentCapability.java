package com.aoo.bcg.config;

import java.util.Map;
import java.util.Set;

/** Complete publication-time declaration for a gameplay component. */
public record ComponentCapability(
        String componentId,
        String version,
        Kind kind,
        Set<String> inputFields,
        Set<String> outputFields,
        Map<String, VersionRange> dependencies,
        Set<String> conflicts,
        Set<String> orderedAfter,
        Set<String> supportedPlayTypes) {

    public enum Kind { RULE, FLOW_ACTION, SCORING, UI }

    public record VersionRange(String minimumInclusive, String maximumExclusive) {
        public VersionRange {
            SemanticVersion minimum = SemanticVersion.parse(minimumInclusive);
            SemanticVersion maximum = SemanticVersion.parse(maximumExclusive);
            if (minimum.compareTo(maximum) >= 0) throw new IllegalArgumentException("invalid component version range");
        }
        public boolean contains(String candidate) {
            SemanticVersion version = SemanticVersion.parse(candidate);
            return version.compareTo(SemanticVersion.parse(minimumInclusive)) >= 0
                    && version.compareTo(SemanticVersion.parse(maximumExclusive)) < 0;
        }
    }

    public ComponentCapability {
        if (componentId == null || !componentId.matches("[a-z][a-z0-9.-]*"))
            throw new IllegalArgumentException("invalid component id");
        SemanticVersion.parse(version);
        if (kind == null) throw new IllegalArgumentException("component kind is required");
        inputFields = copy(inputFields); outputFields = copy(outputFields);
        dependencies = Map.copyOf(dependencies == null ? Map.of() : dependencies);
        conflicts = copy(conflicts); orderedAfter = copy(orderedAfter);
        supportedPlayTypes = copy(supportedPlayTypes);
        if (supportedPlayTypes.isEmpty()) throw new IllegalArgumentException("supported play types are required");
        if (inputFields.stream().anyMatch(value -> !validField(value))
                || outputFields.stream().anyMatch(value -> !validField(value))) {
            throw new IllegalArgumentException("invalid component input/output field");
        }
        if (dependencies.containsKey(componentId) || conflicts.contains(componentId) || orderedAfter.contains(componentId)) {
            throw new IllegalArgumentException("component cannot depend on/conflict/order after itself");
        }
    }

    private static Set<String> copy(Set<String> values) { return Set.copyOf(values == null ? Set.of() : values); }
    private static boolean validField(String value) { return value != null && value.matches("[a-z][a-zA-Z0-9.]*"); }

    private record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {
        static SemanticVersion parse(String value) {
            if (value == null || !value.matches("0|[1-9][0-9]*(\\.(0|[1-9][0-9]*)){2}"))
                throw new IllegalArgumentException("component version must be semantic x.y.z");
            String[] parts = value.split("\\.");
            return new SemanticVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        @Override public int compareTo(SemanticVersion other) {
            int majorOrder = Integer.compare(major, other.major);
            if (majorOrder != 0) return majorOrder;
            int minorOrder = Integer.compare(minor, other.minor);
            return minorOrder != 0 ? minorOrder : Integer.compare(patch, other.patch);
        }
    }
}
