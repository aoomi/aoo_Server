package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_Base;

import java.io.IOException;

/**
 * 亲友圈在线人数统计
 *
 * @author zaf
 */
public class CClubOnlinePlayerCount extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_Base req = new Gson().fromJson(message, CUnion_Base.class);
        SData_Result result = ClubMgr.getInstance().execClubOnlinePlayerCount(req, player);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getCustom());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
