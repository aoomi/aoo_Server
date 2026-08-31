package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.AuthoritativeSessionCommandHandler;
import com.aoo.bcg.gamespi.CommandPayload;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomLifecycleAuthority;
import com.aoo.bcg.gamespi.RoomMembershipLifecycle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Production v2 poker dispatch adapter. Legacy action spellings are accepted only inside the
 * authenticated canonical poker envelope and are translated to the single authoritative session. */
public final class PokerDispatchCommandHandler implements GameCommandHandler {
    private final AuthoritativeSessionCommandHandler authority = new AuthoritativeSessionCommandHandler();

    @Override public GameCommandResult handle(GameRoomHandle room, GameCommandRequest request) {
        if (!request.msgId().startsWith("poker.")) return authority.handle(room, request);
        String action = request.body().requireString("action");
        Map<String,Object> payload = payload(request.body().get("payload"));
        String operation = operation(action, payload);
        GameCommandRequest normalized = new GameCommandRequest("poker." + operation + "_req",
                request.requestId(), request.sequence(), request.roomId(), request.roundNo(),
                request.playVersion(), request.authenticatedUserId(), request.seatId(), payload);
        GameCommandResult result = authority.handle(room, normalized);
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("payload", result.body());
        response.put("stateVersion", room.requireAuthoritativeSession().stateVersion());
        response.put("action", action);
        if ("leave".equals(operation)) {
            response.put(RoomMembershipLifecycle.MEMBER_LEFT_FIELD, true);
            response.put(RoomMembershipLifecycle.MEMBER_LEFT_ACCOUNT_ID_FIELD,
                    Long.parseLong(request.authenticatedUserId()));
        }
        if (room.requireAuthoritativeSession() instanceof RoomLifecycleAuthority lifecycle && lifecycle.isTerminal()) {
            response.put(RoomLifecycleAuthority.TERMINAL_FIELD, true);
            response.put(RoomLifecycleAuthority.TERMINAL_REASON_FIELD, lifecycle.terminalReason());
        }
        return new GameCommandResult(request.msgId().replace("dispatch", "dispatch_resp"),
                request.requestId(), response);
    }

    private static String operation(String action, Map<String,Object> payload) {
        String key = action.replaceAll("[^A-Za-z]", "").toLowerCase();
        if (key.endsWith("getroominfo") || key.endsWith("reconnect") || key.endsWith("state")) return "state";
        if (key.endsWith("readyroom")) return "ready";
        if (key.endsWith("unreadyroom")) return "unready";
        if (key.endsWith("startgame")) return "start";
        if (key.endsWith("trusteeship")) return "trusteeship";
        if (key.endsWith("hint")) return "hint";
        if (key.endsWith("continuegame") || key.endsWith("continue")) return "continue";
        if (key.endsWith("dissolveroomagree")) return "dissolve_agree";
        if (key.endsWith("dissolveroomrefuse")) return "dissolve_refuse";
        if (key.endsWith("dissolveroom")) return "dissolve";
        if (key.endsWith("exitroom") || key.endsWith("leave")) return "leave";
        if (key.endsWith("pass")) return "pass";
        if (key.endsWith("play")) {
            Object cards = payload.getOrDefault("cards", payload.get("cardList"));
            if (!(cards instanceof List<?> list) || list.isEmpty()) throw new IllegalArgumentException("poker play cards required");
            payload.put("cards", cards);
            return "play";
        }
        if (key.endsWith("opcard")) {
            Object cards = payload.getOrDefault("cards", payload.get("cardList"));
            if (!(cards instanceof List<?> list) || list.isEmpty()) return "pass";
            payload.put("cards", cards);
            return "play";
        }
        if (key.endsWith("join") || key.endsWith("enterroom")) return "join";
        throw new IllegalArgumentException("unsupported poker dispatch action: " + action);
    }

    private static Map<String,Object> payload(Object value) {
        if (!(value instanceof Map<?,?> input)) throw new IllegalArgumentException("poker dispatch payload required");
        Map<String,Object> output = new LinkedHashMap<>();
        input.forEach((key, item) -> output.put(String.valueOf(key), item));
        return output;
    }
}
