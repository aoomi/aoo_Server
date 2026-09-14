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

/** Authoritative ZJH aggregate hosted by the shared game service. */
public final class ZJHTable {
    public enum State { WAITING, PLAYING, FINISHED }

    private final long roomId;
    private final long ownerId;
    private final int seatLimit;
    private final GameRandomSource random;
    private final Map<Integer, Long> players = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> hands = new LinkedHashMap<>();
    private final Set<Integer> readySeats = new LinkedHashSet<>();
    private final List<Integer> activeSeats = new ArrayList<>();
    private State state = State.WAITING;
    private int operatorSeat = -1;

    public ZJHTable(long roomId, long ownerId, int seatLimit, long randomSeed) {
        if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("invalid room identity");
        if (seatLimit < 2 || seatLimit > 8) throw new IllegalArgumentException("seatLimit must be 2..8");
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.seatLimit = seatLimit;
        this.random = new SeededGameRandomSource(randomSeed);
    }

    public synchronized void join(int seatId, long playerId) {
        requireState(State.WAITING);
        if (seatId < 0 || seatId >= seatLimit || playerId <= 0) throw new IllegalArgumentException("invalid seat");
        if (players.containsKey(seatId) || players.containsValue(playerId)) throw new IllegalStateException("seat or player already joined");
        players.put(seatId, playerId);
    }

    public synchronized void start() {
        requireState(State.WAITING);
        if (players.size() < 2) throw new IllegalStateException("at least two players are required");
        if (!readySeats.containsAll(players.keySet())) throw new IllegalStateException("all joined players must be ready");
        ZJHSetCard deck = new ZJHSetCard(this, random);
        deck.onXiPai();
        players.keySet().forEach(seat -> hands.put(seat, List.copyOf(deck.popList(3))));
        activeSeats.addAll(players.keySet());
        operatorSeat = activeSeats.get(0);
        state = State.PLAYING;
    }

    public synchronized void ready(int seatId, boolean ready) {
        requireState(State.WAITING);
        if (!players.containsKey(seatId)) throw new IllegalArgumentException("seat not found");
        if (ready) readySeats.add(seatId); else readySeats.remove(seatId);
    }

    public synchronized void fold(int seatId) {
        requireTurn(seatId);
        activeSeats.remove(Integer.valueOf(seatId));
        advanceAfterAction(seatId);
    }

    public synchronized int compare(int seatId, int targetSeatId) {
        requireTurn(seatId);
        if (seatId == targetSeatId || !activeSeats.contains(targetSeatId)) throw new IllegalArgumentException("invalid compare target");
        boolean operatorWins = ZJHGameLogic.CompareCard(new ArrayList<>(hands.get(seatId)), new ArrayList<>(hands.get(targetSeatId)));
        int loser = operatorWins ? targetSeatId : seatId;
        activeSeats.remove(Integer.valueOf(loser));
        advanceAfterAction(seatId);
        return loser;
    }

    public synchronized List<Integer> handView(long authenticatedPlayerId, int seatId) {
        Long owner = players.get(seatId);
        if (owner == null) throw new IllegalArgumentException("seat not found");
        return CardPerspective.hand(hands.getOrDefault(seatId, List.of()), owner == authenticatedPlayerId || state == State.FINISHED);
    }

    public synchronized boolean ownsSeat(long playerId, int seatId) {
        return Long.valueOf(playerId).equals(players.get(seatId));
    }

    public synchronized Integer winnerSeat() {
        return state == State.FINISHED && activeSeats.size() == 1 ? activeSeats.get(0) : null;
    }

    public synchronized Map<String, Object> viewFor(long viewerPlayerId) {
        Map<Integer, Object> seats = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> entry : players.entrySet()) {
            int seatId = entry.getKey();
            seats.put(seatId, Map.of("playerId", entry.getValue(),
                    "cards", handView(viewerPlayerId, seatId),
                    "cardCount", hands.getOrDefault(seatId, List.of()).size(),
                    "active", activeSeats.contains(seatId), "ready", readySeats.contains(seatId)));
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("roomId", roomId);
        view.put("state", state.name());
        view.put("operatorSeat", operatorSeat);
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
                    "active", activeSeats.contains(seatId)));
        }
        return Map.of("ownerId", ownerId, "seatLimit", seatLimit, "state", state.name(),
                "operatorSeat", operatorSeat, "randomSeed", randomSeed(),
                "readySeats", Set.copyOf(readySeats), "seats", Map.copyOf(seats));
    }

    public synchronized Map<String, Object> authoritativeState() { return authoritativeSnapshot(); }

    static ZJHTable restore(long roomId, Map<String, Object> source) {
        ZJHTable table = new ZJHTable(roomId, number(source, "ownerId").longValue(),
                number(source, "seatLimit").intValue(), number(source, "randomSeed").longValue());
        Map<?, ?> seats = map(source, "seats");
        seats.entrySet().stream().sorted(java.util.Comparator.comparingInt(e -> Integer.parseInt(String.valueOf(e.getKey()))))
                .forEach(entry -> {
                    int seat = Integer.parseInt(String.valueOf(entry.getKey()));
                    Map<?, ?> value = requireMap(entry.getValue(), "seat");
                    table.join(seat, ((Number) value.get("playerId")).longValue());
                    table.hands.put(seat, integerList(value.get("cards")));
                    if (Boolean.TRUE.equals(value.get("active"))) table.activeSeats.add(seat);
                });
        for (Integer seat : integerList(source.get("readySeats"))) table.readySeats.add(seat);
        table.state = com.aoo.bcg.gamespi.StrictEnumDecoder.byName(State.class, String.valueOf(source.get("state")));
        table.operatorSeat = number(source, "operatorSeat").intValue();
        return table;
    }

    private static Number number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException("missing numeric snapshot field: " + key);
        return number;
    }
    private static Map<?, ?> map(Map<String, Object> source, String key) { return requireMap(source.get(key), key); }
    private static Map<?, ?> requireMap(Object value, String key) {
        if (!(value instanceof Map<?, ?> result)) throw new IllegalArgumentException("missing map snapshot field: " + key);
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
            state = State.FINISHED;
            return;
        }
        int index = activeSeats.indexOf(previousSeat);
        operatorSeat = activeSeats.get(index < 0 || index + 1 >= activeSeats.size() ? 0 : index + 1);
    }

    private void requireTurn(int seatId) {
        requireState(State.PLAYING);
        if (operatorSeat != seatId) throw new IllegalStateException("not operator seat");
    }

    private void requireState(State expected) {
        if (state != expected) throw new IllegalStateException("expected " + expected + " but was " + state);
    }

    public long roomId() { return roomId; }
    public long ownerId() { return ownerId; }
    public long randomSeed() { return random.seed(); }
    public synchronized State state() { return state; }
    public synchronized int operatorSeat() { return operatorSeat; }
}
