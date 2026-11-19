package core.network.client2game.handler.player;

import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.CPlayer_GetPlayerInfo;

import java.io.IOException;

public class CPlayerGetPlayerInfo extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CPlayer_GetPlayerInfo req = new Gson().fromJson(message, CPlayer_GetPlayerInfo.class);
        SData_Result result = PlayerMgr.getInstance().getPlayerInfo(req);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
