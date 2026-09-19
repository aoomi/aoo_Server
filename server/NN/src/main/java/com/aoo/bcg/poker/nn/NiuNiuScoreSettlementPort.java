package com.aoo.bcg.poker.nn;

import com.aoo.bcg.gamespi.RoomScoreLedger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** CN298 adapter example: seat math stays in the game; account identity and mutation stay in the shared ledger. */
final class NiuNiuScoreSettlementPort {
  private final RoomScoreLedger ledger;
  NiuNiuScoreSettlementPort(RoomScoreLedger ledger) { this.ledger = Objects.requireNonNull(ledger); }

  CarrySnapshot capture(long roomId, Map<Integer, Long> players) {
    RoomScoreLedger.Snapshot snapshot = ledger.snapshot(roomId, players.values());
    Map<Integer, Long> carryBySeat = new LinkedHashMap<>();
    players.forEach((seat, playerId) -> carryBySeat.put(seat, required(snapshot.balances(), playerId)));
    return new CarrySnapshot(snapshot.revision(), Map.copyOf(carryBySeat), snapshot.balances());
  }

  Settlement settle(String operationId, long roomId, int roundNo, int bankerSeat,
      Map<Integer, Long> players, CarrySnapshot carry, List<Integer> winnerOrder,
      List<Integer> loserOrder, Map<Integer, Long> theoreticalAmounts) {
    Map<Integer, Long> seatDeltas = NiuNiuSettlementAllocator.allocate(bankerSeat, winnerOrder,
        loserOrder, theoreticalAmounts, carry.carryBySeat());
    Map<Long, Long> playerDeltas = new LinkedHashMap<>();
    seatDeltas.forEach((seat, delta) -> playerDeltas.put(requiredPlayer(players, seat), delta));
    RoomScoreLedger.Receipt receipt = ledger.commit(new RoomScoreLedger.Command(operationId, roomId,
        roundNo, carry.revision(), carry.playerBalances(), playerDeltas));
    return new Settlement(seatDeltas, receipt);
  }

  record CarrySnapshot(long revision, Map<Integer, Long> carryBySeat, Map<Long, Long> playerBalances) {
    CarrySnapshot { carryBySeat = Map.copyOf(carryBySeat); playerBalances = Map.copyOf(playerBalances); }
  }
  record Settlement(Map<Integer, Long> seatDeltas, RoomScoreLedger.Receipt receipt) {
    Settlement { seatDeltas = Map.copyOf(seatDeltas); Objects.requireNonNull(receipt); }
  }
  private static long required(Map<Long, Long> balances, long playerId) {
    Long value = balances.get(playerId); if (value == null) throw new IllegalStateException("CN298 carry score missing"); return value;
  }
  private static long requiredPlayer(Map<Integer, Long> players, int seat) {
    Long player = players.get(seat); if (player == null) throw new IllegalArgumentException("CN298 settlement seat missing"); return player;
  }
}
