package core.network.client2game.handler;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;

/** Server-side authority boundary for legacy gameplay messages. */
final class LegacyGameRequestGuard {
    private static final Set<String> MUTATING = Set.of("OpCard", "Trusteeship", "Piao", "OpenCard",
            "AddDouble", "QiangZhuang", "KongPai", "RobClose", "FaPaiJieShu", "AutoChoose",
            "ChangePlayerNum", "KickRoom", "Give", "Zhuang", "PlayerOp");
    private static final Set<String> ACTOR_SEAT = Set.of("OpCard", "OpenCard", "AddDouble",
            "QiangZhuang", "KongPai", "RobClose", "FaPaiJieShu", "AutoChoose", "Zhuang", "PlayerOp");

    private LegacyGameRequestGuard() {}

    static void validate(String event, Player player, String message) {
        String operation = operation(event);
        if (operation == null) return;
        JsonObject json;
        try { json = JsonParser.parseString(message).getAsJsonObject(); }
        catch (RuntimeException error) { throw new SecurityException("invalid gameplay request", error); }
        Long roomId = longValue(json, "roomID", "roomId");
        if (roomId == null || roomId <= 0) throw new SecurityException("gameplay room id required");
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomId);
        if (room == null) return; // Preserve the handler's established NotFind_Room response.
        AbsRoomPos ownPosition = room.getRoomPosMgr().getPosByPid(player.getPid());
        if (ownPosition == null) throw new SecurityException("authenticated player is not in requested room");
        if (ACTOR_SEAT.contains(operation)) {
            Integer requestedSeat = intValue(json, "pos", "seatId");
            if (requestedSeat != null && requestedSeat != ownPosition.getPosID())
                throw new SecurityException("actor seat is not owned by authenticated player");
        }
    }

    private static String operation(String event) {
        if (event == null) return null;
        for (String operation : MUTATING) if (event.endsWith(operation) || event.contains("_" + operation)) return operation;
        return null;
    }
    private static Long longValue(JsonObject json, String... names) {
        for (String name : names) { JsonElement value = json.get(name); if (value != null && value.isJsonPrimitive()) return value.getAsLong(); }
        return null;
    }
    private static Integer intValue(JsonObject json, String... names) {
        for (String name : names) { JsonElement value = json.get(name); if (value != null && value.isJsonPrimitive()) return value.getAsInt(); }
        return null;
    }
}
