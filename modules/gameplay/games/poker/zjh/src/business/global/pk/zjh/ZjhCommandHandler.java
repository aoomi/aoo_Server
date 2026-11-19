package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZjhCommandHandler implements GameCommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ZjhCommandHandler.class);
    private static final String COMMAND_PREFIX = "poker.quanguo2997.";
    public static final String DISPATCH = "common.room.dispatch";
    public static final String SIT = COMMAND_PREFIX + "sit_req";
    public static final String JOIN = COMMAND_PREFIX + "join_req";
    public static final String START = COMMAND_PREFIX + "start_req";
    public static final String CONTINUE = COMMAND_PREFIX + "continue_req";
    public static final String READY = COMMAND_PREFIX + "ready_req";
    public static final String LOOK = COMMAND_PREFIX + "look_req";
    public static final String BET = COMMAND_PREFIX + "bet_req";
    public static final String PRE_BET = COMMAND_PREFIX + "prebet_req";
    public static final String TIMEOUT = COMMAND_PREFIX + "timeout_req";
    public static final String FOLD = COMMAND_PREFIX + "fold_req";
    public static final String COMPARE = COMMAND_PREFIX + "compare_req";
    public static final String STATE = COMMAND_PREFIX + "state_req";
    public static final String STATE_REQUEST = STATE;
    public static final String SETTLE = COMMAND_PREFIX + "settle_req";

    @Override public GameCommandResult handle(GameRoomHandle room, GameCommandRequest request) {
        ZjhTable table = room.requireLegacyRoom(ZjhTable.class);
        requireAuthorityIdentity(room, table, request);
        GameCommandRequest command = normalizeDispatch(table, request);
        long playerId = playerId(command.authenticatedUserId());
        Map<String, Object> body;
        switch (command.msgId()) {
            case SIT -> { table.sit(playerId); body = table.viewFor(playerId); }
            // Old protocol compatibility remains centralized here; no new client emits JOIN or READY.
            case JOIN -> { table.sit(playerId); body = table.viewFor(playerId); }
            case READY -> { requireSeat(table, playerId, command.seatId()); table.ready(command.seatId(), booleanValue(command.body(), "ready")); body = table.viewFor(playerId); }
            case START -> { requireOwner(table, playerId); requireSeated(table, playerId); table.start(); body = table.viewFor(playerId); }
            case CONTINUE -> { requireOwner(table, playerId); requireSeated(table, playerId); table.continueRound(); body = table.viewFor(playerId); }
            case LOOK -> { table.look(requireSeated(table, playerId)); body = table.viewFor(playerId); }
            case BET -> { table.bet(requireSeated(table, playerId), number(command.body(), "amount")); body = table.viewFor(playerId); }
            case PRE_BET -> { table.preBet(requireSeated(table, playerId), number(command.body(), "amount")); body = table.viewFor(playerId); }
            case TIMEOUT -> { table.timeout(requireSeated(table, playerId), System.currentTimeMillis()); body = table.viewFor(playerId); }
            case FOLD -> { table.fold(requireSeated(table, playerId)); body = table.viewFor(playerId); }
            case COMPARE -> {
                int seatId = requireSeated(table, playerId);
                int loser = table.compare(seatId, number(command.body(), "targetSeatId"));
                body = Map.of("loserSeat", loser, "view", table.viewFor(playerId));
            }
            case STATE -> body = table.viewFor(playerId);
            case SETTLE -> body = new ZjhSettlementService().settle(table, command.roundNo());
            default -> throw new IllegalArgumentException("unsupported ZJH command: " + command.msgId());
        }
        LOG.info("[ZJH] command applied roomId={} playerId={} seatId={} requestId={} msgId={} stateVersion={}",
                room.roomId(), playerId, table.seatOf(playerId), command.requestId(), command.msgId(), table.stateVersion());
        return new GameCommandResult(request.msgId().replace("_req", "_resp"), request.requestId(), body);
    }

    private static GameCommandRequest normalizeDispatch(ZjhTable table, GameCommandRequest request) {
        if (!DISPATCH.equals(request.msgId())) return request;
        Map<String, Object> dispatch = request.body().asMap();
        String action = String.valueOf(dispatch.getOrDefault("action", ""));
        if (!action.startsWith(COMMAND_PREFIX)) {
            throw new IllegalArgumentException("unsupported ZJH dispatch action: " + action);
        }
        Object expected = dispatch.get("expectedStateVersion");
        if (!(expected instanceof Number number) || number.longValue() < 0
                || (!STATE.equals(action) && number.longValue() != table.stateVersion())) {
            throw new SecurityException("ZJH stale or missing expectedStateVersion");
        }
        Map<String, Object> payload = map(dispatch.get("payload"), "command payload");
        return new GameCommandRequest(action, request.requestId(), request.sequence(), request.roomId(),
                request.roundNo(), request.playVersion(), request.authenticatedUserId(), request.seatId(), payload);
    }

    private static Map<String, Object> map(Object value, String name) {
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException(name + " is required");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        raw.forEach((key, entry) -> result.put(String.valueOf(key), entry));
        return Map.copyOf(result);
    }

    private static long playerId(String value) {
        try { long id = Long.parseLong(value); if (id <= 0) throw new NumberFormatException(); return id; }
        catch (NumberFormatException error) { throw new IllegalArgumentException("authenticated user id must be positive numeric"); }
    }
    private static int number(Map<String, Object> body, String name) {
        Object value = body.get(name);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(name + " is required");
        return number.intValue();
    }
    private static boolean booleanValue(Map<String, Object> body, String name) {
        Object value = body.get(name);
        if (!(value instanceof Boolean result)) throw new IllegalArgumentException(name + " is required");
        return result;
    }
    private static void requireOwner(ZjhTable table, long playerId) {
        if (table.ownerId() != playerId) throw new SecurityException("only room owner may start");
    }
    private static void requireSeat(ZjhTable table, long playerId, int seatId) {
        if (!table.ownsSeat(playerId, seatId)) throw new SecurityException("seat is not owned by authenticated player");
    }
    private static int requireSeated(ZjhTable table, long playerId) {
        int seatId = table.seatOf(playerId);
        if (seatId < 0) throw new SecurityException("spectator cannot perform seated action");
        return seatId;
    }
    private static void requireAuthorityIdentity(GameRoomHandle room, ZjhTable table, GameCommandRequest request) {
        if (request.roomId() != room.roomId() || request.roomId() != table.roomId()) {
            throw new SecurityException("ZJH room identity mismatch");
        }
        if (!ZjhGameProvider.PLAY_VERSION.equals(room.playVersion())
                || !room.playVersion().equals(request.playVersion())) {
            throw new SecurityException("ZJH play version mismatch");
        }
    }
}
