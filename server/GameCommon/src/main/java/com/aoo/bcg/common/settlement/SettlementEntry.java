package com.aoo.bcg.common.settlement;
import java.util.Map;
public record SettlementEntry(long playerId, long scoreDelta, Map<String, Long> components) {
    public SettlementEntry { components = Map.copyOf(components == null ? Map.of() : components); }
}
