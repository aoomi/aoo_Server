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
  private final long ownerId;
  private final long seed;
  private final NiuNiuRules rules;
  private final Map<Integer, Long> players = new LinkedHashMap<>();
  private final Set<Integer> continuationSeats = new LinkedHashSet<>();
  private final Map<Integer, List<Integer>> hands = new LinkedHashMap<>();
  private final Map<Integer, Integer> fifthCards = new LinkedHashMap<>();
  private final Map<Integer, Integer> robMultipliers = new LinkedHashMap<>();
  private final Map<Integer, Integer> bets = new LinkedHashMap<>();
  private final Set<Integer> splitSeats = new LinkedHashSet<>();
  private final Map<Integer, Long> totalScores = new LinkedHashMap<>();
  private final Map<Integer, Integer> winRounds = new LinkedHashMap<>();
  private final Map<Integer, Integer> lossRounds = new LinkedHashMap<>();
  private final Map<Integer, Integer> bankerRounds = new LinkedHashMap<>();
  private final Map<Integer, Integer> bullBullRounds = new LinkedHashMap<>();
  private final Map<Integer, Long> maxRoundWins = new LinkedHashMap<>();
  private final Set<String> consumedOperations = new LinkedHashSet<>();
  private Phase phase = Phase.WAITING;
  private int round;
  private int bankerSeat = -1;
  private long stateVersion;
  private Result lastResult;

  public NiuNiuSession(long roomId, long ownerId, long seed, NiuNiuRules rules) {
    if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("invalid room identity");
    this.roomId = roomId;
    this.ownerId = ownerId;
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
    winRounds.put(seat, 0); lossRounds.put(seat, 0); bankerRounds.put(seat, 0);
    bullBullRounds.put(seat, 0); maxRoundWins.put(seat, 0L);
    changed(operationId);
    if (players.size() == rules.maxPlayers()) startRound();
  }

  public synchronized void start(long playerId, String operationId) {
    requireMutable(operationId);
    if (phase != Phase.WAITING || round != 0) throw new IllegalStateException("CN298_START_NOT_ALLOWED");
    if (playerId != ownerId) throw new IllegalStateException("CN298_ONLY_OWNER_CAN_START");
    if (players.size() < rules.startPlayers()) throw new IllegalStateException("CN298_NOT_ENOUGH_PLAYERS");
    changed(operationId);
    startRound();
  }

  public synchronized void rob(int seat, int multiplier, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.ROBBING || !rules.robOptions().contains(multiplier))
      throw new IllegalArgumentException("CN298 invalid rob multiplier");
    robMultipliers.put(seat, multiplier); changed(operationId);
    if (robMultipliers.keySet().containsAll(players.keySet())) chooseBanker();
  }

  public synchronized void bet(int seat, int multiplier, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.BETTING || seat == bankerSeat || !betOptions(seat).contains(multiplier))
      throw new IllegalArgumentException("CN298 invalid bet");
    bets.put(seat, multiplier); changed(operationId);
    Set<Integer> required = new LinkedHashSet<>(players.keySet()); required.remove(bankerSeat);
    if (bets.keySet().containsAll(required)) {
      players.keySet().forEach(playerSeat -> {
        List<Integer> complete = new ArrayList<>(hands.get(playerSeat));
        complete.add(fifthCards.get(playerSeat));
        hands.put(playerSeat, List.copyOf(complete));
      });
      fifthCards.clear();
      phase = Phase.SPLITTING;
      stateVersion++;
    }
  }

  public synchronized void split(int seat, List<Integer> arrangedCards, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    if (phase != Phase.SPLITTING || splitSeats.contains(seat) || arrangedCards == null
        || !validSplitSelection(hands.get(seat), arrangedCards)) throw new IllegalArgumentException("CN298 invalid split");
    List<Integer> selected = List.copyOf(arrangedCards);
    List<Integer> complete = new ArrayList<>(selected);
    hands.get(seat).stream().filter(card -> !selected.contains(card)).forEach(complete::add);
    NiuNiuHandEvaluator.evaluate(complete, rules.kanShunDouEnabled());
    hands.put(seat, List.copyOf(complete)); splitSeats.add(seat); changed(operationId);
    if (splitSeats.containsAll(players.keySet())) settle();
  }

  /** 真人超时的确定性保底；不包含机器人选择或策略。 */
  public synchronized void timeout(int seat, String operationId) {
    requireMutable(operationId); requirePlayer(seat);
    switch (phase) {
      case ROBBING -> rob(seat, 0, operationId);
      case BETTING -> {
        if (seat == bankerSeat) throw new IllegalStateException("banker does not bet");
        bet(seat, betOptions(seat).getFirst(), operationId);
      }
      case SPLITTING -> split(seat, autoSplitSelection(seat), operationId);
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
    snapshot.put("playVersion", NiuNiuRules.PLAY_VERSION);
    snapshot.put("stateVersion", stateVersion); snapshot.put("round", round);
    snapshot.put("ownerPlayerId", ownerId);
    snapshot.put("roundLimit", rules.rounds()); snapshot.put("maxPlayers", rules.maxPlayers());
    snapshot.put("startPlayers", rules.startPlayers());
    snapshot.put("phase", phase.name());
    snapshot.put("players", Map.copyOf(players));
    // Ready is derived from seating. There is intentionally no mutable ready command or independent ready state.
    snapshot.put("readySeats", Set.copyOf(players.keySet()));
    snapshot.put("viewerStatus", viewerSeat == null ? "SPECTATOR" : "SEATED");
    snapshot.put("viewerSeat", viewerSeat == null ? -1 : viewerSeat);
    snapshot.put("bankerSeat", bankerSeat); snapshot.put("robs", Map.copyOf(robMultipliers));
    snapshot.put("bets", Map.copyOf(bets)); snapshot.put("splitSeats", Set.copyOf(splitSeats));
    snapshot.put("pendingSeats", pendingSeats());
    snapshot.put("robOptions", phase == Phase.ROBBING && viewerSeat != null
        && !robMultipliers.containsKey(viewerSeat) ? rules.robOptions().stream().sorted().toList() : List.of());
    snapshot.put("betOptions", phase == Phase.BETTING && viewerSeat != null && viewerSeat != bankerSeat
        && !bets.containsKey(viewerSeat) ? betOptions(viewerSeat) : List.of());
    snapshot.put("hands", visibleHands); snapshot.put("totalScores", Map.copyOf(totalScores));
    snapshot.put("roundSettlement", lastResult == null ? Map.of() : resultSnapshot(lastResult));
    snapshot.put("playerStats", playerStats());
    snapshot.put("finalSettlement", phase == Phase.FINISHED ? playerStats() : Map.of());
    return Collections.unmodifiableMap(snapshot);
  }

  public synchronized Map<String, Object> authoritativeState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("schemaVersion", 1); state.put("gameCode", NiuNiuRules.GAME_CODE);
    state.put("playVersion", NiuNiuRules.PLAY_VERSION); state.put("roomId", roomId);
    state.put("ownerId", ownerId); state.put("seed", seed);
    state.put("rules", Map.of("rounds", rules.rounds(), "maxPlayers", rules.maxPlayers(),
        "startPlayers", rules.startPlayers(), "mode", rules.mode().name(),
        "maxRobMultiplier", rules.maxRobMultiplier(), "maxPushMultiplier", rules.maxPushMultiplier(),
        "standPolicy", rules.standPolicy().name(), "fastModeEnabled", rules.fastModeEnabled(),
        "kanShunDouEnabled", rules.kanShunDouEnabled()));
    state.put("phase", phase.name()); state.put("round", round); state.put("stateVersion", stateVersion);
    state.put("bankerSeat", bankerSeat); state.put("players", Map.copyOf(players));
    state.put("continuationSeats", List.copyOf(continuationSeats)); state.put("hands", Map.copyOf(hands));
    state.put("fifthCards", Map.copyOf(fifthCards)); state.put("robs", Map.copyOf(robMultipliers));
    state.put("bets", Map.copyOf(bets)); state.put("splitSeats", List.copyOf(splitSeats));
    state.put("totalScores", Map.copyOf(totalScores)); state.put("winRounds", Map.copyOf(winRounds));
    state.put("lossRounds", Map.copyOf(lossRounds)); state.put("bankerRounds", Map.copyOf(bankerRounds));
    state.put("bullBullRounds", Map.copyOf(bullBullRounds)); state.put("maxRoundWins", Map.copyOf(maxRoundWins));
    state.put("consumedOperations", List.copyOf(consumedOperations));
    state.put("lastResultDelta", lastResult == null ? Map.of() : lastResult.scoreDelta());
    return Collections.unmodifiableMap(state);
  }

  public static NiuNiuSession restore(Map<String, Object> state) {
    if (number(state.get("schemaVersion")) != 1
        || !NiuNiuRules.GAME_CODE.equals(String.valueOf(state.get("gameCode")))
        || !NiuNiuRules.PLAY_VERSION.equals(String.valueOf(state.get("playVersion")))) {
      throw new IllegalArgumentException("CN298_UNSUPPORTED_AUTHORITY_SNAPSHOT");
    }
    Map<String, Object> rawRules = objectMap(state.get("rules"), "rules");
    NiuNiuRules rules = new NiuNiuRules(integer(rawRules, "rounds"), integer(rawRules, "maxPlayers"),
        integer(rawRules, "startPlayers"), NiuNiuRules.Mode.valueOf(text(rawRules, "mode")),
        integer(rawRules, "maxRobMultiplier"), integer(rawRules, "maxPushMultiplier"),
        NiuNiuRules.StandPolicy.valueOf(text(rawRules, "standPolicy")),
        bool(rawRules, "fastModeEnabled"), bool(rawRules, "kanShunDouEnabled"));
    NiuNiuSession restored = new NiuNiuSession(longNumber(state, "roomId"), longNumber(state, "ownerId"),
        longNumber(state, "seed"), rules);
    restored.phase = Phase.valueOf(text(state, "phase")); restored.round = integer(state, "round");
    restored.stateVersion = longNumber(state, "stateVersion"); restored.bankerSeat = integer(state, "bankerSeat");
    copyLongMap(state.get("players"), restored.players); copyIntListMap(state.get("hands"), restored.hands);
    copyIntMap(state.get("fifthCards"), restored.fifthCards); copyIntMap(state.get("robs"), restored.robMultipliers);
    copyIntMap(state.get("bets"), restored.bets); copyLongMap(state.get("totalScores"), restored.totalScores);
    copyIntMap(state.get("winRounds"), restored.winRounds); copyIntMap(state.get("lossRounds"), restored.lossRounds);
    copyIntMap(state.get("bankerRounds"), restored.bankerRounds); copyIntMap(state.get("bullBullRounds"), restored.bullBullRounds);
    copyLongMap(state.get("maxRoundWins"), restored.maxRoundWins);
    restored.continuationSeats.addAll(intList(state.get("continuationSeats")));
    restored.splitSeats.addAll(intList(state.get("splitSeats")));
    restored.consumedOperations.addAll(stringList(state.get("consumedOperations")));
    Map<Integer, Long> delta = new LinkedHashMap<>(); copyLongMap(state.get("lastResultDelta"), delta);
    if (!delta.isEmpty()) {
      Map<Integer, NiuNiuHandEvaluator.Hand> evaluated = new LinkedHashMap<>();
      restored.hands.forEach((seat, cards) -> evaluated.put(seat,
          NiuNiuHandEvaluator.evaluate(cards, restored.rules.kanShunDouEnabled())));
      restored.lastResult = new Result(restored.round, restored.bankerSeat, Map.copyOf(delta), Map.copyOf(evaluated));
    }
    return restored;
  }

  public synchronized long roomId() { return roomId; }
  public synchronized long stateVersion() { return stateVersion; }
  public synchronized int round() { return round; }
  public synchronized Phase phase() { return phase; }
  public synchronized Result lastResult() { return lastResult; }
  public synchronized Map<Integer, Long> players() { return Map.copyOf(players); }

  private void startRound() {
    if (round >= rules.rounds()) { phase = Phase.FINISHED; return; }
    round++; continuationSeats.clear(); hands.clear(); fifthCards.clear(); robMultipliers.clear(); bets.clear(); splitSeats.clear();
    List<Integer> deck = new ArrayList<>(52);
    for (int suit = 1; suit <= 4; suit++) for (int rank = 1; rank <= 13; rank++) deck.add(suit * 100 + rank);
    Collections.shuffle(deck, new Random(seed + round));
    int cursor = 0;
    for (int seat : players.keySet()) {
      hands.put(seat, List.copyOf(deck.subList(cursor, cursor += 4)));
      fifthCards.put(seat, deck.get(cursor++));
    }
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
    delta.forEach((seat, score) -> {
      if (score > 0) winRounds.merge(seat, 1, Integer::sum);
      if (score < 0) lossRounds.merge(seat, 1, Integer::sum);
      maxRoundWins.merge(seat, Math.max(0L, score), Math::max);
    });
    bankerRounds.merge(bankerSeat, 1, Integer::sum);
    evaluated.forEach((seat, hand) -> {
      if (hand.type() == NiuNiuHandEvaluator.Type.BULL_BULL) bullBullRounds.merge(seat, 1, Integer::sum);
    });
    lastResult = new Result(round, bankerSeat, Map.copyOf(delta), Map.copyOf(evaluated));
    continuationSeats.clear(); phase = round >= rules.rounds() ? Phase.FINISHED : Phase.SETTLEMENT; stateVersion++;
  }

  /** XQP下注档位：抢庄倍率低于庄家为底分/2倍；达到庄家倍率才拥有推注档位。 */
  private List<Integer> betOptions(int seat) {
    if (seat == bankerSeat || bankerSeat < 0) return List.of();
    int playerRob = robMultipliers.getOrDefault(seat, 0);
    int bankerRob = robMultipliers.getOrDefault(bankerSeat, 0);
    if (playerRob < bankerRob || rules.maxPushMultiplier() == 0) return List.of(1, 2);
    LinkedHashSet<Integer> options = new LinkedHashSet<>();
    options.add(2);
    options.add(Math.max(1, rules.maxPushMultiplier() / 2));
    options.add(rules.maxPushMultiplier());
    return options.stream().filter(value -> value > 0).sorted().toList();
  }

  private List<Integer> pendingSeats() {
    return players.keySet().stream().filter(seat -> switch (phase) {
      case ROBBING -> !robMultipliers.containsKey(seat);
      case BETTING -> seat != bankerSeat && !bets.containsKey(seat);
      case SPLITTING -> !splitSeats.contains(seat);
      case SETTLEMENT -> !continuationSeats.contains(seat);
      default -> false;
    }).sorted().toList();
  }

  private Map<Integer, Map<String, Object>> playerStats() {
    Map<Integer, Map<String, Object>> stats = new LinkedHashMap<>();
    players.keySet().forEach(seat -> stats.put(seat, Map.of(
        "playerId", players.get(seat), "totalScore", totalScores.getOrDefault(seat, 0L),
        "winRounds", winRounds.getOrDefault(seat, 0), "lossRounds", lossRounds.getOrDefault(seat, 0),
        "bankerRounds", bankerRounds.getOrDefault(seat, 0),
        "bullBullRounds", bullBullRounds.getOrDefault(seat, 0),
        "maxRoundWin", maxRoundWins.getOrDefault(seat, 0L))));
    return Map.copyOf(stats);
  }

  private static Map<String, Object> resultSnapshot(Result result) {
    return Map.of("round", result.round(), "bankerSeat", result.bankerSeat(),
        "scoreDelta", result.scoreDelta(), "hands", result.hands());
  }

  private void requireMutable(String operationId) {
    if (operationId == null || operationId.isBlank()) throw new IllegalArgumentException("operationId is required");
    if (consumedOperations.contains(operationId)) throw new IllegalStateException("duplicate CN298 operationId");
    if (phase == Phase.FINISHED) throw new IllegalStateException("CN298 room is finished");
  }
  private void changed(String operationId) { consumedOperations.add(operationId); stateVersion++; }
  private void requirePlayer(int seat) { if (!players.containsKey(seat)) throw new IllegalArgumentException("unknown CN298 seat"); }
  private static IllegalStateException sitFailure(String code) { return new IllegalStateException(code); }
  private List<Integer> autoSplitSelection(int seat) {
    return NiuNiuHandEvaluator.evaluate(hands.get(seat), rules.kanShunDouEnabled())
        .arrangedCards().subList(0, 3);
  }
  private static boolean validSplitSelection(List<Integer> hand, List<Integer> selected) {
    return hand != null && hand.size() == 5 && selected.size() == 3
        && new HashSet<>(selected).size() == 3 && hand.containsAll(selected);
  }
  private static Map<String, Object> objectMap(Object raw, String field) {
    if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("CN298 invalid snapshot " + field);
    Map<String, Object> result = new LinkedHashMap<>(); map.forEach((k, v) -> result.put(String.valueOf(k), v)); return result;
  }
  private static long number(Object value) { return value instanceof Number n ? n.longValue() : Long.MIN_VALUE; }
  private static long longNumber(Map<String, Object> map, String key) {
    Object value = map.get(key); if (!(value instanceof Number n)) throw new IllegalArgumentException("CN298 invalid snapshot " + key); return n.longValue();
  }
  private static int integer(Map<String, Object> map, String key) { return Math.toIntExact(longNumber(map, key)); }
  private static String text(Map<String, Object> map, String key) {
    Object value = map.get(key); if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException("CN298 invalid snapshot " + key); return text;
  }
  private static boolean bool(Map<String, Object> map, String key) {
    Object value = map.get(key); if (!(value instanceof Boolean flag)) throw new IllegalArgumentException("CN298 invalid snapshot " + key); return flag;
  }
  private static List<Integer> intList(Object raw) {
    if (!(raw instanceof List<?> list)) throw new IllegalArgumentException("CN298 invalid snapshot list");
    return list.stream().map(value -> { if (!(value instanceof Number n)) throw new IllegalArgumentException("CN298 invalid snapshot integer"); return n.intValue(); }).toList();
  }
  private static List<String> stringList(Object raw) {
    if (!(raw instanceof List<?> list)) throw new IllegalArgumentException("CN298 invalid snapshot strings");
    return list.stream().map(String::valueOf).toList();
  }
  private static void copyIntMap(Object raw, Map<Integer, Integer> target) {
    objectMap(raw, "integer map").forEach((key, value) -> { if (!(value instanceof Number n)) throw new IllegalArgumentException("CN298 invalid snapshot integer map"); target.put(Integer.parseInt(key), n.intValue()); });
  }
  private static void copyLongMap(Object raw, Map<Integer, Long> target) {
    objectMap(raw, "long map").forEach((key, value) -> { if (!(value instanceof Number n)) throw new IllegalArgumentException("CN298 invalid snapshot long map"); target.put(Integer.parseInt(key), n.longValue()); });
  }
  private static void copyIntListMap(Object raw, Map<Integer, List<Integer>> target) {
    objectMap(raw, "card map").forEach((key, value) -> target.put(Integer.parseInt(key), List.copyOf(intList(value))));
  }
}
