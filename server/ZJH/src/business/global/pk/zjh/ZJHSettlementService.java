package business.global.pk.zjh;

import com.aoo.bcg.common.settlement.SettlementBalancePolicy;
import com.aoo.bcg.common.settlement.SettlementEntry;
import com.aoo.bcg.common.settlement.SettlementResult;
import com.aoo.bcg.common.settlement.SettlementValidator;
import com.aoo.bcg.gamespi.SettlementPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ZJHSettlementService {
    public Map<String, Object> settle(ZJHTable table, int roundNo) {
        Integer winner = table.winnerSeat();
        if (winner == null) throw new IllegalStateException("room is not finished");
        @SuppressWarnings("unchecked") Map<Integer, Object> seats =
                (Map<Integer, Object>) table.authoritativeSnapshot().get("seats");
        List<SettlementEntry> entries = new ArrayList<>();
        long winnerDelta = 0;
        for (Map.Entry<Integer, Object> seat : seats.entrySet()) {
            @SuppressWarnings("unchecked") Map<String, Object> state = (Map<String, Object>) seat.getValue();
            long playerId = ((Number) state.get("playerId")).longValue();
            if (seat.getKey().equals(winner)) continue;
            entries.add(new SettlementEntry(playerId, -1, Map.of("compare", -1L)));
            winnerDelta = com.aoo.bcg.common.math.ExactDomainMath.increment(winnerDelta, "ZJH winner score");
        }
        @SuppressWarnings("unchecked") Map<String, Object> winnerState = (Map<String, Object>) seats.get(winner);
        entries.add(new SettlementEntry(((Number) winnerState.get("playerId")).longValue(), winnerDelta,
                Map.of("compare", winnerDelta)));
        SettlementResult result = new SettlementResult(table.roomId(), roundNo, ZJHGameProvider.PLAY_VERSION, entries);
        SettlementValidator.validate(result, table.roomId(), roundNo, ZJHGameProvider.PLAY_VERSION,
                SettlementBalancePolicy.ZERO_SUM);
        return Map.of("winnerSeat", winner, "entries", result.entries());
    }

    public SettlementPayload payload(ZJHTable table, int roundNo, String playVersion) {
        Map<String, Object> result = settle(table, roundNo);
        @SuppressWarnings("unchecked") List<SettlementEntry> entries = (List<SettlementEntry>) result.get("entries");
        Map<Long, Long> deltas = new java.util.LinkedHashMap<>();
        entries.forEach(entry -> deltas.put(entry.playerId(), entry.scoreDelta()));
        return new SettlementPayload(table.roomId(), roundNo, playVersion, deltas);
    }
}
