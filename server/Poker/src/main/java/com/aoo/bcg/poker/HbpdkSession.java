package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.StatePayload;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 变体仅负责牌堆裁剪和显示扩展；轮转、幂等、恢复与结算入口统一委托公共权威状态机。 */
final class HbpdkSession implements AuthoritativeGameSession {
    private PokerAuthoritativeSession authority;
    private final HbpdkOptions options;
    private List<Integer> stock;
    private final List<Object> events = new ArrayList<>();

    HbpdkSession(long roomId, long ownerId, long seed, HbpdkOptions options) {
        this(new PokerAuthoritativeSession(roomId, ownerId, options.players(), seed,
                HbpdkRules.policy(options)), options, List.of());
    }

    private HbpdkSession(PokerAuthoritativeSession authority, HbpdkOptions options, List<Integer> stock) {
        this.authority = authority;
        this.options = options;
        this.stock = List.copyOf(stock);
    }

    static HbpdkSession restore(Map<String,Object> state) {
        HbpdkOptions options = HbpdkOptions.from(stringMap(state.get("hbpdkRules")));
        return new HbpdkSession(PokerAuthoritativeSession.restore(state, HbpdkRules.policy(options)),
                options, cards(state.get("stock")));
    }

    @Override public synchronized GameCommandResult execute(GameCommandRequest request) {
        long before = authority.stateVersion();
        GameCommandResult result = authority.execute(request);
        if (request.msgId().contains("start") && authority.stateVersion() != before) trimDeal();
        if (authority.stateVersion() != before)
            events.add(Map.of("version", authority.stateVersion(), "after", authoritativeState()));
        return result;
    }

    /**
     * 发牌后用服务端牌库生成固定手数和底牌，并再次经过恢复不变量校验。
     * 这样客户端既不能决定底牌，也不能借变体规则绕过公共牌权归属检查。
     */
    private void trimDeal() {
        Map<String,Object> snapshot = new LinkedHashMap<>(authority.authoritativeState());
        PokerTurnState state = (PokerTurnState) snapshot.get("state");
        List<Integer> deal = new ArrayList<>();
        for (int seat : state.hands().keySet().stream().sorted().toList())
            deal.addAll(state.hands().get(seat));
        if (options.cardNum() == 1) deal.removeIf(HbpdkRules::excludedFromFifteenCardDeck);
        Map<Integer,List<Integer>> hands = new LinkedHashMap<>();
        int offset = 0;
        for (int seat : state.hands().keySet().stream().sorted().toList()) {
            hands.put(seat, new ArrayList<>(deal.subList(offset, offset + options.handSize())));
            offset += options.handSize();
        }
        List<Integer> remaining = new ArrayList<>(deal.subList(offset, deal.size()));
        int required = HbpdkRules.policy(options).family().profile().requiredFirstCard();
        Optional<Integer> holder = hands.entrySet().stream()
                .filter(entry -> entry.getValue().contains(required)).map(Map.Entry::getKey).findFirst();
        int lead;
        if (holder.isPresent()) lead = holder.get();
        else {
            int index = remaining.indexOf(required);
            if (index < 0) throw new IllegalStateException("required card absent after variant deal");
            lead = (Integer) snapshot.get("initialLeadSeat");
            int displaced = hands.get(lead).set(options.handSize() - 1, required);
            remaining.set(index, displaced);
        }
        stock = List.copyOf(remaining);
        snapshot.put("initialLeadSeat", lead);
        snapshot.put("state", new PokerTurnState(hands, lead, null, -1, Set.of(), false, -1));
        authority = PokerAuthoritativeSession.restore(snapshot, HbpdkRules.policy(options));
    }

    @Override public synchronized Map<String,Object> viewFor(long viewer) {
        Map<String,Object> view = new LinkedHashMap<>(authority.viewFor(viewer));
        view.put("stockCount", stock.size());
        view.put("showRemainingCardCount", options.kexuan().contains(11));
        return Map.copyOf(view);
    }

    @Override public synchronized Map<String,Object> authoritativeState() {
        Map<String,Object> state = new LinkedHashMap<>(authority.authoritativeState());
        state.put("hbpdkRules", options.map());
        state.put("stock", stock);
        return Map.copyOf(state);
    }

    List<Object> recordedEvents() { return List.copyOf(events); }

    static StatePayload replay(StatePayload base, List<Object> ordered) {
        Map<String,Object> current = new LinkedHashMap<>(base.asMap());
        long version = number(current.get("stateVersion"));
        for (Object raw : ordered) {
            if (!(raw instanceof Map<?,?> event) || number(event.get("version")) != version + 1
                    || !(event.get("after") instanceof Map<?,?> after))
                throw new IllegalArgumentException("variant replay gap or payload error");
            current = new LinkedHashMap<>();
            for (var entry : after.entrySet()) current.put(String.valueOf(entry.getKey()), entry.getValue());
            version = number(event.get("version"));
            if (number(current.get("stateVersion")) != version)
                throw new IllegalArgumentException("variant replay version mismatch");
        }
        return StatePayload.copyOf(current);
    }

    @Override public SettlementPayload settlement(int round, String version) {
        return authority.settlement(round, version);
    }
    @Override public long stateVersion() { return authority.stateVersion(); }
    @Override public OperationDeadline operationDeadline() { return authority.operationDeadline(); }
    @Override public OperationDeadlineArbiter deadlineArbiter() { return authority.deadlineArbiter(); }
    @Override public List<String> invariantViolations() {
        List<String> errors = new ArrayList<>(authority.invariantViolations());
        Set<Integer> seen = new HashSet<>(stock);
        if (seen.size() != stock.size()) errors.add("DUPLICATE_STOCK");
        if (authority.authoritativeState().get("state") instanceof PokerTurnState state)
            for (List<Integer> hand : state.hands().values())
                for (int card : hand) if (seen.contains(card)) errors.add("STOCK_HAND_OVERLAP");
        return List.copyOf(errors);
    }

    private static long number(Object value) { return ((Number) value).longValue(); }
    private static List<Integer> cards(Object value) {
        if (value == null) return List.of();
        return ((Collection<?>) value).stream().map(item -> ((Number) item).intValue()).toList();
    }
    private static Map<String,Object> stringMap(Object value) {
        Map<String,Object> result = new LinkedHashMap<>();
        ((Map<?,?>) value).forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }
}
