package business.global.pk.zypk;

import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.StrictEnumDecoder;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ZYPKCommandHandler implements GameCommandHandler {
    public static final String STATE_REQUEST = "poker.zypk.state_req";
    public static final String OPERATE_REQUEST = "poker.zypk.operate_req";
    public static final String GIVE_REQUEST = "poker.zypk.give_req";
    public static final String PASS_REQUEST = "poker.zypk.pass_req";
    public static final String BACK_REQUEST = "poker.zypk.back_req";
    public static final String SETTLE_REQUEST = "poker.zypk.settle_req";

    @Override
    public GameCommandResult handle(GameRoomHandle room, GameCommandRequest request) {
        if (room.gameId() != ZYPKPersistenceService.GAME_ID || room.roomId() != request.roomId())
            throw new SecurityException("ZYPK room identity mismatch");
        if (!room.playVersion().equals(request.playVersion())) throw new SecurityException("ZYPK version mismatch");
        ZYPKTable table = room.requireLegacyRoom(ZYPKTable.class);
        long playerId;
        try { playerId = Long.parseLong(request.authenticatedUserId()); }
        catch (NumberFormatException error) { throw new SecurityException("numeric ZYPK player id required", error); }
        if (!table.ownsSeat(playerId, request.seatId())) throw new SecurityException("seat is not owned by player");
        return switch (request.msgId()) {
            case STATE_REQUEST -> response(request, "poker.zypk.state_resp",
                    Map.of("snapshot", table.reconnectView(playerId)));
            case OPERATE_REQUEST -> {
                ZYPK_AnNiu action = action(request.body().get("action"));
                int amount = integer(request.body().get("amount"), 0);
                Integer target = request.body().get("targetSeat") == null ? null
                        : integer(request.body().get("targetSeat"), 0);
                table.operate(request.seatId(), action, cards(request.body().get("cards")), amount, target);
                yield response(request, "poker.zypk.operate_resp",
                        Map.of("snapshot", table.reconnectView(playerId)));
            }
            case GIVE_REQUEST -> {
                int target = integer(request.body().get("targetSeat"), -1);
                int amount = integer(request.body().get("amount"), 0);
                table.transferChips(request.seatId(), target, amount);
                yield response(request, "poker.zypk.give_resp",
                        Map.of("snapshot", table.reconnectView(playerId)));
            }
            case PASS_REQUEST -> {
                table.pass(request.seatId());
                yield response(request, "poker.zypk.pass_resp",
                        Map.of("snapshot", table.reconnectView(playerId)));
            }
            case BACK_REQUEST -> {
                table.rollbackLastOperation(request.seatId());
                yield response(request, "poker.zypk.back_resp",
                        Map.of("snapshot", table.reconnectView(playerId)));
            }
            case SETTLE_REQUEST -> response(request, "poker.zypk.settle_resp",
                    Map.of("settlement", table.settlement(integer(request.body().get("roundNo"), 0))));
            default -> throw new IllegalArgumentException("unsupported ZYPK msgId: " + request.msgId());
        };
    }

    private static GameCommandResult response(GameCommandRequest request, String msgId, Map<String, Object> body) {
        return new GameCommandResult(msgId, request.requestId(), body);
    }

    private static ZYPK_AnNiu action(Object value) {
        if (value instanceof Number) return StrictEnumDecoder.byCode(ZYPK_AnNiu.class, value, ZYPK_AnNiu::value);
        return StrictEnumDecoder.byName(ZYPK_AnNiu.class, value);
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static List<Integer> cards(Object value) {
        if (value == null) return List.of();
        if (!(value instanceof Collection<?> values)) throw new IllegalArgumentException("cards must be a list");
        List<Integer> cards = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Number number)) throw new IllegalArgumentException("cards must contain numbers");
            cards.add(number.intValue());
        }
        return List.copyOf(cards);
    }
}
