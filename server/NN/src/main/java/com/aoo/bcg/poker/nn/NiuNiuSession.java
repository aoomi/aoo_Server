package com.aoo.bcg.poker.nn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * CN298 真人牌局状态机。该类不依赖大厅或跑得快实现，接入层只需把鉴权后的玩家命令映射到这里。
 * 所有改变状态的操作都携带 operationId，并在房间内幂等去重。
 */
public final class NiuNiuSession {
  public enum Phase { WAITING, ROBBING, BETTING, SPLITTING, SETTLEMENT, FINISHED }
  public record Result(int round, int bankerSeat, Map<Integer, Long> scoreDelta,
                       Map<Integer, NiuNiuHandEvaluator.Hand> hands) {}

  private final long roomId;
  private final long seed;
  private final NiuNiuRules rules;
  private final Map<Integer, Long> players = new LinkedHashMap<>();
  private final Set<Integer> continuationSeats = new LinkedHashSet<>();
  private final Map<Integer, List<Integer>> hands = new LinkedHashMap<>();
  private final Map<Integer, Integer> robMultipliers = new LinkedHashMap<>();
  private final Map<Integer, Integer> bets = new LinkedHashMap<>();
  private final Set<Integer> splitSeats = new LinkedHashSet<>();
  private final Map<Integer, Long> totalScores = new LinkedHashMap<>();
  private final Set<String> consumedOperations = new LinkedHashSet<>();
  private Phase phase = Phase.WAITING;
  private int round;
  private int bankerSeat = -1;
  private long stateVersion;
  private Result lastResult;

  public NiuNiuSession(long roomId, long ownerId, long seed, NiuNiuRules rules) {
    if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("invalid room identity");
    this.roomId = roomId;
    this.seed = seed;
    this.rules = Objects.requireNonNull(rules);
  }

  public synchronized void sit(int seat, long playerId, String operationId) {
    requireMutable(operationId);
    if (phase != Phase.WAITING || round != 0) throw sitFailure("CN298_SIT_NOT_ALLOWED");
    if (seat < 0 || seat >= rules.maxPlayers()) throw sitFailure("CN298_INVALID_SEAT");
    if (players.size() >= rules.maxPlayers()) throw sitFailure("CN298_SEATS_FULL");
    if (players.containsValue(playerId)) throw sitFailure("CN298_ALREADY_SEATED");
    if (players.containsKey(seat)) throw sitFailure("CN298_SEAT_OCCUPIED");
    players.put(seat, playerId);
    totalScores.put(seat, 0L);
    changed(operationId);
    if (players.size() >= rules.startPlayers()) startRound();
  }

  public synchronized void rob(int seat, int multiplier, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.ROBBING || multiplier < 0 || multiplier > rules.maxRobMultiplier())
      throw new IllegalArgumentException("CN298 invalid rob multiplier");
    robMultipliers.put(seat, multiplier); changed(operationId);
    if (robMultipliers.keySet().containsAll(players.keySet())) chooseBanker();
  }

  public synchronized void bet(int seat, int multiplier, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.BETTING || seat == bankerSeat || multiplier < 1
        || multiplier > Math.max(1, rules.maxPushMultiplier()))
      throw new IllegalArgumentException("CN298 invalid bet");
    bets.put(seat, multiplier); changed(operationId);
    Set<Integer> required = new LinkedHashSet<>(players.keySet()); required.remove(bankerSeat);
    if (bets.keySet().containsAll(required)) phase = Phase.SPLITTING;
  }

  public synchronized void split(int seat, List<Integer> arrangedCards, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.SPLITTING || arrangedCards == null
        || !sameCards(hands.get(seat), arrangedCards)) throw new IllegalArgumentException("CN298 invalid split");
    NiuNiuHandEvaluator.evaluate(arrangedCards, rules.kanShunDouEnabled());
    hands.put(seat, List.copyOf(arrangedCards)); splitSeats.add(seat); changed(operationId);
    if (splitSeats.containsAll(players.keySet())) settle();
  }

  /** 真人超时的确定性保底；不包含机器人选择或策略。 */
  public synchronized void timeout(int seat, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    switch (phase) {
      case ROBBING -> rob(seat, 0, operationId);
      case BETTING -> {
        if (seat == bankerSeat) throw new IllegalStateException("banker does not bet");
        bet(seat, 1, operationId);
      }
      case SPLITTING -> split(seat, hands.get(seat), operationId);
      default -> throw new IllegalStateException("CN298 has no pending operation");
    }
  }

  public synchronized void continueNextRound(int seat, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.SETTLEMENT) throw new IllegalStateException("CN298 continuation is not allowed");
    continuationSeats.add(seat); changed(operationId);
    if (continuationSeats.containsAll(players.keySet())) startRound();
  }

  public synchronized Map<String, Object> snapshotFor(long playerId) {
    Integer viewerSeat = players.entrySet().stream().filter(e -> e.getValue() == playerId)
        .map(Map.Entry::getKey).findFirst().orElse(null);
    Map<Integer, List<Integer>> visibleHands = new LinkedHashMap<>();
    hands.forEach((seat, cards) -> visibleHands.put(seat,
        phase == Phase.SETTLEMENT || phase == Phase.FINISHED || Objects.equals(seat, viewerSeat)
            ? cards : Collections.nCopies(cards.size(), 0)));
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("gameCode", NiuNiuRules.GAME_CODE); snapshot.put("roomId", roomId);
    snapshot.put("stateVersion", stateVersion); snapshot.put("round", round);
    snapshot.put("roundLimit", rules.rounds()); snapshot.put("maxPlayers", rules.maxPlayers());
    snapshot.put("phase", phase.name());
    snapshot.put("players", Map.copyOf(players));
    // Ready is derived from seating. There is intentionally no mutable ready command or independent ready state.
    snapshot.put("readySeats", Set.copyOf(players.keySet()));
    snapshot.put("viewerStatus", viewerSeat == null ? "SPECTATOR" : "SEATED");
    snapshot.put("viewerSeat", viewerSeat == null ? -1 : viewerSeat);
    snapshot.put("bankerSeat", bankerSeat); snapshot.put("robs", Map.copyOf(robMultipliers));
    snapshot.put("bets", Map.copyOf(bets)); snapshot.put("splitSeats", Set.copyOf(splitSeats));
    snapshot.put("hands", visibleHands); snapshot.put("totalScores", Map.copyOf(totalScores));
    snapshot.put("lastResult", lastResult); return Collections.unmodifiableMap(snapshot);
  }

  public synchronized long roomId() { return roomId; }
  public synchronized long stateVersion() { return stateVersion; }
  public synchronized int round() { return round; }
  public synchronized Phase phase() { return phase; }
  public synchronized Result lastResult() { return lastResult; }
  public synchronized Map<Integer, Long> players() { return Map.copyOf(players); }

  private void startRound() {
    if (round >= rules.rounds()) { phase = Phase.FINISHED; return; }
    round++; continuationSeats.clear(); hands.clear(); robMultipliers.clear(); bets.clear(); splitSeats.clear();
    List<Integer> deck = new ArrayList<>(52);
    for (int suit = 1; suit <= 4; suit++) for (int rank = 1; rank <= 13; rank++) deck.add(suit * 100 + rank);
    Collections.shuffle(deck, new Random(seed + round));
    int cursor = 0;
    for (int seat : players.keySet()) hands.put(seat, List.copyOf(deck.subList(cursor, cursor += 5)));
    bankerSeat = -1; phase = Phase.ROBBING; stateVersion++;
  }

  private void chooseBanker() {
    int best = robMultipliers.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    List<Integer> candidates = robMultipliers.entrySet().stream().filter(e -> e.getValue() == best)
        .map(Map.Entry::getKey).sorted().toList();
    bankerSeat = candidates.get(new Random(seed + round * 31L).nextInt(candidates.size()));
    phase = Phase.BETTING; stateVersion++;
  }

  private void settle() {
    Map<Integer, NiuNiuHandEvaluator.Hand> evaluated = new LinkedHashMap<>();
    hands.forEach((seat, cards) -> evaluated.put(seat,
        NiuNiuHandEvaluator.evaluate(cards, rules.kanShunDouEnabled())));
    Map<Integer, Long> delta = new LinkedHashMap<>(); players.keySet().forEach(s -> delta.put(s, 0L));
    var banker = evaluated.get(bankerSeat);
    int rob = Math.max(1, robMultipliers.getOrDefault(bankerSeat, 0));
    for (int seat : players.keySet()) if (seat != bankerSeat) {
      var challenger = evaluated.get(seat);
      boolean challengerWins = challenger.compareTo(banker) > 0;
      var winningHand = challengerWins ? challenger : banker;
      long score = (long) rob * bets.getOrDefault(seat, 1) * rules.settlementMultiplier(winningHand.type());
      delta.merge(seat, challengerWins ? score : -score, Long::sum);
      delta.merge(bankerSeat, challengerWins ? -score : score, Long::sum);
    }
    delta.forEach((seat, score) -> totalScores.merge(seat, score, Long::sum));
    lastResult = new Result(round, bankerSeat, Map.copyOf(delta), Map.copyOf(evaluated));
    continuationSeats.clear(); phase = round >= rules.rounds() ? Phase.FINISHED : Phase.SETTLEMENT; stateVersion++;
  }

  private void requireMutable(String operationId) {
    if (operationId == null || operationId.isBlank()) throw new IllegalArgumentException("operationId is required");
    if (consumedOperations.contains(operationId)) throw new IllegalStateException("duplicate CN298 operationId");
    if (phase == Phase.FINISHED) throw new IllegalStateException("CN298 room is finished");
  }
  private void changed(String operationId) { consumedOperations.add(operationId); stateVersion++; }
  private void requirePlayer(int seat) { if (!players.containsKey(seat)) throw new IllegalArgumentException("unknown CN298 seat"); }
  private static IllegalStateException sitFailure(String code) { return new IllegalStateException(code); }
  private static boolean sameCards(List<Integer> expected, List<Integer> actual) {
    return expected != null && actual.size() == 5 && new HashSet<>(expected).equals(new HashSet<>(actual));
  }
}
