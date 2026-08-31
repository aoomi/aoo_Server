package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.StatePayload;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 只保存玩法选项和特殊开局钩子；牌局生命周期始终由唯一公共权威状态机负责。 */
final class AypdkSession implements AuthoritativeGameSession {
    private PokerAuthoritativeSession authority;
    private final AypdkOptions options;
    private final List<Object> events = new ArrayList<>();

    AypdkSession(long roomId, long ownerId, long seed, AypdkOptions options) {
        this(new PokerAuthoritativeSession(roomId, ownerId, 3, seed, AypdkRules.policy(options)), options);
    }

    private AypdkSession(PokerAuthoritativeSession authority, AypdkOptions options) {
        this.authority = authority;
        this.options = options;
    }

    static AypdkSession restore(Map<String,Object> state) {
        Object raw = state.get("aypdkOptions");
        if (!(raw instanceof Map<?,?> source)) throw new IllegalArgumentException("variant options missing");
        Map<String,Object> rules = new LinkedHashMap<>();
        source.forEach((key, value) -> rules.put(String.valueOf(key), value));
        AypdkOptions options = AypdkOptions.from(rules);
        return new AypdkSession(PokerAuthoritativeSession.restore(state, AypdkRules.policy(options)), options);
    }

    @Override public synchronized GameCommandResult execute(GameCommandRequest request) {
        long before = authority.stateVersion();
        GameCommandResult result = authority.execute(request);
        if (options.teshu().contains(0) && request.msgId().contains("start")) applySpecialDealWin();
        if (authority.stateVersion() != before)
            events.add(Map.of("version", authority.stateVersion(), "after", authoritativeState()));
        return result;
    }

    @Override public Map<String,Object> viewFor(long viewer) { return authority.viewFor(viewer); }

    @Override public synchronized Map<String,Object> authoritativeState() {
        Map<String,Object> state = new LinkedHashMap<>(authority.authoritativeState());
        state.put("aypdkOptions", options.toMap());
        return Map.copyOf(state);
    }

    List<Object> recordedEvents() { return List.copyOf(events); }

    static StatePayload replay(StatePayload base, List<Object> ordered) {
        Map<String,Object> current = new LinkedHashMap<>(base.asMap());
        long version = number(current.get("stateVersion"));
        for (Object raw : ordered) {
            if (!(raw instanceof Map<?,?> event)) throw new IllegalArgumentException("invalid variant replay event");
            long next = number(event.get("version"));
            if (next != version + 1) throw new IllegalArgumentException("variant replay version gap");
            if (!(event.get("after") instanceof Map<?,?> after))
                throw new IllegalArgumentException("invalid variant replay snapshot");
            current = new LinkedHashMap<>();
            for (var item : after.entrySet()) current.put(String.valueOf(item.getKey()), item.getValue());
            if (number(current.get("stateVersion")) != next)
                throw new IllegalArgumentException("variant replay payload version mismatch");
            version = next;
        }
        return StatePayload.copyOf(current);
    }

    @Override public long stateVersion() { return authority.stateVersion(); }
    @Override public OperationDeadline operationDeadline() { return authority.operationDeadline(); }
    @Override public OperationDeadlineArbiter deadlineArbiter() { return authority.deadlineArbiter(); }
    @Override public List<String> invariantViolations() { return authority.invariantViolations(); }
    @Override public SettlementPayload settlement(int round, String version) {
        return authority.settlement(round, version);
    }

    /** 特殊开局只改写一次权威快照，再走同一恢复校验，避免旁路直接篡改状态字段。 */
    private void applySpecialDealWin() {
        Map<String,Object> snapshot = new LinkedHashMap<>(authority.authoritativeState());
        if (!(snapshot.get("state") instanceof PokerTurnState state)) return;
        for (var entry : state.hands().entrySet()) {
            long aces = entry.getValue().stream().filter(card -> PokerCardCodec.rank(card) == 14).count();
            long twos = entry.getValue().stream().filter(card -> PokerCardCodec.rank(card) == 15).count();
            if (aces == 3 && twos == 1) {
                Map<Integer,List<Integer>> hands = new LinkedHashMap<>(state.hands());
                hands.put(entry.getKey(), List.of());
                snapshot.put("state", new PokerTurnState(hands, entry.getKey(), null, -1,
                        Set.of(), true, entry.getKey()));
                snapshot.put("operationDeadline", Map.of());
                authority = PokerAuthoritativeSession.restore(snapshot, AypdkRules.policy(options));
                return;
            }
        }
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }
}
