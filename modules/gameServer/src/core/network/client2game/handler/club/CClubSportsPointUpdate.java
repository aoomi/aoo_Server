package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_SportsPointUpdate;

import java.io.IOException;

/**
 * 执行竞技点修改
 */
public class CClubSportsPointUpdate extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_SportsPointUpdate req = new Gson().fromJson(message, CClub_SportsPointUpdate.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().execSportsPointUpdate(req, player.getPid(), player);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
