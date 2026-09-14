package core.server;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import com.aoo.bcg.gamespi.GameCapability;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRegistry;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import cenum.room.RoomState;

import java.util.Map;

/** The single production authority for shuffle and seat eviction. */
final class ProductionRoomAdministrationService {
    private final GameRegistry games;

    ProductionRoomAdministrationService(GameRegistry games) { this.games = games; }

    GameCommandResult execute(Player actor, AbsBaseRoom room,
                              com.ddm.server.protocol.v2.ProtocolV2AuthorityRuntime.Command command,
                              String action,long expectedStateVersion,long nextStateVersion) {
        if (!command.playVersion().equals(games.require(room.getBaseRoomConfigure().getGameType().getId(),
                command.playVersion()).descriptor().version())) throw rejected("ROOM_PLAY_VERSION_MISMATCH");
        AbsRoomPos actorSeat = room.getRoomPosMgr().getPosByPid(actor.getPid());
        if (actorSeat == null) throw rejected("ROOM_ACTOR_NOT_SEATED");
        requireNumber(command.body(), "seatId", actorSeat.getPosID());
        long expected = number(command.body(), "expectedStateVersion");
        if (expected != expectedStateVersion || nextStateVersion != Math.addExact(expectedStateVersion,1))
            throw rejected("ROOM_STATE_VERSION_CONFLICT");
        if (!RoomState.Init.equals(room.getRoomState())) throw rejected("ROOM_STATE_FORBIDS_ADMIN_OPERATION");

        SData_Result<?> result;
        Map<String,Object> response;
        if ("shuffle".equals(action)) {
            if (!games.require(room.getBaseRoomConfigure().getGameType().getId(), command.playVersion())
                    .capabilityManifest().supports(GameCapability.ROOM_SHUFFLE))
                throw rejected("ROOM_SHUFFLE_UNSUPPORTED");
            result = room.opXiPai(actor.getPid());
            response = Map.of("accepted", true, "stateVersion", nextStateVersion);
        } else if ("kick".equals(action)) {
            int targetSeat = Math.toIntExact(number(command.body(), "targetSeatId"));
            if (targetSeat == actorSeat.getPosID()) throw rejected("ROOM_CANNOT_KICK_SELF");
            if (room.getRoomPosMgr().getPosByPosID(targetSeat) == null) throw rejected("ROOM_TARGET_SEAT_NOT_FOUND");
            result = room.kickOut(actor.getPid(), targetSeat);
            response = Map.of("accepted", true, "targetSeatId", targetSeat,
                    "authorityCommitted", true, "stateVersion", nextStateVersion);
        } else throw rejected("ROOM_ADMIN_ACTION_UNSUPPORTED");
        if (!ErrorCode.Success.equals(result.getCode()))
            throw rejected("ROOM_AUTHORITY_REJECTED:" + result.getCode().value());
        return new GameCommandResult("room." + action + "_resp", command.requestId(), response);
    }

    private static long number(Map<String,Object> body,String key) {
        if (!(body.get(key) instanceof Number n) || n.longValue() < 0) throw rejected("ROOM_INVALID_" + key.toUpperCase());
        return n.longValue();
    }
    private static void requireNumber(Map<String,Object> body,String key,long expected) {
        if (number(body,key) != expected) throw rejected("ROOM_SEAT_MISMATCH");
    }
    private static IllegalArgumentException rejected(String code) { return new IllegalArgumentException(code); }
}
