package business.global.pk.zypk;

import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.aoo.bcg.common.reconnect.CardPerspective;
import com.aoo.bcg.common.settlement.SettlementEntry;
import com.aoo.bcg.common.settlement.SettlementResult;
import com.aoo.bcg.common.settlement.SettlementValidator;
import jsproto.c2s.cclass.pk.BasePocker.PockerListType;
import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Single authoritative aggregate for the configurable ZYPK ruleset. */
public final class ZYPKTable {
    public enum State { WAITING, PLAYING, FINISHED }
    public enum DealerPolicy { ROTATE, RANDOM, ROB, FIXED }
    public enum OperationMode { TURN_BASED, SIMULTANEOUS }

    public record SeatState(int seatId, int chips, int wager, int multiplier, boolean viewed,
                            boolean revealed, boolean folded, Set<Integer> visibleSeats) {
        public SeatState { visibleSeats = Set.copyOf(visibleSeats); }
    }

    private record TurnSnapshot(int actorSeat, State state, int operatorSeat, int currentBet,
                                Map<Integer, List<Integer>> hands, List<Integer> deck,
                                Map<Integer, Set<Integer>> publicDrawnCards, Map<Integer, Integer> wagers,
                                Map<Integer, Integer> multipliers, Set<Integer> viewedSeats,
                                Set<Integer> revealedSeats, Set<Integer> foldedSeats,
                                Map<Integer, Set<Integer>> visibleSeats) {
        private TurnSnapshot {
            Map<Integer, List<Integer>> handCopies = new LinkedHashMap<>();
            hands.forEach((seat, cards) -> handCopies.put(seat, List.copyOf(cards)));
            hands = Map.copyOf(handCopies);
            deck = List.copyOf(deck);
            Map<Integer, Set<Integer>> publicCopies = new LinkedHashMap<>();
            publicDrawnCards.forEach((seat, cards) -> publicCopies.put(seat, Set.copyOf(cards)));
            publicDrawnCards = Map.copyOf(publicCopies);
            wagers = Map.copyOf(wagers);
            multipliers = Map.copyOf(multipliers);
            viewedSeats = Set.copyOf(viewedSeats);
            revealedSeats = Set.copyOf(revealedSeats);
            foldedSeats = Set.copyOf(foldedSeats);
            Map<Integer, Set<Integer>> visibleCopies = new LinkedHashMap<>();
            visibleSeats.forEach((seat, seats) -> visibleCopies.put(seat, Set.copyOf(seats)));
            visibleSeats = Map.copyOf(visibleCopies);
        }
    }

    private final long roomId;
    private final long ownerId;
    private final int seatLimit;
    private final int privateCardCount;
    private final int reserveCardCount;
    private final Set<Integer> excludedCards;
    private final Set<ZYPK_AnNiu> enabledActions;
    private final int initialChipCount;
    private final OperationMode operationMode;
    private final GameRandomSource random;
    private final Map<Integer, Long> players = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> hands = new LinkedHashMap<>();
    private final Set<Integer> foldedSeats = new LinkedHashSet<>();
    private final Set<Integer> viewedSeats = new LinkedHashSet<>();
    private final Set<Integer> revealedSeats = new LinkedHashSet<>();
    private final Map<Integer, Integer> wagers = new LinkedHashMap<>();
    private final Map<Integer, Integer> multipliers = new LinkedHashMap<>();
    private final Map<Integer, Integer> chips = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> publicDrawnCards = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> visibleSeats = new LinkedHashMap<>();
    private final List<TurnSnapshot> history = new ArrayList<>();
    private State state = State.WAITING;
    private int dealerSeat = -1;
    private int operatorSeat = -1;
    private int currentBet;
    private List<Integer> deck = List.of();

    public ZYPKTable(long roomId, long ownerId, int seatLimit, int privateCardCount,
                     int reserveCardCount, Set<Integer> excludedCards, long randomSeed) {
        this(roomId, ownerId, seatLimit, privateCardCount, reserveCardCount, excludedCards,
                defaultActions(), 0, OperationMode.TURN_BASED, randomSeed);
    }

    public ZYPKTable(long roomId, long ownerId, int seatLimit, int privateCardCount,
                     int reserveCardCount, Set<Integer> excludedCards,
                     Set<ZYPK_AnNiu> enabledActions, long randomSeed) {
        this(roomId, ownerId, seatLimit, privateCardCount, reserveCardCount, excludedCards,
                enabledActions, 0, OperationMode.TURN_BASED, randomSeed);
    }

    public ZYPKTable(long roomId, long ownerId, int seatLimit, int privateCardCount,
                     int reserveCardCount, Set<Integer> excludedCards,
                     Set<ZYPK_AnNiu> enabledActions, int initialChipCount, long randomSeed) {
        this(roomId, ownerId, seatLimit, privateCardCount, reserveCardCount, excludedCards,
                enabledActions, initialChipCount, OperationMode.TURN_BASED, randomSeed);
    }

    public ZYPKTable(long roomId, long ownerId, int seatLimit, int privateCardCount,
                     int reserveCardCount, Set<Integer> excludedCards,
                     Set<ZYPK_AnNiu> enabledActions, int initialChipCount,
                     OperationMode operationMode, long randomSeed) {
        if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("invalid room identity");
        if (seatLimit < 2 || seatLimit > 8) throw new IllegalArgumentException("seatLimit must be 2..8");
        if (privateCardCount <= 0 || reserveCardCount < 0) throw new IllegalArgumentException("invalid card counts");
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.seatLimit = seatLimit;
        this.privateCardCount = privateCardCount;
        this.reserveCardCount = reserveCardCount;
        this.excludedCards = Set.copyOf(excludedCards == null ? Set.of() : excludedCards);
        this.enabledActions = Set.copyOf(enabledActions == null ? Set.of() : enabledActions);
        if (initialChipCount < 0) throw new IllegalArgumentException("initial chips must not be negative");
        this.initialChipCount = initialChipCount;
        this.operationMode = java.util.Objects.requireNonNull(operationMode, "operation mode required");
        if (this.enabledActions.contains(ZYPK_AnNiu.Not))
            throw new IllegalArgumentException("Not cannot be enabled as an action");
        this.random = new SeededGameRandomSource(randomSeed);
    }

    private static Set<ZYPK_AnNiu> defaultActions() {
        EnumSet<ZYPK_AnNiu> actions = EnumSet.allOf(ZYPK_AnNiu.class);
        actions.remove(ZYPK_AnNiu.Not);
        return actions;
    }

    public synchronized void join(int seatId, long playerId) {
        requireState(State.WAITING);
        if (seatId < 0 || seatId >= seatLimit || playerId <= 0) throw new IllegalArgumentException("invalid seat");
        if (players.containsKey(seatId) || players.containsValue(playerId)) throw new IllegalStateException("seat or player occupied");
        players.put(seatId, playerId);
        wagers.put(seatId, 0);
        multipliers.put(seatId, 1);
        chips.put(seatId, initialChipCount);
        publicDrawnCards.put(seatId, new LinkedHashSet<>());
        visibleSeats.put(seatId, new LinkedHashSet<>());
    }

    public synchronized void start(Integer configuredDealerSeat) {
        start(configuredDealerSeat == null ? DealerPolicy.ROTATE : DealerPolicy.FIXED,
                configuredDealerSeat, null, Set.of());
    }

    public synchronized void start(DealerPolicy policy, Integer configuredDealerSeat,
                                   Integer previousDealerSeat, Set<Integer> robSeats) {
        requireState(State.WAITING);
        if (players.size() < 2) throw new IllegalStateException("at least two players are required");
        ArrayList<Integer> cards = BasePockerLogic.getRandomPockerList(1, 1, PockerListType.POCKERLISTTYPE_TWOEND);
        cards.removeIf(excludedCards::contains);
        int required = Math.addExact(Math.multiplyExact(players.size(), privateCardCount), reserveCardCount);
        if (cards.size() < required) throw new IllegalStateException("not enough cards");
        random.shuffle(cards);
        dealerSeat = selectDealer(policy, configuredDealerSeat, previousDealerSeat, robSeats);
        for (Integer seat : players.keySet()) {
            hands.put(seat, new ArrayList<>(cards.subList(0, privateCardCount)));
            cards.subList(0, privateCardCount).clear();
        }
        deck = new ArrayList<>(cards);
        operatorSeat = dealerSeat;
        state = State.PLAYING;
    }

    private int selectDealer(DealerPolicy policy, Integer configured, Integer previous, Set<Integer> robSeats) {
        if (policy == null) throw new IllegalArgumentException("dealer policy required");
        List<Integer> occupied = List.copyOf(players.keySet());
        return switch (policy) {
            case FIXED -> {
                if (configured == null || !players.containsKey(configured))
                    throw new IllegalArgumentException("fixed dealer seat is not occupied");
                yield configured;
            }
            case RANDOM -> occupied.get(random.nextInt(occupied.size()));
            case ROB -> {
                List<Integer> bidders = occupied.stream()
                        .filter(seat -> robSeats != null && robSeats.contains(seat)).toList();
                List<Integer> candidates = bidders.isEmpty() ? occupied : bidders;
                yield candidates.get(random.nextInt(candidates.size()));
            }
            case ROTATE -> {
                if (previous == null || !players.containsKey(previous)) yield occupied.get(0);
                int index = occupied.indexOf(previous);
                yield occupied.get((index + 1) % occupied.size());
            }
        };
    }

    public synchronized void operate(int seatId, ZYPK_AnNiu action, List<Integer> selectedCards) {
        operate(seatId, action, selectedCards, 0, null);
    }

    public synchronized void operate(int seatId, ZYPK_AnNiu action, List<Integer> selectedCards,
                                     int amount, Integer targetSeat) {
        requireTurn(seatId);
        if (action == null || action == ZYPK_AnNiu.Not) throw new IllegalArgumentException("action required");
        if (!enabledActions.contains(action)) throw new IllegalStateException("action is disabled by room configuration");
        validateAction(seatId, action, amount, targetSeat);
        history.add(snapshot(seatId));
        switch (action) {
            case OutCard -> removeOwnedCards(seatId, selectedCards);
            case BuPai -> draw(seatId, false);
            case BuMingPai -> draw(seatId, true);
            case QiPai -> foldedSeats.add(seatId);
            case KanPai -> viewedSeats.add(seatId);
            case MingPai -> revealedSeats.add(seatId);
            case GenZhu -> wagers.put(seatId, currentBet);
            case YaZhu -> {
                int wager = Math.addExact(wagers.get(seatId), amount);
                wagers.put(seatId, wager);
                currentBet = Math.max(currentBet, wager);
            }
            case JiaBei -> multipliers.put(seatId, Math.multiplyExact(multipliers.get(seatId), amount));
            case BiPai -> {
                visibleSeats.get(seatId).add(targetSeat);
                visibleSeats.get(targetSeat).add(seatId);
            }
            case LiPai -> { }
            default -> throw new IllegalArgumentException("unsupported action: " + action);
        }
        if (operationMode == OperationMode.TURN_BASED) advance(seatId);
    }

    public synchronized void rollbackLastOperation(int seatId) {
        requireState(State.PLAYING);
        if (history.isEmpty()) throw new IllegalStateException("no operation to rollback");
        TurnSnapshot snapshot = history.get(history.size() - 1);
        if (snapshot.actorSeat() != seatId) throw new SecurityException("cannot rollback another player's operation");
        history.remove(history.size() - 1);
        state = snapshot.state();
        operatorSeat = snapshot.operatorSeat();
        currentBet = snapshot.currentBet();
        hands.clear();
        snapshot.hands().forEach((seat, cards) -> hands.put(seat, new ArrayList<>(cards)));
        deck = new ArrayList<>(snapshot.deck());
        publicDrawnCards.clear();
        snapshot.publicDrawnCards().forEach((seat, cards) -> publicDrawnCards.put(seat, new LinkedHashSet<>(cards)));
        replace(wagers, snapshot.wagers());
        replace(multipliers, snapshot.multipliers());
        replace(viewedSeats, snapshot.viewedSeats());
        replace(revealedSeats, snapshot.revealedSeats());
        replace(foldedSeats, snapshot.foldedSeats());
        visibleSeats.clear();
        snapshot.visibleSeats().forEach((seat, visible) -> visibleSeats.put(seat, new LinkedHashSet<>(visible)));
    }

    public synchronized void transferChips(int fromSeat, int toSeat, int amount) {
        requireState(State.PLAYING);
        if (fromSeat == toSeat || !players.containsKey(fromSeat) || !players.containsKey(toSeat))
            throw new IllegalArgumentException("distinct occupied chip transfer seats required");
        if (amount <= 0 || chips.get(fromSeat) < amount) throw new IllegalArgumentException("invalid chip transfer amount");
        chips.put(fromSeat, Math.subtractExact(chips.get(fromSeat), amount));
        chips.put(toSeat, Math.addExact(chips.get(toSeat), amount));
    }

    public synchronized SettlementResult settlement(int roundNo) {
        if (roundNo <= 0) throw new IllegalArgumentException("positive round number required");
        List<SettlementEntry> entries = players.entrySet().stream().map(entry -> {
            long delta = (long) chips.get(entry.getKey()) - initialChipCount;
            return new SettlementEntry(entry.getValue(), delta, Map.of("chipTransfer", delta));
        }).toList();
        SettlementResult result = new SettlementResult(roomId, roundNo,
                ZYPKPersistenceService.PLAY_VERSION, entries);
        SettlementValidator.validate(result, roomId, roundNo,
                ZYPKPersistenceService.PLAY_VERSION, true);
        return result;
    }

    public synchronized void pass(int seatId) {
        requireTurn(seatId);
        history.removeIf(snapshot -> snapshot.actorSeat() == seatId);
        if (operationMode == OperationMode.TURN_BASED) advance(seatId);
    }

    private void validateAction(int seatId, ZYPK_AnNiu action, int amount, Integer targetSeat) {
        if ((action == ZYPK_AnNiu.YaZhu || action == ZYPK_AnNiu.JiaBei) && amount <= 0)
            throw new IllegalArgumentException("positive amount required");
        if (action == ZYPK_AnNiu.GenZhu && currentBet <= wagers.get(seatId))
            throw new IllegalStateException("nothing to call");
        if (action == ZYPK_AnNiu.BiPai && (targetSeat == null || targetSeat == seatId
                || !players.containsKey(targetSeat) || foldedSeats.contains(targetSeat)))
            throw new IllegalArgumentException("active comparison target required");
    }

    private TurnSnapshot snapshot(int actorSeat) {
        Map<Integer, List<Integer>> handCopies = new LinkedHashMap<>();
        hands.forEach((seat, cards) -> handCopies.put(seat, List.copyOf(cards)));
        Map<Integer, Set<Integer>> publicCopies = new LinkedHashMap<>();
        publicDrawnCards.forEach((seat, cards) -> publicCopies.put(seat, Set.copyOf(cards)));
        Map<Integer, Set<Integer>> visibility = new LinkedHashMap<>();
        visibleSeats.forEach((seat, visible) -> visibility.put(seat, Set.copyOf(visible)));
        return new TurnSnapshot(actorSeat, state, operatorSeat, currentBet, Map.copyOf(handCopies), List.copyOf(deck),
                Map.copyOf(publicCopies), Map.copyOf(wagers), Map.copyOf(multipliers),
                Set.copyOf(viewedSeats), Set.copyOf(revealedSeats), Set.copyOf(foldedSeats), visibility);
    }

    private static <T> void replace(Set<T> destination, Set<T> source) {
        destination.clear();
        destination.addAll(source);
    }

    private static <K, V> void replace(Map<K, V> destination, Map<K, V> source) {
        destination.clear();
        destination.putAll(source);
    }

    private void draw(int seatId, boolean publiclyVisible) {
        if (deck.size() <= reserveCardCount) throw new IllegalStateException("reserved cards reached");
        int card = deck.remove(0);
        hands.get(seatId).add(card);
        if (publiclyVisible) publicDrawnCards.get(seatId).add(card);
    }

    private void removeOwnedCards(int seatId, List<Integer> selected) {
        if (selected == null || selected.isEmpty()) throw new IllegalArgumentException("cards required");
        List<Integer> copy = new ArrayList<>(hands.get(seatId));
        for (Integer card : selected) if (!copy.remove(card)) throw new IllegalArgumentException("card not owned");
        hands.put(seatId, copy);
    }

    private void advance(int previousSeat) {
        List<Integer> active = players.keySet().stream().filter(seat -> !foldedSeats.contains(seat)).toList();
        if (active.size() <= 1) {
            operatorSeat = active.isEmpty() ? -1 : active.get(0);
            state = State.FINISHED;
            return;
        }
        List<Integer> orderedSeats = List.copyOf(players.keySet());
        int previousIndex = orderedSeats.indexOf(previousSeat);
        for (int offset = 1; offset <= orderedSeats.size(); offset++) {
            int candidate = orderedSeats.get((previousIndex + offset) % orderedSeats.size());
            if (!foldedSeats.contains(candidate)) {
                operatorSeat = candidate;
                return;
            }
        }
    }

    private void requireTurn(int seatId) {
        requireState(State.PLAYING);
        if (!players.containsKey(seatId) || foldedSeats.contains(seatId)
                || operationMode == OperationMode.TURN_BASED && seatId != operatorSeat)
            throw new IllegalStateException("not operator seat");
    }

    private void requireState(State expected) {
        if (state != expected) throw new IllegalStateException("expected " + expected + " but was " + state);
    }

    public synchronized List<Integer> handView(long viewerId, int seatId) {
        Long playerId = players.get(seatId);
        if (playerId == null) throw new IllegalArgumentException("seat not found");
        Integer viewerSeat = players.entrySet().stream()
                .filter(entry -> entry.getValue() == viewerId).map(Map.Entry::getKey).findFirst().orElse(null);
        boolean visible = viewerId == playerId || state == State.FINISHED || revealedSeats.contains(seatId)
                || viewerSeat != null && visibleSeats.getOrDefault(viewerSeat, Set.of()).contains(seatId);
        if (visible) return CardPerspective.hand(hands.get(seatId), true);
        Set<Integer> publicCards = publicDrawnCards.getOrDefault(seatId, Set.of());
        return hands.get(seatId).stream().map(card -> publicCards.contains(card) ? card : 0).toList();
    }


    public synchronized SeatState seatState(int seatId) {
        if (!players.containsKey(seatId)) throw new IllegalArgumentException("seat not found");
        return new SeatState(seatId, chips.get(seatId), wagers.get(seatId), multipliers.get(seatId), viewedSeats.contains(seatId),
                revealedSeats.contains(seatId), foldedSeats.contains(seatId), Set.copyOf(visibleSeats.get(seatId)));
    }

    public synchronized ZYPKPlayerView reconnectView(long authenticatedPlayerId) {
        Integer viewerSeat = players.entrySet().stream()
                .filter(entry -> entry.getValue() == authenticatedPlayerId)
                .map(Map.Entry::getKey).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("player is not seated in room"));
        Map<Integer, ZYPKPlayerView.SeatView> seatViews = new LinkedHashMap<>();
        players.forEach((seatId, playerId) -> {
            SeatState seat = seatState(seatId);
            List<Integer> cards = handView(authenticatedPlayerId, seatId);
            seatViews.put(seatId, new ZYPKPlayerView.SeatView(seatId, playerId, cards,
                    hands.getOrDefault(seatId, List.of()).size(), seat.wager(), seat.multiplier(),
                    seat.chips(), seat.viewed(), seat.revealed(), seat.folded(), seat.visibleSeats()));
        });
        return new ZYPKPlayerView(roomId, state, viewerSeat, dealerSeat, operatorSeat,
                currentBet, deck.size(), seatViews);
    }

    /** Full server-only state for recovery. Never expose this map to a client. */
    public synchronized Map<String, Object> authoritativeState() {
        Map<Integer, List<Integer>> handCopies = new LinkedHashMap<>();
        hands.forEach((seat, cards) -> handCopies.put(seat, List.copyOf(cards)));
        Map<Integer, Set<Integer>> visibilityCopies = new LinkedHashMap<>();
        visibleSeats.forEach((seat, visible) -> visibilityCopies.put(seat, Set.copyOf(visible)));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ownerId", ownerId);
        snapshot.put("seatLimit", seatLimit);
        snapshot.put("privateCardCount", privateCardCount);
        snapshot.put("reserveCardCount", reserveCardCount);
        snapshot.put("excludedCards", excludedCards);
        snapshot.put("enabledActions", enabledActions.stream().map(Enum::name).toList());
        snapshot.put("initialChipCount", initialChipCount);
        snapshot.put("operationMode", operationMode.name());
        snapshot.put("randomSeed", random.seed());
        snapshot.put("players", java.util.Collections.unmodifiableMap(new LinkedHashMap<>(players)));
        snapshot.put("hands", Map.copyOf(handCopies));
        snapshot.put("deck", List.copyOf(deck));
        snapshot.put("foldedSeats", Set.copyOf(foldedSeats));
        snapshot.put("viewedSeats", Set.copyOf(viewedSeats));
        snapshot.put("revealedSeats", Set.copyOf(revealedSeats));
        snapshot.put("wagers", Map.copyOf(wagers));
        snapshot.put("multipliers", Map.copyOf(multipliers));
        snapshot.put("chips", Map.copyOf(chips));
        snapshot.put("publicDrawnCards", Map.copyOf(publicDrawnCards));
        snapshot.put("visibleSeats", Map.copyOf(visibilityCopies));
        snapshot.put("state", state.name());
        snapshot.put("dealerSeat", dealerSeat);
        snapshot.put("operatorSeat", operatorSeat);
        snapshot.put("currentBet", currentBet);
        return Map.copyOf(snapshot);
    }

    static ZYPKTable restore(long roomId, Map<String, Object> source) {
        long ownerId = number(source, "ownerId").longValue();
        int seatLimit = number(source, "seatLimit").intValue();
        int privateCardCount = number(source, "privateCardCount").intValue();
        int reserveCardCount = number(source, "reserveCardCount").intValue();
        Set<Integer> excluded = integerSet(source.get("excludedCards"));
        Set<ZYPK_AnNiu> actions = new LinkedHashSet<>();
        for (Object value : collection(source.get("enabledActions"))) actions.add(
                com.aoo.bcg.gamespi.StrictEnumDecoder.byName(ZYPK_AnNiu.class, String.valueOf(value)));
        int initialChips = source.containsKey("initialChipCount") ? number(source, "initialChipCount").intValue() : 0;
        OperationMode mode = source.containsKey("operationMode")
                ? com.aoo.bcg.gamespi.StrictEnumDecoder.byName(OperationMode.class,
                        String.valueOf(source.get("operationMode"))) : OperationMode.TURN_BASED;
        ZYPKTable table = new ZYPKTable(roomId, ownerId, seatLimit, privateCardCount,
                reserveCardCount, excluded, actions, initialChips, mode, number(source, "randomSeed").longValue());
        map(source, "players").entrySet().stream()
                .sorted(java.util.Comparator.comparingInt(entry -> Integer.parseInt(String.valueOf(entry.getKey()))))
                .forEach(entry -> table.join(Integer.parseInt(String.valueOf(entry.getKey())),
                        ((Number) entry.getValue()).longValue()));
        table.hands.clear();
        map(source, "hands").forEach((seat, cards) -> table.hands.put(Integer.parseInt(String.valueOf(seat)), integerList(cards)));
        table.deck = integerList(source.get("deck"));
        replace(table.foldedSeats, integerSet(source.get("foldedSeats")));
        replace(table.viewedSeats, integerSet(source.get("viewedSeats")));
        replace(table.revealedSeats, integerSet(source.get("revealedSeats")));
        restoreIntegerMap(table.wagers, map(source, "wagers"));
        restoreIntegerMap(table.multipliers, map(source, "multipliers"));
        if (source.containsKey("chips")) restoreIntegerMap(table.chips, map(source, "chips"));
        if (source.containsKey("publicDrawnCards")) {
            table.publicDrawnCards.clear();
            map(source, "publicDrawnCards").forEach((seat, cards) ->
                    table.publicDrawnCards.put(Integer.parseInt(String.valueOf(seat)), integerSet(cards)));
        }
        table.visibleSeats.clear();
        map(source, "visibleSeats").forEach((seat, visible) ->
                table.visibleSeats.put(Integer.parseInt(String.valueOf(seat)), integerSet(visible)));
        table.state = com.aoo.bcg.gamespi.StrictEnumDecoder.byName(State.class,
                String.valueOf(source.get("state")));
        table.dealerSeat = number(source, "dealerSeat").intValue();
        table.operatorSeat = number(source, "operatorSeat").intValue();
        table.currentBet = number(source, "currentBet").intValue();
        return table;
    }

    private static Number number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException("missing numeric snapshot field: " + key);
        return number;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> map(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> values)) throw new IllegalArgumentException("missing map snapshot field: " + key);
        return (Map<Object, Object>) values;
    }

    private static java.util.Collection<?> collection(Object value) {
        if (!(value instanceof java.util.Collection<?> values)) throw new IllegalArgumentException("invalid snapshot collection");
        return values;
    }

    private static ArrayList<Integer> integerList(Object value) {
        ArrayList<Integer> result = new ArrayList<>();
        for (Object item : collection(value)) result.add(((Number) item).intValue());
        return result;
    }

    private static Set<Integer> integerSet(Object value) {
        return new LinkedHashSet<>(integerList(value));
    }

    private static void restoreIntegerMap(Map<Integer, Integer> target, Map<Object, Object> source) {
        target.clear();
        source.forEach((key, value) -> target.put(Integer.parseInt(String.valueOf(key)), ((Number) value).intValue()));
    }

    public long roomId() { return roomId; }
    public long ownerId() { return ownerId; }
    public long randomSeed() { return random.seed(); }
    public synchronized State state() { return state; }
    public synchronized int dealerSeat() { return dealerSeat; }
    public synchronized int operatorSeat() { return operatorSeat; }
    public synchronized int currentBet() { return currentBet; }
    public synchronized boolean ownsSeat(long playerId, int seatId) {
        return playerId > 0 && java.util.Objects.equals(players.get(seatId), playerId);
    }
}
