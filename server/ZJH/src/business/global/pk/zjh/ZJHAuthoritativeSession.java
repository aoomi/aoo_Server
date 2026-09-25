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
 * Unified Gateway authority backed by the single CN297 aggregate.
 *
 * <p>This is an adapter, not a shadow game state: legacy ZJH services and the
 * framework snapshot/settlement path read and mutate the same {@link ZJHTable}.</p>
 */
final class ZJHAuthoritativeSession implements AuthoritativeGameSession, LegacyCompatibleRoom {
    private final ZJHTable table;
    private final OperationDeadlineArbiter deadlineArbiter = new OperationDeadlineArbiter();

    ZJHAuthoritativeSession(ZJHTable table) {
        this.table = java.util.Objects.requireNonNull(table, "table");
    }

    static ZJHAuthoritativeSession restore(Map<String, Object> state) {
        Object rawRoomId = state.get("roomId");
        if (!(rawRoomId instanceof Number roomId) || roomId.longValue() <= 0) {
            throw new IllegalArgumentException("CN297 snapshot roomId is required");
        }
        return new ZJHAuthoritativeSession(ZJHTable.restore(roomId.longValue(), state));
    }

    @Override public GameCommandResult execute(GameCommandRequest request) {
        GameRoomHandle room = new GameRoomHandle(table.roomId(), ZJHPersistenceService.GAME_ID,
                ZJHGameProvider.PLAY_VERSION, this);
        return new ZJHCommandHandler().handle(room, request);
    }

    @Override public Map<String, Object> viewFor(long viewerPlayerId) { return table.viewFor(viewerPlayerId); }

    @Override public Map<String, Object> authoritativeState() {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>(table.authoritativeState());
        snapshot.put("roomId", table.roomId());
        snapshot.put("schemaVersion", 3);
        // Gateway lifecycle policies consume the same canonical markers emitted by
        // the shared Poker authority. Keep them in every durable CN297 snapshot so
        // create, restore and later joins cannot disagree about whether play began.
        snapshot.put("phase", table.state().name());
        snapshot.put("started", table.roundNo() > 0 || table.state() != ZJHTable.State.WAITING);
        return Map.copyOf(snapshot);
    }

    @Override public long stateVersion() { return table.stateVersion(); }

    @Override public OperationDeadline operationDeadline() {
        Map<String, Object> state = table.authoritativeState();
        long epochMillis = state.get("operationDeadlineEpochMillis") instanceof Number value
                ? value.longValue() : 0L;
        int seat = table.operatorSeat();
        if (epochMillis <= 0 || seat < 0 || table.state() != ZJHTable.State.PLAYING) {
            return OperationDeadline.none();
        }
        return new OperationDeadline("cn297-" + table.roundNo() + '-' + table.stateVersion(),
                seat, Instant.ofEpochMilli(epochMillis));
    }

    @Override public OperationDeadlineArbiter deadlineArbiter() { return deadlineArbiter; }

    @Override public List<String> invariantViolations() {
        Map<String, Object> snapshot = table.authoritativeState();
        Object rawSeats = snapshot.get("seats");
        if (!(rawSeats instanceof Map<?, ?> seats)) return List.of("CN297_SEATS_MISSING");
        List<Integer> cards = new ArrayList<>();
        for (Object rawSeat : seats.values()) {
            if (!(rawSeat instanceof Map<?, ?> seat) || !(seat.get("cards") instanceof List<?> hand)) {
                return List.of("CN297_HAND_MISSING");
            }
            for (Object card : hand) {
                if (!(card instanceof Number value)) return List.of("CN297_CARD_INVALID");
                cards.add(value.intValue());
            }
        }
        return new HashSet<>(cards).size() == cards.size() ? List.of() : List.of("CN297_CARD_DUPLICATED");
    }

    @Override public SettlementPayload settlement(int roundNo, String playVersion) {
        if (!ZJHGameProvider.PLAY_VERSION.equals(playVersion)) {
            throw new IllegalArgumentException("CN297 settlement playVersion mismatch");
        }
        return new ZJHSettlementService().payload(table, roundNo, playVersion);
    }

    @Override public Object legacyRoom() { return table; }

    @Override public AuthoritativeGameSession authoritativeSession() { return this; }
}
