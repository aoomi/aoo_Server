package core.network.client2game.handler.room;

import business.player.Player;
import business.player.feature.PlayerRoom;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_EnterRoom;
import java.io.IOException;

/** Shared room-entry command; identity always comes from the authenticated connection. */
public abstract class CBaseEnterRoom extends PlayerHandler {
    @Override public final void handle(Player player, WebSocketRequest request, String message) throws IOException {
        CBase_EnterRoom req = new Gson().fromJson(message, CBase_EnterRoom.class);
        SData_Result result = player.getFeature(PlayerRoom.class).findAndEnter(req.getPosID(), req.getRoomKey(), req.getClubId(), req.getPassword());
        if (ErrorCode.Success.equals(result.getCode())) request.response(result.getData()); else request.error(result.getCode(), result.getMsg());
    }
}
