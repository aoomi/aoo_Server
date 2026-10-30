package com.aoo.bcg.config;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Rejects incomplete, cyclic, conflicting, unordered or version-incompatible component sets. */
public final class ComponentCombinationCompiler {
    public record CompiledComponents(String playType, List<ComponentCapability> ordered,
                                     Map<String, String> outputOwners) { }

    public CompiledComponents compile(String playType, RuleSchema schema, List<ComponentCapability> candidates) {
        if (schema == null || !schema.playType().equals(playType)) throw new IllegalArgumentException("play schema mismatch");
        if (candidates == null || candidates.isEmpty()) throw new IllegalArgumentException("components are required");
        LinkedHashMap<String, ComponentCapability> components = new LinkedHashMap<>();
        EnumSet<ComponentCapability.Kind> capabilities = EnumSet.noneOf(ComponentCapability.Kind.class);
        for (ComponentCapability component : candidates) {
            if (component == null || components.putIfAbsent(component.componentId(), component) != null)
                throw new IllegalArgumentException("duplicate component id");
            if (!component.supportedPlayTypes().contains(playType))
                throw new IllegalArgumentException("component does not support play type: " + component.componentId());
            capabilities.add(component.kind());
        }
        if (!capabilities.equals(EnumSet.allOf(ComponentCapability.Kind.class)))
            throw new IllegalArgumentException("rule, flow action, scoring and UI capabilities are all required");

        Map<String, Set<String>> outgoing = new HashMap<>();
        Map<String, Integer> incoming = new HashMap<>();
        components.keySet().forEach(id -> { outgoing.put(id, new HashSet<>()); incoming.put(id, 0); });
        components.values().forEach(component -> {
            component.conflicts().forEach(conflict -> {
                if (components.containsKey(conflict)) throw new IllegalArgumentException("component conflict: " + component.componentId() + "/" + conflict);
            });
            component.dependencies().forEach((dependencyId, range) -> {
                ComponentCapability dependency = components.get(dependencyId);
                if (dependency == null) throw new IllegalArgumentException("missing component dependency: " + dependencyId);
                if (!range.contains(dependency.version())) throw new IllegalArgumentException("component dependency version mismatch: " + dependencyId);
                addEdge(dependencyId, component.componentId(), outgoing, incoming);
            });
            component.orderedAfter().forEach(previous -> {
                if (!components.containsKey(previous)) throw new IllegalArgumentException("ordered component missing: " + previous);
                addEdge(previous, component.componentId(), outgoing, incoming);
            });
        });

        PriorityQueue<String> ready = new PriorityQueue<>();
        incoming.forEach((id, count) -> { if (count == 0) ready.add(id); });
        List<ComponentCapability> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.remove();
            ordered.add(components.get(id));
            outgoing.get(id).stream().sorted().forEach(next -> {
                int count = incoming.compute(next, (ignored, old) -> old - 1);
                if (count == 0) ready.add(next);
            });
        }
        if (ordered.size() != components.size()) throw new IllegalArgumentException("cyclic component dependency/order");

        LinkedHashMap<String, String> outputOwners = new LinkedHashMap<>();
        Set<String> availableInputs = new HashSet<>(schema.fields().keySet());
        for (ComponentCapability component : ordered) {
            if (!availableInputs.containsAll(component.inputFields())) {
                Set<String> missing = new HashSet<>(component.inputFields()); missing.removeAll(availableInputs);
                throw new IllegalArgumentException("component inputs are unreachable: " + missing);
            }
            component.outputFields().forEach(output -> {
                String previous = outputOwners.putIfAbsent(output, component.componentId());
                if (previous != null) throw new IllegalArgumentException("component output collision: " + output);
                availableInputs.add(output);
            });
        }
        return new CompiledComponents(playType, List.copyOf(ordered), Map.copyOf(outputOwners));
    }

    private static void addEdge(String before, String after, Map<String, Set<String>> outgoing,
                                Map<String, Integer> incoming) {
        if (outgoing.get(before).add(after)) incoming.compute(after, (ignored, old) -> old + 1);
    }
}
