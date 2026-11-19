package business.global.pk.zjh;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.LegacyCompatibleRoom;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Unified Gateway authority backed by the single ZJH aggregate.
 *
 * <p>This is an adapter, not a shadow game state: legacy ZJH services and the
 * framework snapshot/settlement path read and mutate the same {@link ZjhTable}.</p>
 */
final class ZjhAuthoritativeSession implements AuthoritativeGameSession, LegacyCompatibleRoom {
    private final ZjhTable table;
    private final OperationDeadlineArbiter deadlineArbiter = new OperationDeadlineArbiter();

    ZjhAuthoritativeSession(ZjhTable table) {
        this.table = java.util.Objects.requireNonNull(table, "table");
    }

    static ZjhAuthoritativeSession restore(Map<String, Object> state) {
        Object rawRoomId = state.get("roomId");
        if (!(rawRoomId instanceof Number roomId) || roomId.longValue() <= 0) {
            throw new IllegalArgumentException("ZJH snapshot roomId is required");
        }
        return new ZjhAuthoritativeSession(ZjhTable.restore(roomId.longValue(), state));
    }

    @Override public GameCommandResult execute(GameCommandRequest request) {
        GameRoomHandle room = new GameRoomHandle(table.roomId(), ZjhPersistenceService.GAME_ID,
                ZjhGameProvider.PLAY_VERSION, this);
        return new ZjhCommandHandler().handle(room, request);
    }

    @Override public Map<String, Object> viewFor(long viewerPlayerId) { return table.viewFor(viewerPlayerId); }

    @Override public Map<String, Object> authoritativeState() {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>(table.authoritativeState());
        snapshot.put("roomId", table.roomId());
        snapshot.put("schemaVersion", 3);
        // Gateway lifecycle policies consume the same canonical markers emitted by
        // the shared Poker authority. Keep them in every durable ZJH snapshot so
        // create, restore and later joins cannot disagree about whether play began.
        snapshot.put("phase", table.state().name());
        snapshot.put("started", table.roundNo() > 0 || table.state() != ZjhTable.State.WAITING);
        return Map.copyOf(snapshot);
    }

    @Override public long stateVersion() { return table.stateVersion(); }

    @Override public OperationDeadline operationDeadline() {
        Map<String, Object> state = table.authoritativeState();
        long epochMillis = state.get("operationDeadlineEpochMillis") instanceof Number value
                ? value.longValue() : 0L;
        int seat = table.operatorSeat();
        if (epochMillis <= 0 || seat < 0 || table.state() != ZjhTable.State.PLAYING) {
            return OperationDeadline.none();
        }
        return new OperationDeadline("zjh-" + table.roundNo() + '-' + table.stateVersion(),
                seat, Instant.ofEpochMilli(epochMillis));
    }

    @Override public OperationDeadlineArbiter deadlineArbiter() { return deadlineArbiter; }

    @Override public List<String> invariantViolations() {
        Map<String, Object> snapshot = table.authoritativeState();
        Object rawSeats = snapshot.get("seats");
        if (!(rawSeats instanceof Map<?, ?> seats)) return List.of("ZJH_SEATS_MISSING");
        List<Integer> cards = new ArrayList<>();
        for (Object rawSeat : seats.values()) {
            if (!(rawSeat instanceof Map<?, ?> seat) || !(seat.get("cards") instanceof List<?> hand)) {
                return List.of("ZJH_HAND_MISSING");
            }
            for (Object card : hand) {
                if (!(card instanceof Number value)) return List.of("ZJH_CARD_INVALID");
                cards.add(value.intValue());
            }
        }
        return new HashSet<>(cards).size() == cards.size() ? List.of() : List.of("ZJH_CARD_DUPLICATED");
    }

    @Override public SettlementPayload settlement(int roundNo, String playVersion) {
        if (!ZjhGameProvider.PLAY_VERSION.equals(playVersion)) {
            throw new IllegalArgumentException("ZJH settlement playVersion mismatch");
        }
        return new ZjhSettlementService().payload(table, roundNo, playVersion);
    }

    @Override public Object legacyRoom() { return table; }

    @Override public AuthoritativeGameSession authoritativeSession() { return this; }
}
