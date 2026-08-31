package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import java.util.Map;

public final class ZJHCommandHandler implements GameCommandHandler {
    public static final String JOIN = "poker.zjh.join_req";
    public static final String START = "poker.zjh.start_req";
    public static final String READY = "poker.zjh.ready_req";
    public static final String FOLD = "poker.zjh.fold_req";
    public static final String COMPARE = "poker.zjh.compare_req";
    public static final String STATE = "poker.zjh.state_req";
    public static final String STATE_REQUEST = STATE;
    public static final String SETTLE = "poker.zjh.settle_req";

    @Override public GameCommandResult handle(GameRoomHandle room, GameCommandRequest request) {
        ZJHTable table = room.requireLegacyRoom(ZJHTable.class);
        long playerId = playerId(request.authenticatedUserId());
        Map<String, Object> body;
        switch (request.msgId()) {
            case JOIN -> { table.join(request.seatId(), playerId); body = table.viewFor(playerId); }
            case READY -> { requireSeat(table, playerId, request.seatId()); table.ready(request.seatId(), booleanValue(request.body(), "ready")); body = table.viewFor(playerId); }
            case START -> { requireOwner(table, playerId); table.start(); body = table.viewFor(playerId); }
            case FOLD -> { requireSeat(table, playerId, request.seatId()); table.fold(request.seatId()); body = table.viewFor(playerId); }
            case COMPARE -> {
                requireSeat(table, playerId, request.seatId());
                int loser = table.compare(request.seatId(), number(request.body(), "targetSeatId"));
                body = Map.of("loserSeat", loser, "view", table.viewFor(playerId));
            }
            case STATE -> body = table.viewFor(playerId);
            case SETTLE -> body = new ZJHSettlementService().settle(table, request.roundNo());
            default -> throw new IllegalArgumentException("unsupported ZJH command: " + request.msgId());
        }
        return new GameCommandResult(request.msgId().replace("_req", "_resp"), request.requestId(), body);
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
    private static void requireOwner(ZJHTable table, long playerId) {
        if (table.ownerId() != playerId) throw new SecurityException("only room owner may start");
    }
    private static void requireSeat(ZJHTable table, long playerId, int seatId) {
        if (!table.ownsSeat(playerId, seatId)) throw new SecurityException("seat is not owned by authenticated player");
    }
}
