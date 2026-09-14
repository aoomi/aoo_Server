package com.aoo.bcg.common.settlement;
import java.util.List;
public record SettlementResult(long roomId, int roundNo, String playVersion, List<SettlementEntry> entries) {
    public SettlementResult { entries = List.copyOf(entries); }
}
