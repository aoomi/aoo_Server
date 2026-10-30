package com.aoo.bcg.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic global -> province -> city inheritance with schema validation after every override. */
public final class RegionalRuleResolver {
    public enum Level { PROVINCE, CITY }
    public record Layer(String regionCode, String parentCode, Level level, Map<String, ?> overrides) {
        public Layer {
            if (regionCode == null || !regionCode.matches("[A-Z0-9-]{2,16}") || level == null)
                throw new IllegalArgumentException("invalid region layer");
            parentCode = parentCode == null ? "" : parentCode;
            overrides = Map.copyOf(overrides == null ? Map.of() : overrides);
            if (level == Level.PROVINCE && !parentCode.isEmpty()) throw new IllegalArgumentException("province inherits global only");
            if (level == Level.CITY && parentCode.isBlank()) throw new IllegalArgumentException("city parent province is required");
        }
    }
    public record RegionalRuleSet(Map<String, Map<String, Object>> rulesByRegion) {
        public Map<String, Object> global() { return rulesByRegion.get(""); }
        public Map<String, Object> require(String regionCode) {
            Map<String, Object> rules = rulesByRegion.get(regionCode);
            if (rules == null) throw new IllegalArgumentException("unknown region: " + regionCode);
            return rules;
        }
    }

    public RegionalRuleSet compile(RuleSchema schema, Map<String, ?> globalDraft, List<Layer> layers) {
        if (schema == null || layers == null) throw new IllegalArgumentException("schema and layers are required");
        LinkedHashMap<String, Layer> byCode = new LinkedHashMap<>();
        for (Layer layer : layers) {
            if (layer == null || byCode.putIfAbsent(layer.regionCode(), layer) != null)
                throw new IllegalArgumentException("duplicate region layer");
        }
        byCode.values().stream().filter(layer -> layer.level() == Level.CITY).forEach(city -> {
            Layer parent = byCode.get(city.parentCode());
            if (parent == null || parent.level() != Level.PROVINCE)
                throw new IllegalArgumentException("city must inherit an existing province: " + city.regionCode());
        });
        LinkedHashMap<String, Map<String, Object>> resolved = new LinkedHashMap<>();
        resolved.put("", schema.normalize(globalDraft));
        HashSet<String> visiting = new HashSet<>();
        byCode.keySet().forEach(code -> resolve(code, schema, byCode, resolved, visiting));
        return new RegionalRuleSet(Map.copyOf(resolved));
    }

    private Map<String, Object> resolve(String code, RuleSchema schema, Map<String, Layer> layers,
                                        Map<String, Map<String, Object>> resolved, Set<String> visiting) {
        Map<String, Object> existing = resolved.get(code);
        if (existing != null) return existing;
        if (!visiting.add(code)) throw new IllegalArgumentException("cyclic region inheritance");
        Layer layer = layers.get(code);
        String parentCode = layer.level() == Level.PROVINCE ? "" : layer.parentCode();
        Map<String, Object> parent = parentCode.isEmpty() ? resolved.get("")
                : resolve(parentCode, schema, layers, resolved, visiting);
        HashMap<String, Object> merged = new HashMap<>(parent);
        merged.putAll(layer.overrides());
        Map<String, Object> result = schema.normalize(merged);
        resolved.put(code, result);
        visiting.remove(code);
        return result;
    }
}
