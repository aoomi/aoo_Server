package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleSchemaTest {
    @Test void canonicalizesAliasesAndOwnsDefaultsRangesEnumsAndDependencies() {
        RuleSchema schema = schema();
        Map<String, Object> normalized = schema.normalize(Map.of(
                "renshu", 3, "paymentMode", "OWNER", "gpsRequired", true, "gpsRadius", 200));
        assertEquals(3, normalized.get("playerCount"));
        assertEquals(8, normalized.get("roundCount"));
        assertTrue(schema.descriptions(normalized).stream().anyMatch(value -> value.contains("roundCount=8 局")));
        assertThrows(IllegalArgumentException.class, () -> schema.normalize(Map.of("playerCount", 5, "paymentMode", "OWNER")));
        assertThrows(IllegalArgumentException.class, () -> schema.normalize(Map.of(
                "playerCount", 3, "paymentMode", "OWNER", "gpsRequired", true)));
        assertThrows(IllegalArgumentException.class, () -> schema.normalize(Map.of(
                "playerCount", 3, "paymentMode", "OWNER", "ownerFee", 3, "aaFee", 2)));
        assertThrows(IllegalArgumentException.class, () -> schema.normalize(Map.of(
                "playerCount", 3, "paymentMode", "CLUB", "unknownLegacyFlag", true)));
    }

    @Test void schemaVersionsAreImmutableAndAliasCollisionsAreRejected() {
        RuleSchemaRegistry registry = new RuleSchemaRegistry();
        registry.register(schema());
        assertSame(schema().getClass(), registry.require("poker.pdk", "2").getClass());
        assertThrows(IllegalStateException.class, () -> registry.register(schema()));
        RuleFieldDefinition first = integer("playerCount", Set.of("count"), true, null, 2, 4, "人", "人数");
        RuleFieldDefinition second = integer("roundCount", Set.of("count"), true, null, 1, 32, "局", "局数");
        assertThrows(IllegalArgumentException.class, () -> new RuleSchema("bad", "1", List.of(first, second), List.of(), List.of()));
    }

    private static RuleSchema schema() {
        return new RuleSchema("poker.pdk", "2", List.of(
                integer("playerCount", Set.of("renshu"), true, null, 2, 4, "人", "参与人数"),
                integer("roundCount", Set.of("jushu"), true, 8, 1, 32, "局", "牌局总局数"),
                enumeration("paymentMode", Set.of("payType"), Set.of("OWNER", "AA", "CLUB"), "付费方"),
                bool("gpsRequired", false, "是否启用定位限制"),
                integer("gpsRadius", Set.of(), false, null, 50, 1000, "米", "定位半径"),
                integer("ownerFee", Set.of(), false, null, 0, 100, "钻石", "房主费用"),
                integer("aaFee", Set.of(), false, null, 0, 100, "钻石", "AA费用")),
                List.of(Set.of("ownerFee", "aaFee")),
                List.of(new RuleSchema.Dependency("gpsRequired", true, Set.of("gpsRadius"))));
    }

    private static RuleFieldDefinition integer(String name, Set<String> aliases, boolean required,
            Object defaultValue, int min, int max, String unit, String explanation) {
        return new RuleFieldDefinition(name, aliases, RuleFieldDefinition.ValueType.INTEGER, unit, required,
                defaultValue, BigDecimal.valueOf(min), BigDecimal.valueOf(max), Set.of(), name, explanation);
    }

    private static RuleFieldDefinition bool(String name, boolean defaultValue, String explanation) {
        return new RuleFieldDefinition(name, Set.of(), RuleFieldDefinition.ValueType.BOOLEAN, "开关", true,
                defaultValue, null, null, Set.of(), name, explanation);
    }

    private static RuleFieldDefinition enumeration(String name, Set<String> aliases, Set<String> values, String explanation) {
        return new RuleFieldDefinition(name, aliases, RuleFieldDefinition.ValueType.ENUM, "枚举", true,
                null, null, null, values, name, explanation);
    }
}
