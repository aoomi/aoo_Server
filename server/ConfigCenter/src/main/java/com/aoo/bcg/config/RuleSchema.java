package com.aoo.bcg.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Versioned, closed-world schema for a play type. */
public final class RuleSchema {
    public record Dependency(String field, Object equalsValue, Set<String> requires) {
        public Dependency {
            requireFieldName(field);
            requires = Set.copyOf(requires);
            if (requires.isEmpty()) throw new IllegalArgumentException("dependency targets are required");
        }
    }

    private final String playType;
    private final String version;
    private final Map<String, RuleFieldDefinition> fields;
    private final Map<String, String> canonicalByInputName;
    private final List<Set<String>> mutuallyExclusive;
    private final List<Dependency> dependencies;

    public RuleSchema(String playType, String version, List<RuleFieldDefinition> fields,
                      List<Set<String>> mutuallyExclusive, List<Dependency> dependencies) {
        this.playType = requireName(playType);
        this.version = requireVersion(version);
        if (fields == null || fields.isEmpty()) throw new IllegalArgumentException("schema fields are required");
        LinkedHashMap<String, RuleFieldDefinition> definitions = new LinkedHashMap<>();
        HashMap<String, String> inputs = new HashMap<>();
        for (RuleFieldDefinition field : fields) {
            if (field == null || definitions.putIfAbsent(field.name(), field) != null) {
                throw new IllegalArgumentException("duplicate schema field");
            }
            for (String inputName : field.allNames()) {
                String old = inputs.putIfAbsent(inputName, field.name());
                if (old != null) throw new IllegalArgumentException("rule field alias collision: " + inputName);
            }
        }
        this.fields = Map.copyOf(definitions);
        this.canonicalByInputName = Map.copyOf(inputs);
        this.mutuallyExclusive = copyGroups(mutuallyExclusive);
        this.dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        validateConstraintReferences();
    }

    public String playType() { return playType; }
    public String version() { return version; }
    public Map<String, RuleFieldDefinition> fields() { return fields; }

    /** Resolves aliases, injects the sole authoritative defaults and validates every constraint. */
    public Map<String, Object> normalize(Map<String, ?> draft) {
        Objects.requireNonNull(draft, "draft");
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
        draft.forEach((inputName, value) -> {
            String canonical = canonicalByInputName.get(inputName);
            if (canonical == null) throw new IllegalArgumentException("unknown rule field: " + inputName);
            if (normalized.putIfAbsent(canonical, fields.get(canonical).validate(value)) != null) {
                throw new IllegalArgumentException("canonical field and alias both supplied: " + canonical);
            }
        });
        fields.values().forEach(field -> {
            if (!normalized.containsKey(field.name()) && field.defaultValue() != null) {
                normalized.put(field.name(), field.validate(field.defaultValue()));
            }
            if (field.required() && !normalized.containsKey(field.name())) {
                throw new IllegalArgumentException("required rule field missing: " + field.name());
            }
        });
        for (Set<String> group : mutuallyExclusive) {
            long present = group.stream().filter(normalized::containsKey).count();
            if (present > 1) throw new IllegalArgumentException("mutually exclusive rule fields: " + group);
        }
        for (Dependency dependency : dependencies) {
            if (Objects.equals(normalized.get(dependency.field()), dependency.equalsValue())) {
                Set<String> missing = new HashSet<>(dependency.requires());
                missing.removeAll(normalized.keySet());
                if (!missing.isEmpty()) throw new IllegalArgumentException("rule field dependency missing: " + missing);
            }
        }
        return Map.copyOf(normalized);
    }

    public List<String> descriptions(Map<String, ?> normalizedRules) {
        Map<String, Object> verified = normalize(normalizedRules);
        List<String> result = new ArrayList<>();
        fields.values().stream().filter(field -> verified.containsKey(field.name())).forEach(field ->
                result.add(field.displayName() + "=" + verified.get(field.name()) + " " + field.unit()
                        + "（" + field.explanation() + "）"));
        return List.copyOf(result);
    }

    private void validateConstraintReferences() {
        mutuallyExclusive.forEach(group -> requireKnown(group, "mutual exclusion"));
        dependencies.forEach(dependency -> {
            requireKnown(Set.of(dependency.field()), "dependency source");
            requireKnown(dependency.requires(), "dependency target");
            RuleFieldDefinition source = fields.get(dependency.field());
            source.validate(dependency.equalsValue());
        });
    }

    private void requireKnown(Set<String> names, String label) {
        if (!fields.keySet().containsAll(names)) throw new IllegalArgumentException(label + " contains unknown fields");
    }

    private static List<Set<String>> copyGroups(List<Set<String>> source) {
        if (source == null) return List.of();
        List<Set<String>> result = new ArrayList<>();
        for (Set<String> group : source) {
            Set<String> copy = Set.copyOf(group);
            if (copy.size() < 2) throw new IllegalArgumentException("mutual exclusion needs at least two fields");
            result.add(copy);
        }
        return List.copyOf(result);
    }

    private static String requireName(String value) {
        if (value == null || !value.matches("[a-z][a-z0-9.-]*")) throw new IllegalArgumentException("invalid play type");
        return value;
    }

    private static String requireFieldName(String value) {
        if (value == null || !value.matches("[a-z][a-zA-Z0-9]*")) throw new IllegalArgumentException("invalid rule field");
        return value;
    }

    private static String requireVersion(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) throw new IllegalArgumentException("invalid schema version");
        return value;
    }
}
