package business.global.pk.zjh;

import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.aoo.bcg.common.reconnect.CardPerspective;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.time.Instant;

/** Authoritative ZJH aggregate hosted by the shared game service. */
public final class ZJHTable {
    public enum State { WAITING, PLAYING, ROUND_FINISHED, FINISHED }

    private final long roomId;
    private final long ownerId;
    private final int seatLimit;
    private final ZJHRules rules;
    private final GameRandomSource random;
    private final Map<Integer, Long> players = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> hands = new LinkedHashMap<>();
    private final Set<Integer> readySeats = new LinkedHashSet<>();
    private final Set<Integer> lookedSeats = new LinkedHashSet<>();
    private final Set<Integer> comparedOutSeats = new LinkedHashSet<>();
    private final Map<Integer, Long> committedBets = new LinkedHashMap<>();
    private final Map<Integer, Integer> preBets = new LinkedHashMap<>();
    /** Durable per-player totals; updated exactly once when a round reaches its terminal state. */
    private final Map<Long, Long> cumulativeScoreDeltas = new LinkedHashMap<>();
    private final List<Integer> activeSeats = new ArrayList<>();
    private State state = State.WAITING;
    private int operatorSeat = -1;
    private int bettingRound;
    private int actionsInRound;
    private long pot;
    /** XQP-compatible current call expressed as the blind-player amount. */
    private int currentBlindBet;
    private long stateVersion;
    private long operationDeadlineEpochMillis;
    private int roundNo;

    public ZJHTable(long roomId, long ownerId, ZJHRules rules, long randomSeed) {
        this(roomId, ownerId, rules, randomSeed, true);
    }

    public ZJHTable(long roomId, long ownerId, int seatLimit, long randomSeed) {
        this(roomId, ownerId, ZJHRules.from(Map.of("seatLimit", seatLimit)), randomSeed, true);
    }

    private ZJHTable(long roomId, long ownerId, ZJHRules rules, long randomSeed, boolean ignored) {
        if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("invalid room identity");
        int seatLimit = rules.seatLimit();
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.seatLimit = seatLimit;
        this.rules = rules;
        this.random = new SeededGameRandomSource(randomSeed);
    }

    /** New authoritative write: sitting atomically occupies the seat and marks it ready. */
    public synchronized void sit(int seatId, long playerId) {
        requireState(State.WAITING);
        if (seatId < 0 || seatId >= seatLimit || playerId <= 0) throw new IllegalArgumentException("invalid seat");
        if (players.containsKey(seatId) || players.containsValue(playerId)) throw new IllegalStateException("seat or player already joined");
        players.put(seatId, playerId);
        readySeats.add(seatId);
        stateVersion++;
    }

    /** Central legacy compatibility for old snapshots and tests; production commands use sit. */
    @Deprecated
    public synchronized void join(int seatId, long playerId) { sit(seatId, playerId); }

    public synchronized void start() {
        requireState(State.WAITING);
        if (players.size() < rules.minimumPlayers()) throw new IllegalStateException("minimum player count is not met");
        roundNo = 1;
        beginRound();
    }

    public synchronized void continueRound() {
        requireState(State.ROUND_FINISHED);
        roundNo++;
        beginRound();
    }

    private void beginRound() {
        hands.clear(); activeSeats.clear(); lookedSeats.clear(); comparedOutSeats.clear();
        committedBets.clear(); preBets.clear();
        ZJHSetCard deck = new ZJHSetCard(this, random);
        deck.onXiPai();
        players.keySet().forEach(seat -> hands.put(seat, List.copyOf(deck.popList(3))));
        activeSeats.addAll(players.keySet());
        for (Integer seat : activeSeats) committedBets.put(seat, (long) rules.baseBet());
        pot = Math.multiplyExact((long) activeSeats.size(), rules.baseBet());
        currentBlindBet = rules.baseBet();
        bettingRound = 1;
        actionsInRound = 0;
        operatorSeat = activeSeats.get(0);
        state = State.PLAYING;
        stateVersion++;
        touchDeadline();
    }

    public synchronized void ready(int seatId, boolean ready) {
        requireState(State.WAITING);
        if (!players.containsKey(seatId)) throw new IllegalArgumentException("seat not found");
        // Legacy requests are acknowledged without reintroducing a readiness state:
        // every seated player is ready by definition in CN297.
    }

    public synchronized void fold(int seatId) {
        requireTurn(seatId);
        activeSeats.remove(Integer.valueOf(seatId));
        preBets.remove(seatId);
        stateVersion++;
        advanceAfterAction(seatId);
    }

    public synchronized void look(int seatId) {
        requireTurn(seatId);
        if (bettingRound <= rules.mustBlindRounds()) throw new IllegalStateException("must remain blind in this betting round");
        if (!lookedSeats.add(seatId)) throw new IllegalStateException("cards already viewed");
        stateVersion++;
        touchDeadline();
    }

    public synchronized long bet(int seatId, int amount) {
        requireTurn(seatId);
        int minimum = minimumBet(seatId);
        if (amount < minimum || amount > rules.maximumBet()) throw new IllegalArgumentException("bet outside allowed range");
        committedBets.merge(seatId, (long) amount, Math::addExact);
        pot = Math.addExact(pot, amount);
        currentBlindBet = Math.max(currentBlindBet, lookedSeats.contains(seatId) ? amount / 2 : amount);
        preBets.remove(seatId);
        stateVersion++;
        advanceAfterAction(seatId);
        return pot;
    }

    public synchronized void preBet(int seatId, int amount) {
        requireActiveSeat(seatId);
        if (amount < minimumBet(seatId) || amount > rules.maximumBet()) throw new IllegalArgumentException("preBet outside allowed range");
        preBets.put(seatId, amount);
        stateVersion++;
    }

    /** Human operation timeout. The authoritative fallback is fold; no robot decision is involved. */
    public synchronized void timeout(int seatId, long nowEpochMillis) {
        requireTurn(seatId);
        if (nowEpochMillis < operationDeadlineEpochMillis) throw new IllegalStateException("operation deadline has not elapsed");
        fold(seatId);
    }

    public synchronized int compare(int seatId, int targetSeatId) {
        requireTurn(seatId);
        if (bettingRound < rules.compareStartRound()) throw new IllegalStateException("compare is not available in this betting round");
        if (seatId == targetSeatId || !activeSeats.contains(targetSeatId)) throw new IllegalArgumentException("invalid compare target");
        int compareCost = Math.multiplyExact(minimumBet(seatId), 2);
        if (compareCost > rules.maximumBet()) throw new IllegalStateException("compare cost exceeds maximum bet");
        committedBets.merge(seatId, (long) compareCost, Math::addExact);
        pot = Math.addExact(pot, compareCost);
        boolean operatorWins = ZJHGameLogic.CompareCard(new ArrayList<>(hands.get(seatId)), new ArrayList<>(hands.get(targetSeatId)));
        int loser = operatorWins ? targetSeatId : seatId;
        activeSeats.remove(Integer.valueOf(loser));
        comparedOutSeats.add(loser);
        preBets.remove(loser);
        stateVersion++;
        advanceAfterAction(seatId);
        return loser;
    }

    public synchronized List<Integer> handView(long authenticatedPlayerId, int seatId) {
        Long owner = players.get(seatId);
        if (owner == null) throw new IllegalArgumentException("seat not found");
        boolean visible = state == State.ROUND_FINISHED || state == State.FINISHED
                || (owner == authenticatedPlayerId && lookedSeats.contains(seatId));
        return CardPerspective.hand(hands.getOrDefault(seatId, List.of()), visible);
    }

    public synchronized boolean ownsSeat(long playerId, int seatId) {
        return Long.valueOf(playerId).equals(players.get(seatId));
    }

    public synchronized int seatOf(long playerId) {
        return players.entrySet().stream().filter(entry -> entry.getValue() == playerId)
                .map(Map.Entry::getKey).findFirst().orElse(-1);
    }

    public synchronized Integer winnerSeat() {
        return (state == State.ROUND_FINISHED || state == State.FINISHED) && activeSeats.size() == 1 ? activeSeats.get(0) : null;
    }

    public synchronized Map<String, Object> viewFor(long viewerPlayerId) {
        Map<Integer, Object> seats = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> entry : players.entrySet()) {
            int seatId = entry.getKey();
            seats.put(seatId, Map.of("playerId", entry.getValue(),
                    "cards", handView(viewerPlayerId, seatId),
                    "cardCount", hands.getOrDefault(seatId, List.of()).size(),
                    "active", activeSeats.contains(seatId), "ready", readySeats.contains(seatId),
                    "looked", lookedSeats.contains(seatId), "comparedOut", comparedOutSeats.contains(seatId),
                    "committedBet", committedBets.getOrDefault(seatId, 0L),
                    "preBet", preBets.getOrDefault(seatId, 0)));
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("gameCode", ZJHGameProvider.GAME_CODE);
        view.put("playVersion", ZJHGameProvider.PLAY_VERSION);
        view.put("roomId", roomId);
        // Client permissions must be derived from the room aggregate, never inferred from a handoff player.
        view.put("ownerPlayerId", ownerId);
        view.put("seatLimit", seatLimit);
        view.put("minimumPlayers", rules.minimumPlayers());
        view.put("viewerRole", players.containsValue(viewerPlayerId) ? "SEATED" : "SPECTATOR");
        view.put("state", state.name());
        view.put("roundNo", roundNo);
        view.put("roundLimit", rules.totalRounds());
        view.put("operatorSeat", operatorSeat);
        view.put("bettingRound", bettingRound);
        view.put("compareStartRound", rules.compareStartRound());
        view.put("mustBlindRounds", rules.mustBlindRounds());
        view.put("baseBet", rules.baseBet());
        view.put("maximumBet", rules.maximumBet());
        view.put("minimumBet", players.containsValue(viewerPlayerId) ? minimumBet(seatOf(viewerPlayerId)) : currentBlindBet);
        view.put("pot", pot);
        view.put("operationDeadlineEpochMillis", operationDeadlineEpochMillis);
        view.put("stateVersion", stateVersion);
        view.put("winnerSeat", winnerSeat() == null ? -1 : winnerSeat());
        view.put("seats", Map.copyOf(seats));
        return Map.copyOf(view);
    }

    /** Server-only durable state. Never return this map directly to a client. */
    public synchronized Map<String, Object> authoritativeSnapshot() {
        Map<Integer, Object> seats = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> entry : players.entrySet()) {
            int seatId = entry.getKey();
            seats.put(seatId, Map.of("playerId", entry.getValue(),
                    "cards", List.copyOf(hands.getOrDefault(seatId, List.of())),
                    "active", activeSeats.contains(seatId), "looked", lookedSeats.contains(seatId),
                    "comparedOut", comparedOutSeats.contains(seatId),
                    "committedBet", committedBets.getOrDefault(seatId, 0L),
                    "preBet", preBets.getOrDefault(seatId, 0)));
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ownerId", ownerId); snapshot.put("seatLimit", seatLimit); snapshot.put("state", state.name());
        snapshot.put("operatorSeat", operatorSeat); snapshot.put("randomSeed", randomSeed());
        snapshot.put("readySeats", Set.copyOf(readySeats)); snapshot.put("seats", Map.copyOf(seats));
        snapshot.put("bettingRound", bettingRound); snapshot.put("actionsInRound", actionsInRound);
        snapshot.put("pot", pot); snapshot.put("stateVersion", stateVersion);
        snapshot.put("currentBlindBet", currentBlindBet);
        snapshot.put("operationDeadlineEpochMillis", operationDeadlineEpochMillis);
        snapshot.put("roundNo", roundNo);
        snapshot.put("cumulativeScoreDeltas", Map.copyOf(cumulativeScoreDeltas));
        snapshot.put("rules", rules.toMap());
        return Map.copyOf(snapshot);
    }

    public synchronized Map<String, Object> authoritativeState() { return authoritativeSnapshot(); }

    static ZJHTable restore(long roomId, Map<String, Object> source) {
        ZJHRules restoredRules = source.get("rules") instanceof Map<?, ?> ruleMap
                ? ZJHRules.from(stringObjectMap(ruleMap))
                : ZJHRules.from(Map.of("seatLimit", number(source, "seatLimit").intValue()));
        ZJHTable table = new ZJHTable(roomId, number(source, "ownerId").longValue(),
                restoredRules, number(source, "randomSeed").longValue());
        Map<?, ?> seats = map(source, "seats");
        seats.entrySet().stream().sorted(java.util.Comparator.comparingInt(e -> Integer.parseInt(String.valueOf(e.getKey()))))
                .forEach(entry -> {
                    int seat = Integer.parseInt(String.valueOf(entry.getKey()));
                    Map<?, ?> value = requireMap(entry.getValue(), "seat");
                    table.players.put(seat, ((Number) value.get("playerId")).longValue());
                    table.hands.put(seat, integerList(value.get("cards")));
                    if (Boolean.TRUE.equals(value.get("active"))) table.activeSeats.add(seat);
                    if (Boolean.TRUE.equals(value.get("looked"))) table.lookedSeats.add(seat);
                    if (Boolean.TRUE.equals(value.get("comparedOut"))) table.comparedOutSeats.add(seat);
                    Object committedBet = value.get("committedBet");
                    if (committedBet instanceof Number number) table.committedBets.put(seat, number.longValue());
                    Object preBet = value.get("preBet");
                    if (preBet instanceof Number number && number.intValue() > 0) table.preBets.put(seat, number.intValue());
                });
        // Normalize legacy snapshots to the current invariant: seated means ready.
        table.readySeats.addAll(table.players.keySet());
        table.state = com.aoo.bcg.gamespi.StrictEnumDecoder.byName(State.class, String.valueOf(source.get("state")));
        table.operatorSeat = number(source, "operatorSeat").intValue();
        table.bettingRound = optionalNumber(source, "bettingRound", 0).intValue();
        table.actionsInRound = optionalNumber(source, "actionsInRound", 0).intValue();
        table.pot = optionalNumber(source, "pot", 0).longValue();
        table.currentBlindBet = optionalNumber(source, "currentBlindBet", restoredRules.baseBet()).intValue();
        table.stateVersion = optionalNumber(source, "stateVersion", 0).longValue();
        table.operationDeadlineEpochMillis = optionalNumber(source, "operationDeadlineEpochMillis", 0).longValue();
        table.roundNo = optionalNumber(source, "roundNo", 0).intValue();
        Object cumulative = source.get("cumulativeScoreDeltas");
        if (cumulative instanceof Map<?, ?> values) values.forEach((playerId, score) -> {
            if (!(score instanceof Number number)) throw new IllegalArgumentException("invalid cumulative score snapshot");
            table.cumulativeScoreDeltas.put(Long.parseLong(String.valueOf(playerId)), number.longValue());
        });
        return table;
    }

    private static Number number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException("missing numeric snapshot field: " + key);
        return number;
    }
    private static Number optionalNumber(Map<String, Object> source, String key, Number fallback) {
        Object value = source.get(key);
        return value instanceof Number number ? number : fallback;
    }
    private static Map<?, ?> map(Map<String, Object> source, String key) { return requireMap(source.get(key), key); }
    private static Map<?, ?> requireMap(Object value, String key) {
        if (!(value instanceof Map<?, ?> result)) throw new IllegalArgumentException("missing map snapshot field: " + key);
        return result;
    }
    private static Map<String, Object> stringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
    private static List<Integer> integerList(Object value) {
        if (!(value instanceof java.util.Collection<?> values)) throw new IllegalArgumentException("invalid snapshot collection");
        List<Integer> result = new ArrayList<>();
        for (Object item : values) result.add(((Number) item).intValue());
        return result;
    }

    private void advanceAfterAction(int previousSeat) {
        if (activeSeats.size() <= 1) {
            operatorSeat = activeSeats.isEmpty() ? -1 : activeSeats.get(0);
            state = roundNo >= rules.totalRounds() ? State.FINISHED : State.ROUND_FINISHED;
            operationDeadlineEpochMillis = 0;
            accumulateFinishedRound();
            return;
        }
        actionsInRound++;
        if (actionsInRound >= activeSeats.size()) {
            actionsInRound = 0;
            bettingRound++;
        }
        operatorSeat = nextActiveSeatAfter(previousSeat);
        touchDeadline();
        applyQueuedPreBets();
    }

    /** Finds the clockwise successor even when the acting seat removed itself during fold/compare. */
    private int nextActiveSeatAfter(int previousSeat) {
        for (Integer seat : activeSeats) if (seat > previousSeat) return seat;
        return activeSeats.getFirst();
    }

    /**
     * 预下注是服务端权威动作：轮到座位时自动入池。若玩家看牌后最低下注翻倍导致预设值失效，
     * 只清除该预设并保留其正常操作机会，不替玩家猜测新的下注额。
     */
    private void applyQueuedPreBets() {
        while (state == State.PLAYING) {
            Integer amount = preBets.remove(operatorSeat);
            if (amount == null) return;
            int minimum = minimumBet(operatorSeat);
            if (amount < minimum || amount > rules.maximumBet()) {
                stateVersion++;
                touchDeadline();
                return;
            }
            int automaticSeat = operatorSeat;
            committedBets.merge(automaticSeat, (long) amount, Math::addExact);
            pot = Math.addExact(pot, amount);
            currentBlindBet = Math.max(currentBlindBet, lookedSeats.contains(automaticSeat) ? amount / 2 : amount);
            stateVersion++;
            advanceAfterAction(automaticSeat);
        }
    }

    private void requireTurn(int seatId) {
        requireState(State.PLAYING);
        if (operatorSeat != seatId) throw new IllegalStateException("not operator seat");
    }

    private int minimumBet(int seatId) {
        return Math.multiplyExact(currentBlindBet, lookedSeats.contains(seatId) ? 2 : 1);
    }

    private void requireActiveSeat(int seatId) {
        requireState(State.PLAYING);
        if (!activeSeats.contains(seatId)) throw new IllegalStateException("seat is not active");
    }

    private void touchDeadline() {
        operationDeadlineEpochMillis = Math.addExact(Instant.now().toEpochMilli(), rules.operationSeconds() * 1000L);
    }

    private void requireState(State expected) {
        if (state != expected) throw new IllegalStateException("expected " + expected + " but was " + state);
    }

    public long roomId() { return roomId; }
    public long ownerId() { return ownerId; }
    public long randomSeed() { return random.seed(); }
    public synchronized State state() { return state; }
    public synchronized int operatorSeat() { return operatorSeat; }
    public ZJHRules rules() { return rules; }
    public synchronized int bettingRound() { return bettingRound; }
    public synchronized long pot() { return pot; }
    public synchronized long committedBet(int seatId) { return committedBets.getOrDefault(seatId, 0L); }
    public synchronized Map<Integer, Long> committedBets() { return Map.copyOf(committedBets); }
    public synchronized int winnerBonusPerOpponent() {
        Integer winner = winnerSeat();
        if (winner == null) throw new IllegalStateException("round is not finished");
        List<Integer> cards = hands.get(winner);
        int type = ZJHGameLogic.GetCardType(new ArrayList<>(cards));
        if (type == ZJHGameLogic.ZJH_PAOZI) {
            boolean aaa = cards.stream().allMatch(card -> ZJHGameLogic.GetCardValue(card) == 14);
            return aaa ? rules.aaaBonus() : rules.leopardBonus();
        }
        return type == ZJHGameLogic.ZJH_SHUNJIN ? rules.straightFlushBonus() : 0;
    }
    private void accumulateFinishedRound() {
        Integer winner = winnerSeat();
        if (winner == null) throw new IllegalStateException("finished round has no winner");
        int winnerBonus = winnerBonusPerOpponent();
        long winnerDelta = pot - committedBet(winner);
        for (Map.Entry<Integer, Long> seat : players.entrySet()) {
            if (seat.getKey().equals(winner)) continue;
            long loss = Math.negateExact(Math.addExact(committedBet(seat.getKey()), winnerBonus));
            cumulativeScoreDeltas.merge(seat.getValue(), loss, Math::addExact);
            winnerDelta = Math.addExact(winnerDelta, winnerBonus);
        }
        cumulativeScoreDeltas.merge(players.get(winner), winnerDelta, Math::addExact);
    }
    public synchronized Map<Long, Long> cumulativeScoreDeltas() { return Map.copyOf(cumulativeScoreDeltas); }
    public synchronized long stateVersion() { return stateVersion; }
    public synchronized int roundNo() { return roundNo; }
}
